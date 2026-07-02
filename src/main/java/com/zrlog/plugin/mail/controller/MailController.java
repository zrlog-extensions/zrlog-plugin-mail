package com.zrlog.plugin.mail.controller;

import com.google.gson.Gson;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.model.PublicInfo;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.mail.model.EmailApiResponse;
import com.zrlog.plugin.mail.model.EmailConfig;
import com.zrlog.plugin.mail.model.EmailPageData;
import com.zrlog.plugin.mail.model.EmailRequestParams;
import com.zrlog.plugin.mail.model.EmailSendResponse;
import com.zrlog.plugin.mail.service.EmailRepository;
import com.zrlog.plugin.mail.util.MailUtil;
import com.zrlog.plugin.type.ActionType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final EmailRepository repository;
    private final Gson gson = new Gson();

    public MailController(IOSession session, MsgPacket requestPacket, HttpRequestInfo requestInfo) {
        this.session = session;
        this.requestPacket = requestPacket;
        this.requestInfo = requestInfo;
        this.repository = new EmailRepository(session);
    }

    public void update() {
        EmailConfig config = repository.saveConfig(params());
        response(EmailApiResponse.success(config));
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
        EmailConfig config = repository.readConfig();
        response(EmailApiResponse.success(repository.page(params(), config.getRetentionDays())));
    }

    public void testEmailService() {
        EmailConfig config = repository.readConfig();
        Map<String, Object> smtpMap = configMap(config);
        List<String> recipients = new ArrayList<>();
        recipients.add(config.getTo());
        String subject = "这是一封测试邮件";
        try {
            PublicInfo responseSync = session.getResponseSync(ContentType.JSON, new HashMap<>(), ActionType.LOAD_PUBLIC_INFO, PublicInfo.class);
            smtpMap.put("displayName", responseSync.getTitle());
            MailUtil.sendMail(config.getTo(), subject, "<div>当你看到这封邮件的时候，说明邮件服务已经可以正常工作了</div>\n", smtpMap, new ArrayList<>());
            repository.record(recipients, subject, "测试发送", true, 200, "", 0);
            response(EmailApiResponse.success(new EmailSendResponse(200)));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "send email error ", e);
            repository.record(recipients, subject, "测试发送", false, 500, e.getMessage(), 0);
            response(EmailApiResponse.success(new EmailSendResponse(500)));
        }
    }

    private EmailApiResponse<EmailPageData> pageData() {
        EmailConfig config = repository.readConfig();
        Map<String, Object> overview = repository.overview(config.getRetentionDays());
        EmailRequestParams firstPageParams = new EmailRequestParams();
        firstPageParams.setPage("1");
        firstPageParams.setPageSize("10");
        EmailPageData data = new EmailPageData();
        data.setDark(requestInfo.isDarkMode());
        data.setColorPrimary(requestInfo.getAdminColorPrimary());
        data.setPlugin(session.getPlugin());
        data.setConfig(config);
        data.setSummary(overview.get("summary"));
        data.setTrend(overview.get("trend"));
        data.setLogs(repository.page(firstPageParams, config.getRetentionDays()));
        return EmailApiResponse.success(data);
    }

    private EmailRequestParams params() {
        if (requestInfo.getRequestBody() != null && requestInfo.getRequestBody().length > 0) {
            String body = new String(requestInfo.getRequestBody(), StandardCharsets.UTF_8);
            if (body.trim().startsWith("{")) {
                EmailRequestParams params = gson.fromJson(body, EmailRequestParams.class);
                return params == null ? new EmailRequestParams() : params;
            }
        }
        return EmailRequestParams.fromParam(paramValue("to"), paramValue("from"), paramValue("smtpServer"),
                paramValue("password"), paramValue("port"), paramValue("emailLogRetentionDays"),
                paramValue("retentionDays"), paramValue("page"), paramValue("pageSize"), paramValue("keyword"),
                paramValue("status"));
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

    private String paramValue(String key) {
        if (requestInfo.getParam() == null || requestInfo.getParam().get(key) == null || requestInfo.getParam().get(key).length == 0) {
            return "";
        }
        return requestInfo.getParam().get(key)[0];
    }

    private void response(Object data) {
        session.sendMsg(ContentType.JSON, data, requestPacket.getMethodStr(), requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
    }

    private boolean isDarkMode() {
        return requestInfo.isDarkMode();
    }

}
