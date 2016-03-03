package com.fzb.zrlog.plugin.mail;

import com.fzb.zrlog.plugin.IOSession;
import com.fzb.zrlog.plugin.api.IPluginAction;
import com.fzb.zrlog.plugin.data.codec.HttpRequestInfo;
import com.fzb.zrlog.plugin.data.codec.MsgPacket;
import com.fzb.zrlog.plugin.mail.controller.MailController;

public class EmailPluginAction implements IPluginAction {
    @Override
    public void start(IOSession ioSession, MsgPacket msgPacket) {

    }

    @Override
    public void stop(IOSession ioSession, MsgPacket msgPacket) {

    }

    @Override
    public void install(IOSession ioSession, MsgPacket msgPacket, HttpRequestInfo httpRequestInfo) {
        new MailController(ioSession, msgPacket, httpRequestInfo).index();
    }

    @Override
    public void uninstall(IOSession ioSession, MsgPacket msgPacket) {

    }
}
