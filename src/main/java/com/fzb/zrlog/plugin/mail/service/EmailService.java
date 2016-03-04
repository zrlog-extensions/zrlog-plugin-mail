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
                Map map = new JSONDeserializer<Map>().deserialize(responseMsgPacket.getDataStr());
                Map<String, List> requestMap = new JSONDeserializer<Map>().deserialize(requestPacket.getDataStr());
                Map<String, Object> response = new HashMap<>();
                try {
                    boolean error = false;
                    if (requestMap.get("title") == null || requestMap.get("content") == null) {
                        error = true;
                    }
                    if (requestMap.get("to") == null) {
                        List<String> strList = new ArrayList<>();
                        strList.add(map.get("to").toString());
                        requestMap.put("to", strList);
                    }
                    if (error) {
                        response.put("status", 401);
                    } else {
                        MailUtil.sendMail(requestMap.get("to"), requestMap.get("title").get(0).toString(), requestMap.get("content").get(0).toString(), map);
                        response.put("status", 200);
                    }
                } catch (Exception e) {
                    LOGGER.error("send email error ", e);
                    response.put("status", 500);
                }
                ioSession.sendMsg(ContentType.JSON, response, requestPacket.getMethodStr(), requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
            }
        });
    }
}
