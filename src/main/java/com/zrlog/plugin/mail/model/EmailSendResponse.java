package com.zrlog.plugin.mail.model;

public class EmailSendResponse {

    private int status;

    public EmailSendResponse() {
    }

    public EmailSendResponse(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
