package com.fzb.zrlog.plugin.mail;


import com.fzb.zrlog.plugin.client.NioClient;
import com.fzb.zrlog.plugin.mail.controller.MailController;
import com.fzb.zrlog.plugin.mail.service.EmailService;
import com.fzb.zrlog.plugin.render.FreeMarkerRenderHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Start {
    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        List<Class> classList = new ArrayList<>();
        classList.add(MailController.class);
        new NioClient(null, new FreeMarkerRenderHandler()).connectServerByProperties(args, classList, "/plugin.properties", EmailPluginAction.class, EmailService.class);
    }
}

