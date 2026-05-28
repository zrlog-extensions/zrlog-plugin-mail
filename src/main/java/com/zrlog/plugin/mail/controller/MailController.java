package com.zrlog.plugin.mail.controller;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.common.IdUtil;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.model.PublicInfo;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.mail.model.EmailConfig;
import com.zrlog.plugin.mail.service.EmailRepository;
import com.zrlog.plugin.mail.util.MailUtil;
import com.zrlog.plugin.type.ActionType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by xiaochun on 2016/2/13.
 */
public class MailController {

    private static final Logger LOGGER = LoggerUtil.getLogger(MailController.class);

    private final IOSession session;
    private final MsgPacket requestPacket;
    private final HttpRequestInfo requestInfo;
    private final EmailRepository repository = EmailRepository.getInstance();
    private final Gson gson = new Gson();

    public MailController(IOSession session, MsgPacket requestPacket, HttpRequestInfo requestInfo) {
        this.session = session;
        this.requestPacket = requestPacket;
        this.requestInfo = requestInfo;
    }

    public void update() {
        EmailConfig config = repository.saveConfig(session, params());
        response(successMap(config));
    }

    public void index() {
        Map<String, Object> data = new HashMap<>();
        data.put("theme", isDarkMode() ? "dark" : "light");
        data.put("data", gson.toJson(pageData()));
        session.responseHtml("/templates/index", data, requestPacket.getMethodStr(), requestPacket.getMsgId());
    }

    public void json() {
        response(pageData());
    }

    public void list() {
        EmailConfig config = repository.readConfig(session);
        response(successMap(repository.page(session, params(), config.getRetentionDays())));
        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("key", "to,from,smtpServer,password,port");
        session.sendJsonMsg(keyMap, ActionType.GET_WEBSITE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST, msgPacket -> {
            Map map = new Gson().fromJson(msgPacket.getDataStr(), Map.class);
            Map<String, Object> data = new HashMap<>();
            data.put("theme", Objects.equals(requestInfo.getHeader().get("Dark-Mode"), "true") ? "dark" : "light");
            data.put("data", new Gson().toJson(map));
            session.responseHtml("/templates/index", data, requestPacket.getMethodStr(), requestPacket.getMsgId());
        });
    }

    public void testEmailService() {
        EmailConfig config = repository.readConfig(session);
        Map<String, Object> smtpMap = configMap(config);
        Map<String, Object> response = new HashMap<>();
        List<String> recipients = new ArrayList<>();
        recipients.add(config.getTo());
        String subject = "这是一封测试邮件";
        try {
            PublicInfo responseSync = session.getResponseSync(ContentType.JSON, new HashMap<>(), ActionType.LOAD_PUBLIC_INFO, PublicInfo.class);
            smtpMap.put("displayName", responseSync.getTitle());
            MailUtil.sendMail(config.getTo(), subject, "<div>当你看到这封邮件的时候，说明邮件服务已经可以正常工作了</div>\n", smtpMap, new ArrayList<>());
            repository.record(session, recipients, subject, "测试发送", true, 200, "", 0);
            response.put("status", 200);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "send email error ", e);
            repository.record(session, recipients, subject, "测试发送", false, 500, e.getMessage(), 0);
            response.put("status", 500);
        }
        response(successMap(response));
    }

    private Map<String, Object> pageData() {
        EmailConfig config = repository.readConfig(session);
        Map<String, Object> overview = repository.overview(session, config.getRetentionDays());
        Map<String, Object> firstPageParams = new HashMap<>();
        firstPageParams.put("page", "1");
        firstPageParams.put("pageSize", "10");
        PublicInfo publicInfo = publicInfo();
        Map<String, Object> data = new HashMap<>();
        data.put("dark", publicInfo.getDarkMode() == null ? isDarkMode() : publicInfo.getDarkMode());
        data.put("colorPrimary", notBlank(publicInfo.getAdminColorPrimary()) ? publicInfo.getAdminColorPrimary() : "#1677ff");
        data.put("plugin", session.getPlugin());
        data.put("config", config);
        data.put("summary", overview.get("summary"));
        data.put("trend", overview.get("trend"));
        data.put("logs", repository.page(session, firstPageParams, config.getRetentionDays()));
        return successMap(data);
    }

    private PublicInfo publicInfo() {
        try {
            PublicInfo publicInfo = session.getResponseSync(ContentType.JSON, new HashMap<>(), ActionType.LOAD_PUBLIC_INFO, PublicInfo.class);
            return publicInfo == null ? new PublicInfo() : publicInfo;
        } catch (Exception e) {
            return new PublicInfo();
        }
    }

    private Map<String, Object> params() {
        if (requestInfo.getRequestBody() != null && requestInfo.getRequestBody().length > 0) {
            String body = new String(requestInfo.getRequestBody(), StandardCharsets.UTF_8);
            if (body.trim().startsWith("{")) {
                return gson.fromJson(body, Map.class);
            }
        }
        if (requestInfo.getParam() == null) {
            return new HashMap<>();
        }
        return requestInfo.simpleParam();
    }

    private Map<String, Object> configMap(EmailConfig config) {
        Map<String, Object> map = new HashMap<>();
        map.put("to", config.getTo());
        map.put("from", config.getFrom());
        map.put("smtpServer", config.getSmtpServer());
        map.put("password", config.getPassword());
        map.put("port", config.getPort());
        return map;
    }

    private void response(Map<String, Object> map) {
        session.sendMsg(ContentType.JSON, map, requestPacket.getMethodStr(), requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
    }

    private Map<String, Object> successMap(Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("data", data);
        return map;
    }

    private boolean isDarkMode() {
        return requestInfo.getHeader() != null && Objects.equals(requestInfo.getHeader().get("Dark-Mode"), "true");
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
