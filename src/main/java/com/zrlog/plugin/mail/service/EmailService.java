package com.zrlog.plugin.mail.service;

import com.google.gson.Gson;
import com.zrlog.plugin.IMsgPacketCallBack;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.api.IPluginService;
import com.zrlog.plugin.api.Service;
import com.zrlog.plugin.common.IdUtil;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.model.PublicInfo;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.mail.util.MailUtil;
import com.zrlog.plugin.type.ActionType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service("emailService")
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
                Map<String, Object> requestMap = new Gson().fromJson(requestPacket.getDataStr(), Map.class);
                Map<String, Object> response = new HashMap<>();
                SendContext sendContext = parseContext(map, requestMap);
                try {
                    if (requestMap.get("title") == null || requestMap.get("content") == null) {
                        response.put("status", 401);
                        REPOSITORY.record(ioSession, sendContext.to, sendContext.title, "服务调用", false, 401, "missing title or content", sendContext.attachmentCount);
                    } else {
                        response.put("status", 200);
                        sendEmail(map, requestMap, sendContext);
                        REPOSITORY.record(ioSession, sendContext.to, sendContext.title, "服务调用", true, 200, "", sendContext.attachmentCount);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "send email error ", e);
                    response.put("status", 500);
                    REPOSITORY.record(ioSession, sendContext.to, sendContext.title, "服务调用", false, 500, e.getMessage(), sendContext.attachmentCount);
                }

                ioSession.sendMsg(ContentType.JSON, response, requestPacket.getMethodStr(), requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
            }

            private SendContext parseContext(Map<String, Object> map, Map<String, Object> requestMap) {
                SendContext context = new SendContext();
                if (requestMap.get("to") == null && map.get("to") != null) {
                    context.to.add(map.get("to").toString());
                } else if (requestMap.get("to") instanceof List) {
                    context.to = (List) requestMap.get("to");
                } else if (requestMap.get("to") instanceof String) {
                    context.to.add(requestMap.get("to").toString());
                }
                context.title = firstString(requestMap.get("title"));
                context.content = firstString(requestMap.get("content"));
                if (requestMap.get("files") instanceof List) {
                    context.attachmentCount = ((List) requestMap.get("files")).size();
                }
                return context;
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
                return "";
            }
        });
    }

    private static class SendContext {
        private List<String> to = new ArrayList<>();
        private String title = "";
        private String content = "";
        private int attachmentCount;
    }
}
