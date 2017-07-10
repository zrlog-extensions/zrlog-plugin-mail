package com.zrlog.plugin.mail;


import com.zrlog.plugin.client.NioClient;
import com.zrlog.plugin.mail.controller.MailController;
import com.zrlog.plugin.mail.service.EmailService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Start {

    public static void main(String[] args) throws IOException {
        List<Class> classList = new ArrayList<>();
        classList.add(MailController.class);
        new NioClient().connectServer(args, classList, EmailPluginAction.class, EmailService.class);
    }
}

