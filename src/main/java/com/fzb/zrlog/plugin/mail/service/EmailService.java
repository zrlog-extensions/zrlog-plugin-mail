package com.fzb.zrlog.plugin.mail.service;

import com.fzb.zrlog.plugin.IMsgPacketCallBack;
import com.fzb.zrlog.plugin.IOSession;
import com.fzb.zrlog.plugin.api.IPluginService;
import com.fzb.zrlog.plugin.api.Service;
import com.fzb.zrlog.plugin.common.IdUtil;
import com.fzb.zrlog.plugin.data.codec.ContentType;
import com.fzb.zrlog.plugin.data.codec.MsgPacket;
import com.fzb.zrlog.plugin.data.codec.MsgPacketStatus;
import com.fzb.zrlog.plugin.mail.util.MailUtil;
import com.fzb.zrlog.plugin.type.ActionType;
import flexjson.JSONDeserializer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

@Service("emailService")
public class EmailService implements IPluginService {

    private static Logger LOGGER = Logger.getLogger(EmailService.class);

    @Override
    public void handle(final IOSession ioSession, final MsgPacket requestPacket) {
        final Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("key", "to,from,smtpServer,password,port");
        ioSession.sendJsonMsg(keyMap, ActionType.GET_WEBSITE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST, new IMsgPacketCallBack() {
            @Override
            public void handler(MsgPacket responseMsgPacket) {
                Map<String, Object> map = new JSONDeserializer<Map<String, Object>>().deserialize(responseMsgPacket.getDataStr());
                Map<String, Object> requestMap = new JSONDeserializer<Map>().deserialize(requestPacket.getDataStr());
                Map<String, Object> response = new HashMap<>();
                try {
                    if (requestMap.get("title") == null || requestMap.get("content") == null) {
                        response.put("status", 401);
                    } else {
                        response.put("status", 200);
                        sendEmail(map, requestMap);
                    }
                } catch (Exception e) {
                    LOGGER.error("send email error ", e);
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
