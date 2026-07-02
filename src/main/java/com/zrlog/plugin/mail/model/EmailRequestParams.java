package com.zrlog.plugin.mail.model;

public class EmailRequestParams {

    private String to;
    private String from;
    private String smtpServer;
    private String password;
    private String port;
    private String emailLogRetentionDays;
    private String retentionDays;
    private String page;
    private String pageSize;
    private String keyword;
    private String status;

    public static EmailRequestParams fromParam(String to, String from, String smtpServer, String password, String port,
                                               String emailLogRetentionDays, String retentionDays, String page,
                                               String pageSize, String keyword, String status) {
        EmailRequestParams request = new EmailRequestParams();
        request.setTo(to);
        request.setFrom(from);
        request.setSmtpServer(smtpServer);
        request.setPassword(password);
        request.setPort(port);
        request.setEmailLogRetentionDays(emailLogRetentionDays);
        request.setRetentionDays(retentionDays);
        request.setPage(page);
        request.setPageSize(pageSize);
        request.setKeyword(keyword);
        request.setStatus(status);
        return request;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSmtpServer() {
        return smtpServer;
    }

    public void setSmtpServer(String smtpServer) {
        this.smtpServer = smtpServer;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getEmailLogRetentionDays() {
        return emailLogRetentionDays;
    }

    public void setEmailLogRetentionDays(String emailLogRetentionDays) {
        this.emailLogRetentionDays = emailLogRetentionDays;
    }

    public String getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(String retentionDays) {
        this.retentionDays = retentionDays;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
