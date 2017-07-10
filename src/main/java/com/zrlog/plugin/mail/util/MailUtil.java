package com.zrlog.plugin.mail.util;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.util.*;

public class MailUtil {


    private MailUtil() {
    }

    public static boolean sendMail(String to, String title, String content, Map<String, Object> sMTPMap, List<File> files)
            throws Exception {
        List<String> tos = new ArrayList<>();
        tos.add(to);
        return sendMail(tos, title, content, sMTPMap, files);
    }

    public static boolean sendMail(List<String> to, String title, String content, Map<String, Object> sMTPMap, List<File> files) throws Exception {
        final Properties prop = getMailSettings(sMTPMap);
        String displayName = prop.getProperty("mail.smtp.displayName");
        Authenticator sMTPAuth = new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(prop.getProperty("mail.smtp.username"), prop.getProperty("mail.smtp.password"));
            }
        };
        final Session session = Session.getDefaultInstance(prop, sMTPAuth);
        final Message message = new MimeMessage(session);
        Address address = new InternetAddress(
                prop.getProperty("mail.smtp.from"), displayName);
        InternetAddress[] addresses = new InternetAddress[to.size()];
        for (int i = 0; i < to.size(); i++) {
            addresses[i] = new InternetAddress(to.get(i));
        }
        message.setFrom(address);
        message.setRecipients(Message.RecipientType.TO, addresses);
        message.setSubject(title);
        Multipart mp = new MimeMultipart();
        MimeBodyPart mbp = new MimeBodyPart();
        mbp.setContent(content, "text/html;charset=utf-8");
        mp.addBodyPart(mbp);

        // 附件功能
        for (File file : files) {
            mbp = new MimeBodyPart();
            if (file.exists()) {
                FileDataSource fds = new FileDataSource(file);
                mbp.setDataHandler(new DataHandler(fds));
                mbp.setFileName(fds.getName());
                mp.addBodyPart(mbp);
            }
        }

        message.setContent(mp);
        message.setSentDate(new Date());
        message.saveChanges();
        new Thread() {
            public void run() {
                try {
                    Transport tran = session.getTransport("smtp");
                    tran.connect(prop.getProperty("mail.smtp.host"),
                            prop.getProperty("mail.smtp.username"),
                            prop.getProperty("mail.smtp.password"));
                    tran.sendMessage(message, message.getAllRecipients());
                    tran.close();
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        }.start();
        return true;
    }

    public static boolean sendMail(List<String> to, String title, String content, Map<String, Object> sMTPMap)
            throws Exception {
        return sendMail(to, title, content, sMTPMap, new ArrayList<File>());
    }

    private static Properties getMailSettings(Map<String, Object> sMTPMap) {
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.displayName", "ZrLog系统邮件");
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.username", sMTPMap.get("from").toString());
        properties.setProperty("mail.smtp.port", sMTPMap.get("port").toString());
        properties.setProperty("mail.smtp.password", sMTPMap.get("password").toString());
        properties.setProperty("mail.smtp.host", sMTPMap.get("smtpServer").toString());
        properties.setProperty("mail.smtp.from", sMTPMap.get("from").toString());
        return properties;
    }
}
