package com.fzb.zrlog.plugin.mail.controller;

import com.fzb.zrlog.plugin.IMsgPacketCallBack;
import com.fzb.zrlog.plugin.IOSession;
import com.fzb.zrlog.plugin.common.IdUtil;
import com.fzb.zrlog.plugin.data.codec.ContentType;
import com.fzb.zrlog.plugin.data.codec.HttpRequestInfo;
import com.fzb.zrlog.plugin.data.codec.MsgPacket;
import com.fzb.zrlog.plugin.data.codec.MsgPacketStatus;
import com.fzb.zrlog.plugin.mail.util.JtwigUtil;
import com.fzb.zrlog.plugin.mail.util.MailUtil;
import com.fzb.zrlog.plugin.type.ActionType;
import flexjson.JSONDeserializer;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xiaochun on 2016/2/13.
 */
public class MailController {

    private static Logger LOGGER = Logger.getLogger(MailController.class);

    private IOSession session;
    private MsgPacket requestPacket;
    private HttpRequestInfo requestInfo;

    public MailController(IOSession session, MsgPacket requestPacket, HttpRequestInfo requestInfo) {
        this.session = session;
        this.requestPacket = requestPacket;
        this.requestInfo = requestInfo;
    }

    public void update() {
        session.sendMsg(new MsgPacket(requestInfo.simpleParam(), ContentType.JSON, MsgPacketStatus.SEND_REQUEST, IdUtil.getInt(), ActionType.SET_WEBSITE.name()), new IMsgPacketCallBack() {
            @Override
            public void handler(MsgPacket msgPacket) {
                Map<String, Object> map = new HashMap<>();
                map.put("success", true);
                session.sendMsg(new MsgPacket(map, ContentType.JSON, MsgPacketStatus.RESPONSE_SUCCESS, requestPacket.getMsgId(), requestPacket.getMethodStr()));
            }
        });
    }

    public void index() {
        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("key", "to,from,smtpServer,password,port");
        session.sendJsonMsg(keyMap, ActionType.GET_WEBSITE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST, new IMsgPacketCallBack() {
            @Override
            public void handler(MsgPacket msgPacket) {
                Map map = new JSONDeserializer<Map>().deserialize(msgPacket.getDataStr());
                map.put("url", requestInfo.getUrl());
                session.sendMsg(ContentType.HTML, JtwigUtil.render(MailController.class.getResourceAsStream("/templates/index.twig.html"), map,session.getPlugin()), requestPacket.getMethodStr(), requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
            }
        });

    }

    public void testEmailService() {
        Map<String, Object> keyMap = new HashMap<>();
        keyMap.put("key", "to,from,smtpServer,password,port");
        session.sendJsonMsg(keyMap, ActionType.GET_WEBSITE.name(), IdUtil.getInt(), MsgPacketStatus.SEND_REQUEST, new IMsgPacketCallBack() {
            @Override
            public void handler(MsgPacket msgPacket) {
                Map map = new JSONDeserializer<Map>().deserialize(msgPacket.getDataStr());
                Map<String, Object> response = new HashMap<>();
                try {
                    MailUtil.sendMail(map.get("to").toString(), "这是一封测试邮件", "<h3>这是一封测试邮件</h3>\n", map);
                    response.put("status", 200);
                } catch (Exception e) {
                    LOGGER.error("send email error ", e);
                    response.put("status", 500);
                }
                session.sendMsg(ContentType.JSON, response, requestPacket.getMethodStr(), requestPacket.getMsgId(), MsgPacketStatus.RESPONSE_SUCCESS);
            }
        });

    }
}
