package com.zrlog.plugin.mail.service;

import com.google.gson.Gson;
import com.zrlog.plugin.IMsgPacketCallBack;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.api.Capability;
import com.zrlog.plugin.api.IPluginService;
import com.zrlog.plugin.api.Service;
import com.zrlog.plugin.common.IdUtil;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.model.PublicInfo;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.mail.model.EmailConfig;
import com.zrlog.plugin.mail.model.EmailSendRequest;
import com.zrlog.plugin.mail.model.EmailSendResponse;
import com.zrlog.plugin.mail.model.WebsiteKeyRequest;
import com.zrlog.plugin.mail.util.MailUtil;
import com.zrlog.plugin.message.CapabilityInvokeResult;
import com.zrlog.plugin.type.ActionType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service("emailService")
@Capability(
        key = "notification.email.send",
        type = "notification_channel",
        label = "发送邮件通知",
        description = "通过已配置的 SMTP 服务发送通知邮件，并记录发送结果。",
        exposure = {"notification"},
        channel = "email",
        timeoutSeconds = 60
)
public class EmailService implements IPluginService {

    private static final Logger LOGGER = LoggerUtil.getLogger(EmailService.class);


    @Override
    public void handle(final IOSession ioSession, final MsgPacket requestPacket) {
        final EmailRepository REPOSITORY = new EmailRepository(ioSession);
        ioSession.sendJsonMsg(WebsiteKeyRequest.of("to,from,smtpServer,password,port"), ActionType.GET_WEBSITE.name(), IdUtil.getInt(),
                MsgPacketStatus.SEND_REQUEST, new IMsgPacketCallBack() {
            @Override
            public void handler(MsgPacket responseMsgPacket) {
                Gson gson = new Gson();
                EmailConfig config = gson.fromJson(responseMsgPacket.getDataStr(), EmailConfig.class);
                if (config == null) {
                    config = new EmailConfig();
                }
                EmailSendRequest rawRequest = gson.fromJson(requestPacket.getDataStr(), EmailSendRequest.class);
                EmailSendRequest request = payload(requestPacket, rawRequest);
                EmailSendResponse response = new EmailSendResponse();
                SendContext sendContext = parseContext(config, request);
                int status;
                String error = "";
                try {
                    if (!request.hasRequiredContent()) {
                        status = 401;
                        error = "missing title or content";
                        REPOSITORY.record(sendContext.to, sendContext.title, sendContext.source, false, status, error, sendContext.attachmentCount);
                    } else {
                        error = validateConfig(config, sendContext);
                        if (!error.isEmpty()) {
                            status = 400;
                            REPOSITORY.record(sendContext.to, sendContext.title, sendContext.source, false, status, error, sendContext.attachmentCount);
                        } else {
                            status = 200;
                            sendEmail(config, request, sendContext);
                            REPOSITORY.record(sendContext.to, sendContext.title, sendContext.source, true, status, "", sendContext.attachmentCount);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "send email error ", e);
                    status = 500;
                    error = e.getMessage();
                    REPOSITORY.record(sendContext.to, sendContext.title, sendContext.source, false, status, error, sendContext.attachmentCount);
                }
                response.setStatus(status);
                sendResponse(ioSession, requestPacket, response, status, error);
            }

            private SendContext parseContext(EmailConfig config, EmailSendRequest request) {
                SendContext context = new SendContext();
                if (request.getTo() == null) {
                    addRecipient(context, config.getTo());
                } else if (request.getTo() instanceof List) {
                    for (Object item : (List) request.getTo()) {
                        addRecipient(context, item);
                    }
                } else {
                    addRecipient(context, request.getTo());
                }
                context.title = firstString(request.getTitle());
                context.content = firstString(request.getContent());
                context.attachmentCount = request.attachmentCount();
                context.source = source(request);
                return context;
            }

            private void addRecipient(SendContext context, Object value) {
                String recipient = firstString(value);
                if (!recipient.trim().isEmpty()) {
                    context.to.add(recipient.trim());
                }
            }

            private String validateConfig(EmailConfig config, SendContext context) {
                List<String> missing = new ArrayList<>();
                if (context.to.isEmpty()) {
                    missing.add("收件人");
                }
                addMissing(missing, config.getFrom(), "发件人");
                addMissing(missing, config.getSmtpServer(), "SMTP 服务器");
                addMissing(missing, config.getPassword(), "密码");
                addMissing(missing, config.getPort(), "端口");
                if (missing.isEmpty()) {
                    return "";
                }
                return "邮件插件未配置：" + String.join("、", missing);
            }

            private void addMissing(List<String> missing, String value, String label) {
                if (firstString(value).trim().isEmpty()) {
                    missing.add(label);
                }
            }

            private void sendEmail(EmailConfig config, EmailSendRequest request, SendContext context) throws Exception {
                List<File> files = new ArrayList<>();
                if (request.getFiles() != null) {
                    for (String str : request.getFiles()) {
                        files.add(new File(str));
                    }
                }

                PublicInfo responseSync = ioSession.getResponseSync(ContentType.JSON, new HashMap<>(), ActionType.LOAD_PUBLIC_INFO, PublicInfo.class);
                Map<String, Object> map = configMap(config);
                map.put("displayName", responseSync.getTitle());
                MailUtil.sendMail(context.to, context.title, context.content, map, files);
            }

            private String firstString(Object value) {
                if (value instanceof List && !((List) value).isEmpty()) {
                    return ((List) value).get(0).toString();
                }
                if (value instanceof String) {
                    return (String) value;
                }
                return value == null ? "" : String.valueOf(value);
            }

            private String source(EmailSendRequest request) {
                String sourcePluginName = firstString(request.getSourcePluginName());
                String notificationType = firstString(request.getNotificationType());
                if (!sourcePluginName.isEmpty() || !notificationType.isEmpty()) {
                    return "通知:" + (sourcePluginName.isEmpty() ? notificationType : sourcePluginName);
                }
                return "服务调用";
            }

            private EmailSendRequest payload(MsgPacket requestPacket, EmailSendRequest rawRequest) {
                if (rawRequest == null) {
                    return new EmailSendRequest();
                }
                return rawRequest.effectivePayload(ActionType.CAPABILITY_INVOKE.name().equals(requestPacket.getMethodStr()));
            }
        });
    }

    private void sendResponse(IOSession ioSession,
                              MsgPacket requestPacket,
                              EmailSendResponse response,
                              int status,
                              String error) {
        if (ActionType.CAPABILITY_INVOKE.name().equals(requestPacket.getMethodStr())) {
            CapabilityInvokeResult result = new CapabilityInvokeResult();
            result.setSuccess(status == 200);
            Map<String, Object> data = new HashMap<>();
            data.put("status", response.getStatus());
            result.setData(data);
            if (!result.isSuccess()) {
                result.setErrorMessage(error == null || error.trim().isEmpty() ? "send email failed" : error);
            }
            ioSession.sendJsonMsg(result, requestPacket.getMethodStr(), requestPacket.getMsgId(),
                    result.isSuccess() ? MsgPacketStatus.RESPONSE_SUCCESS : MsgPacketStatus.RESPONSE_ERROR);
            return;
        }
        ioSession.sendMsg(ContentType.JSON, response, requestPacket.getMethodStr(), requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
    }

    private static Map<String, Object> configMap(EmailConfig config) {
        Map<String, Object> map = new HashMap<>();
        map.put("to", config.getTo());
        map.put("from", config.getFrom());
        map.put("smtpServer", config.getSmtpServer());
        map.put("password", config.getPassword());
        map.put("port", config.getPort());
        return map;
    }

    private static class SendContext {
        private List<String> to = new ArrayList<>();
        private String title = "";
        private String content = "";
        private String source = "服务调用";
        private int attachmentCount;
    }
}
