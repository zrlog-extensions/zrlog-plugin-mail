package com.zrlog.plugin.mail;

import com.google.gson.Gson;
import com.sun.mail.handlers.multipart_mixed;
import com.sun.mail.handlers.text_html;
import com.sun.mail.handlers.text_plain;
import com.sun.mail.handlers.text_xml;
import com.zrlog.plugin.common.PluginNativeImageUtils;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.mail.controller.MailController;
import com.zrlog.plugin.mail.util.MailUtil;
import com.zrlog.plugin.message.Plugin;

import jakarta.activation.CommandMap;
import jakarta.activation.MailcapCommandMap;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GraalvmAgentApplication {


    public static void main(String[] args) throws IOException {
        new Gson().toJson(new HttpRequestInfo());
        new Gson().toJson(new Plugin());
        //new Gson().toJson(new User());
        //new Gson().toJson(new CommentsEntry());
        String basePath = System.getProperty("user.dir").replace("\\target", "").replace("/target", "");
        //PathKit.setRootPath(basePath);
        File file = new File(basePath + "/src/main/resources");
        PluginNativeImageUtils.doLoopResourceLoad(file.listFiles(), file.getPath() + "/", "/");
        //Application.nativeAgent = true;
        PluginNativeImageUtils.exposeController(Collections.singletonList(MailController.class));
        PluginNativeImageUtils.usedGsonObject();
        Map<String, Object> smtpMap = new HashMap<>();
        smtpMap.put("to", "test@testto.com");
        smtpMap.put("from", "test@testto.com");
        smtpMap.put("smtpServer", "127.0.0.1");
        smtpMap.put("password", "password");
        smtpMap.put("port", 8888);
        try {
            setupMail();
            MailUtil.sendMail("test@test.com", "test-native-agent", "no content <br/>", smtpMap, new ArrayList<>());
        } catch (Exception e) {
            e.printStackTrace();
        }
        Application.main(args);

    }

    public static void setupMail() {
        // Ensure necessary handlers are registered
        MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        mc.addMailcap("multipart/mixed;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        CommandMap.setDefaultCommandMap(mc);

        // Explicitly load classes to ensure they are included in the native image
        try {
            Class.forName("com.sun.mail.smtp.SMTPTransport");
            Class.forName("com.sun.mail.smtp.SMTPSSLTransport");
            Class.forName("com.sun.mail.handlers.multipart_mixed");
            Class.forName("com.sun.mail.handlers.text_plain");
            Class.forName("com.sun.mail.handlers.text_html");
            Class.forName("com.sun.mail.handlers.text_xml");
            Class.forName("jakarta.activation.MailcapCommandMap");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}