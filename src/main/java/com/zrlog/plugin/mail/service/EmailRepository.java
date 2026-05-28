package com.zrlog.plugin.mail.service;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.mail.model.EmailConfig;
import com.zrlog.plugin.mail.model.EmailLogEntry;
import com.zrlog.plugin.mail.model.EmailLogStore;
import com.zrlog.plugin.type.ActionType;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EmailRepository {

    private static final Logger LOGGER = LoggerUtil.getLogger(EmailRepository.class);
    private static final EmailRepository INSTANCE = new EmailRepository();
    private static final String STORE_KEY = "emailSendLogs";
    private static final String RETENTION_DAYS_KEY = "emailLogRetentionDays";
    private static final String CONFIG_KEYS = "to,from,smtpServer,password,port," + RETENTION_DAYS_KEY;
    private static final int DEFAULT_RETENTION_DAYS = 30;
    private static final int MAX_VALUE_BYTES = 950 * 1024;
    private static final ZoneId ZONE = ZoneId.systemDefault();
    private final Gson gson = new Gson();

    public static EmailRepository getInstance() {
        return INSTANCE;
    }

    public synchronized EmailConfig readConfig(IOSession session) {
        Map<String, String> request = new HashMap<>();
        request.put("key", CONFIG_KEYS);
        Map responseMap = session.getResponseSync(ContentType.JSON, request, ActionType.GET_WEBSITE, Map.class);
        EmailConfig config = new EmailConfig();
        if (responseMap != null) {
            config.setTo(stringValue(responseMap.get("to")));
            config.setFrom(stringValue(responseMap.get("from")));
            config.setSmtpServer(stringValue(responseMap.get("smtpServer")));
            config.setPassword(stringValue(responseMap.get("password")));
            config.setPort(stringValue(responseMap.get("port")));
            config.setRetentionDays(normalizeRetentionDays(stringValue(responseMap.get(RETENTION_DAYS_KEY))));
        }
        return config;
    }

    public synchronized EmailConfig saveConfig(IOSession session, Map<String, Object> params) {
        EmailConfig config = new EmailConfig();
        config.setTo(limit(stringValue(params.get("to")), 240));
        config.setFrom(limit(stringValue(params.get("from")), 240));
        config.setSmtpServer(limit(stringValue(params.get("smtpServer")), 180));
        config.setPassword(limit(stringValue(params.get("password")), 240));
        config.setPort(limit(stringValue(params.get("port")), 12));
        String retentionDays = stringValue(params.get(RETENTION_DAYS_KEY));
        if (!notBlank(retentionDays)) {
            retentionDays = stringValue(params.get("retentionDays"));
        }
        config.setRetentionDays(normalizeRetentionDays(retentionDays));

        Map<String, String> request = new HashMap<>();
        request.put("to", config.getTo());
        request.put("from", config.getFrom());
        request.put("smtpServer", config.getSmtpServer());
        request.put("password", config.getPassword());
        request.put("port", config.getPort());
        request.put(RETENTION_DAYS_KEY, String.valueOf(config.getRetentionDays()));
        session.getResponseSync(ContentType.JSON, request, ActionType.SET_WEBSITE, Map.class);
        return config;
    }

    public synchronized void record(IOSession session, List<String> recipients, String subject, String source,
                                    boolean success, int status, String error, int attachmentCount) {
        try {
            EmailLogStore store = readStore(session);
            EmailLogEntry entry = new EmailLogEntry();
            entry.setTimestamp(System.currentTimeMillis());
            entry.setRecipients(limitRecipients(recipients));
            entry.setSubject(limit(subject, 180));
            entry.setSource(limit(source, 40));
            entry.setSuccess(success);
            entry.setStatus(status);
            entry.setError(limit(error, 500));
            entry.setAttachmentCount(Math.max(0, attachmentCount));
            store.getItems().add(entry);
            writeStore(session, prune(store, readConfig(session).getRetentionDays()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "record email send log error", e);
        }
    }

    public synchronized Map<String, Object> overview(IOSession session, int retentionDays) {
        List<EmailLogEntry> logs = listRecentLogs(session, retentionDays);
        int success = 0;
        int failed = 0;
        int attachmentCount = 0;
        Map<String, Integer> dayCounts = new LinkedHashMap<>();
        LocalDate today = LocalDate.now(ZONE);
        for (int i = retentionDays - 1; i >= 0; i--) {
            dayCounts.put(today.minusDays(i).toString(), 0);
        }
        for (EmailLogEntry log : logs) {
            if (log.isSuccess()) {
                success++;
            } else {
                failed++;
            }
            attachmentCount += log.getAttachmentCount();
            LocalDate day = toDay(log.getTimestamp());
            if (day != null) {
                String key = day.toString();
                dayCounts.put(key, dayCounts.getOrDefault(key, 0) + 1);
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("summary", metrics(retentionDays, logs.size(), success, failed, attachmentCount));
        data.put("trend", trend(dayCounts));
        return data;
    }

    public synchronized Map<String, Object> page(IOSession session, Map<String, Object> params, int retentionDays) {
        int page = Math.max(1, parseInt(stringValue(params.get("page")), 1));
        int pageSize = Math.max(1, Math.min(100, parseInt(stringValue(params.get("pageSize")), 10)));
        String keyword = stringValue(params.get("keyword")).toLowerCase();
        String status = stringValue(params.get("status"));
        List<EmailLogEntry> logs = listRecentLogs(session, retentionDays);
        Collections.sort(logs, new Comparator<EmailLogEntry>() {
            @Override
            public int compare(EmailLogEntry left, EmailLogEntry right) {
                return Long.compare(right.getTimestamp(), left.getTimestamp());
            }
        });
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (EmailLogEntry log : logs) {
            Map<String, Object> row = rowMap(log);
            if ("success".equals(status) && !log.isSuccess()) {
                continue;
            }
            if ("failed".equals(status) && log.isSuccess()) {
                continue;
            }
            if (notBlank(keyword) && !gson.toJson(row).toLowerCase().contains(keyword)) {
                continue;
            }
            filtered.add(row);
        }
        int total = filtered.size();
        int from = Math.min((page - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("rows", filtered.subList(from, to));
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);
        return data;
    }

    private EmailLogStore readStore(IOSession session) {
        try {
            String json = readWebsiteValue(session, STORE_KEY);
            if (!notBlank(json)) {
                return new EmailLogStore();
            }
            EmailLogStore store = gson.fromJson(json, EmailLogStore.class);
            return store == null ? new EmailLogStore() : store;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "read email log store error", e);
            return new EmailLogStore();
        }
    }

    private void writeStore(IOSession session, EmailLogStore store) {
        String json = gson.toJson(store);
        while (json.getBytes(StandardCharsets.UTF_8).length > MAX_VALUE_BYTES && !store.getItems().isEmpty()) {
            store.getItems().remove(0);
            json = gson.toJson(store);
        }
        writeWebsiteValue(session, STORE_KEY, json);
    }

    private EmailLogStore prune(EmailLogStore store, int retentionDays) {
        long minTime = LocalDate.now(ZONE).minusDays(retentionDays - 1L).atStartOfDay(ZONE).toInstant().toEpochMilli();
        List<EmailLogEntry> items = new ArrayList<>();
        for (EmailLogEntry item : store.getItems()) {
            if (item.getTimestamp() >= minTime) {
                items.add(item);
            }
        }
        store.setItems(items);
        return store;
    }

    private List<EmailLogEntry> listRecentLogs(IOSession session, int retentionDays) {
        return prune(readStore(session), retentionDays).getItems();
    }

    private List<Map<String, Object>> metrics(int retentionDays, int total, int success, int failed, int attachmentCount) {
        List<Map<String, Object>> metrics = new ArrayList<>();
        metrics.add(metricMap(retentionDays + " 天发送", total, total > 0 ? "processing" : "normal"));
        metrics.add(metricMap("发送成功", success, success > 0 ? "processing" : "normal"));
        metrics.add(metricMap("发送失败", failed, failed > 0 ? "warning" : "normal"));
        metrics.add(metricMap("附件数", attachmentCount, "normal"));
        return metrics;
    }

    private List<Map<String, Object>> trend(Map<String, Integer> dayCounts) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : dayCounts.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", entry.getKey().substring(5));
            row.put("value", entry.getValue());
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> rowMap(EmailLogEntry log) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", String.valueOf(log.getTimestamp()) + "-" + log.getStatus());
        row.put("timestamp", log.getTimestamp());
        row.put("time", formatTime(log.getTimestamp()));
        row.put("recipients", log.getRecipients() == null ? new ArrayList<String>() : log.getRecipients());
        row.put("subject", defaultText(log.getSubject(), ""));
        row.put("source", defaultText(log.getSource(), ""));
        row.put("success", log.isSuccess());
        row.put("status", log.getStatus());
        row.put("error", defaultText(log.getError(), ""));
        row.put("attachmentCount", log.getAttachmentCount());
        return row;
    }

    private Map<String, Object> metricMap(String label, int value, String status) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("label", label);
        map.put("value", value);
        map.put("status", status);
        return map;
    }

    private String readWebsiteValue(IOSession session, String key) {
        Map<String, String> request = new HashMap<>();
        request.put("key", key);
        Map responseMap = session.getResponseSync(ContentType.JSON, request, ActionType.GET_WEBSITE, Map.class);
        if (responseMap == null || responseMap.get(key) == null) {
            return "";
        }
        return String.valueOf(responseMap.get(key));
    }

    private void writeWebsiteValue(IOSession session, String key, String value) {
        Map<String, String> request = new HashMap<>();
        request.put(key, value);
        session.getResponseSync(ContentType.JSON, request, ActionType.SET_WEBSITE, Map.class);
    }

    private List<String> limitRecipients(List<String> recipients) {
        List<String> result = new ArrayList<>();
        if (recipients == null) {
            return result;
        }
        for (String recipient : recipients) {
            if (notBlank(recipient)) {
                result.add(limit(recipient, 240));
            }
            if (result.size() >= 20) {
                break;
            }
        }
        return result;
    }

    private int normalizeRetentionDays(String value) {
        int days = parseInt(value, DEFAULT_RETENTION_DAYS);
        if (days == 90 || days == 180) {
            return days;
        }
        return DEFAULT_RETENTION_DAYS;
    }

    private int parseInt(String value, int defaultValue) {
        try {
            if (!notBlank(value)) {
                return defaultValue;
            }
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof List && !((List) value).isEmpty()) {
            return String.valueOf(((List) value).get(0));
        }
        return String.valueOf(value);
    }

    private LocalDate toDay(long time) {
        if (time <= 0) {
            return null;
        }
        return Instant.ofEpochMilli(time).atZone(ZONE).toLocalDate();
    }

    private String formatTime(long timestamp) {
        if (timestamp <= 0) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
    }

    private String limit(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max);
    }

    private String defaultText(String value, String defaultValue) {
        return notBlank(value) ? value : defaultValue;
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
