package com.zrlog.plugin.mail.model;

import java.util.ArrayList;
import java.util.List;

public class EmailSendRequest {

    private Object to;
    private String title;
    private String content;
    private List<String> files = new ArrayList<>();
    private String sourcePluginName;
    private String notificationType;
    private EmailSendRequest payload;

    public EmailSendRequest effectivePayload(boolean capabilityInvoke) {
        if (capabilityInvoke && payload != null) {
            return payload;
        }
        return this;
    }

    public boolean hasRequiredContent() {
        return title != null && content != null;
    }

    public int attachmentCount() {
        return files == null ? 0 : files.size();
    }

    public Object getTo() {
        return to;
    }

    public void setTo(Object to) {
        this.to = to;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    public String getSourcePluginName() {
        return sourcePluginName;
    }

    public void setSourcePluginName(String sourcePluginName) {
        this.sourcePluginName = sourcePluginName;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public EmailSendRequest getPayload() {
        return payload;
    }

    public void setPayload(EmailSendRequest payload) {
        this.payload = payload;
    }
}
