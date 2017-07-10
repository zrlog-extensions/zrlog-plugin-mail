package com.zrlog.plugin.mail.service;

import com.zrlog.plugin.IMsgPacketCallBack;
import com.zrlog.plugin.IOSession;
import com.zrlog.plugin.api.IPluginService;
import com.zrlog.plugin.api.Service;
import com.zrlog.plugin.common.IdUtil;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.data.codec.ContentType;
import com.zrlog.plugin.data.codec.MsgPacket;
import com.zrlog.plugin.data.codec.MsgPacketStatus;
import com.zrlog.plugin.mail.util.MailUtil;
import com.zrlog.plugin.type.ActionType;
import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service("emailService")
public class EmailService implements IPluginService {

    private static Logger LOGGER = LoggerUtil.getLogger(EmailService.class);

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
                try {
                    if (requestMap.get("title") == null || requestMap.get("content") == null) {
                        response.put("status", 401);
                    } else {
                        response.put("status", 200);
                        sendEmail(map, requestMap);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "send email error ", e);
                    response.put("status", 500);
                }
                ioSession.sendMsg(ContentType.JSON, response, requestPacket.getMethodStr(), requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
            }

            private void sendEmail(Map<String, Object> map, Map<String, Object> requestMap) throws Exception {
                List<String> to = new ArrayList<>();
                String content = "";
                String title = "";
                if (requestMap.get("to") == null) {
                    to.add(map.get("to").toString());
                } else if (requestMap.get("to") instanceof List) {
                    to = (List) requestMap.get("to");
                } else if (requestMap.get("to") instanceof String) {
                    to.add(requestMap.get("to").toString());
                }
                //parse title
                if (requestMap.get("title") instanceof List) {
                    title = ((List) requestMap.get("title")).get(0).toString();
                } else if (requestMap.get("title") instanceof String) {
                    title = (String) requestMap.get("title");
                }
                //parse content
                if (requestMap.get("content") instanceof List) {
                    content = ((List) requestMap.get("content")).get(0).toString();
                } else if (requestMap.get("content") instanceof String) {
                    content = (String) requestMap.get("content");
                }
                List<File> files = new ArrayList<>();
                if (requestMap.get("files") != null && requestMap.get("files") instanceof List) {
                    List<String> fileStrList = (List<String>) requestMap.get("files");
                    for (String str : fileStrList) {
                        files.add(new File(str));
                    }
                }

                MailUtil.sendMail(to, title, content, map, files);
            }
        });
    }
}
