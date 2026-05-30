package com.zrlog.plugin.mail;

import com.zrlog.plugin.common.PluginNativeImageUtils;
import com.zrlog.plugin.mail.controller.MailController;
import com.zrlog.plugin.mail.model.EmailConfig;
import com.zrlog.plugin.mail.model.EmailLogEntry;
import com.zrlog.plugin.mail.model.EmailLogStore;
import com.zrlog.plugin.mail.service.EmailService;
import com.zrlog.plugin.mail.util.MailUtil;
import jakarta.activation.CommandMap;
import jakarta.activation.MailcapCommandMap;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GraalvmAgentApplication {


    public static void main(String[] args) throws IOException {
        PluginNativeImageUtils.gsonNativeAgentByClazz(Arrays.asList(EmailConfig.class, EmailLogEntry.class, EmailLogStore.class));
        String basePath = System.getProperty("user.dir").replace("\\target", "").replace("/target", "");
        //PathKit.setRootPath(basePath);
        File file = new File(basePath + "/src/main/resources");
        PluginNativeImageUtils.doLoopResourceLoad(file.listFiles(), file.getPath() + "/", "/");
        //Application.nativeAgent = true;
        PluginNativeImageUtils.exposeController(Collections.singletonList(MailController.class));
        PluginNativeImageUtils.usedGsonObject();
        exposePluginReflectivePaths();
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

    private static void exposePluginReflectivePaths() {
        try {
            EmailPluginAction.class.newInstance();
            EmailService.class.newInstance();
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
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
