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
        label = "邮件通知",
        description = "通过 ZrLog 邮件配置发送标准通知",
        exposure = {"notification"},
        channel = "email",
        timeoutSeconds = 60
)
public class EmailService implements IPluginService {

    private static final Logger LOGGER = LoggerUtil.getLogger(EmailService.class);
    private static final EmailRepository REPOSITORY = EmailRepository.getInstance();

    @Override
    public void handle(final IOSession ioSession, final MsgPacket requestPacket) {
        final Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("key", "to,from,smtpServer,password,port");
        ioSession.sendJsonMsg(keyMap, ActionType.GET_WEBSITE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST, new IMsgPacketCallBack() {
            @Override
            public void handler(MsgPacket responseMsgPacket) {
                Map<String, Object> map = new Gson().fromJson(responseMsgPacket.getDataStr(), Map.class);
                if (map == null) {
                    map = new HashMap<>();
                }
                Map<String, Object> rawRequestMap = new Gson().fromJson(requestPacket.getDataStr(), Map.class);
                Map<String, Object> requestMap = payloadMap(requestPacket, rawRequestMap);
                Map<String, Object> response = new HashMap<>();
                SendContext sendContext = parseContext(map, requestMap);
                int status;
                String error = "";
                try {
                    if (requestMap.get("title") == null || requestMap.get("content") == null) {
                        status = 401;
                        error = "missing title or content";
                        REPOSITORY.record(ioSession, sendContext.to, sendContext.title, sendContext.source, false, status, error, sendContext.attachmentCount);
                    } else {
                        error = validateConfig(map, sendContext);
                        if (!error.isEmpty()) {
                            status = 400;
                            REPOSITORY.record(ioSession, sendContext.to, sendContext.title, sendContext.source, false, status, error, sendContext.attachmentCount);
                        } else {
                            status = 200;
                            sendEmail(map, requestMap, sendContext);
                            REPOSITORY.record(ioSession, sendContext.to, sendContext.title, sendContext.source, true, status, "", sendContext.attachmentCount);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "send email error ", e);
                    status = 500;
                    error = e.getMessage();
                    REPOSITORY.record(ioSession, sendContext.to, sendContext.title, sendContext.source, false, status, error, sendContext.attachmentCount);
                }
                response.put("status", status);
                sendResponse(ioSession, requestPacket, response, status, error);
            }

            private SendContext parseContext(Map<String, Object> map, Map<String, Object> requestMap) {
                SendContext context = new SendContext();
                if (requestMap.get("to") == null) {
                    addRecipient(context, map.get("to"));
                } else if (requestMap.get("to") instanceof List) {
                    for (Object item : (List) requestMap.get("to")) {
                        addRecipient(context, item);
                    }
                } else {
                    addRecipient(context, requestMap.get("to"));
                }
                context.title = firstString(requestMap.get("title"));
                context.content = firstString(requestMap.get("content"));
                if (requestMap.get("files") instanceof List) {
                    context.attachmentCount = ((List) requestMap.get("files")).size();
                }
                context.source = source(requestMap);
                return context;
            }

            private void addRecipient(SendContext context, Object value) {
                String recipient = firstString(value);
                if (!recipient.trim().isEmpty()) {
                    context.to.add(recipient.trim());
                }
            }

            private String validateConfig(Map<String, Object> map, SendContext context) {
                List<String> missing = new ArrayList<>();
                if (context.to.isEmpty()) {
                    missing.add("收件人");
                }
                addMissing(missing, map, "from", "发件人");
                addMissing(missing, map, "smtpServer", "SMTP 服务器");
                addMissing(missing, map, "password", "密码");
                addMissing(missing, map, "port", "端口");
                if (missing.isEmpty()) {
                    return "";
                }
                return "邮件插件未配置：" + String.join("、", missing);
            }

            private void addMissing(List<String> missing, Map<String, Object> map, String key, String label) {
                if (firstString(map.get(key)).trim().isEmpty()) {
                    missing.add(label);
                }
            }

            private void sendEmail(Map<String, Object> map, Map<String, Object> requestMap, SendContext context) throws Exception {
                List<File> files = new ArrayList<>();
                if (requestMap.get("files") != null && requestMap.get("files") instanceof List) {
                    List<String> fileStrList = (List<String>) requestMap.get("files");
                    for (String str : fileStrList) {
                        files.add(new File(str));
                    }
                }

                PublicInfo responseSync = ioSession.getResponseSync(ContentType.JSON, new HashMap<>(), ActionType.LOAD_PUBLIC_INFO, PublicInfo.class);
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

            private String source(Map<String, Object> requestMap) {
                String sourcePluginName = firstString(requestMap.get("sourcePluginName"));
                String notificationType = firstString(requestMap.get("notificationType"));
                if (!sourcePluginName.isEmpty() || !notificationType.isEmpty()) {
                    return "通知:" + (sourcePluginName.isEmpty() ? notificationType : sourcePluginName);
                }
                return "服务调用";
            }

            private Map<String, Object> payloadMap(MsgPacket requestPacket, Map<String, Object> rawRequestMap) {
                if (ActionType.CAPABILITY_INVOKE.name().equals(requestPacket.getMethodStr())
                        && rawRequestMap != null && rawRequestMap.get("payload") instanceof Map) {
                    return (Map<String, Object>) rawRequestMap.get("payload");
                }
                return rawRequestMap == null ? new HashMap<String, Object>() : rawRequestMap;
            }
        });
    }

    private void sendResponse(IOSession ioSession,
                              MsgPacket requestPacket,
                              Map<String, Object> response,
                              int status,
                              String error) {
        if (ActionType.CAPABILITY_INVOKE.name().equals(requestPacket.getMethodStr())) {
            CapabilityInvokeResult result = new CapabilityInvokeResult();
            result.setSuccess(status == 200);
            result.setData(response);
            if (!result.isSuccess()) {
                result.setErrorMessage(error == null || error.trim().isEmpty() ? "send email failed" : error);
            }
            ioSession.sendJsonMsg(result, requestPacket.getMethodStr(), requestPacket.getMsgId(),
                    result.isSuccess() ? MsgPacketStatus.RESPONSE_SUCCESS : MsgPacketStatus.RESPONSE_ERROR);
            return;
        }
        ioSession.sendMsg(ContentType.JSON, response, requestPacket.getMethodStr(), requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
    }

    private static class SendContext {
        private List<String> to = new ArrayList<>();
        private String title = "";
        private String content = "";
        private String source = "服务调用";
        private int attachmentCount;
    }
}
