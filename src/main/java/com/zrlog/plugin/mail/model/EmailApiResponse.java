package com.zrlog.plugin.mail.model;

public class EmailApiResponse<T> {

    private boolean success;
    private T data;

    public EmailApiResponse() {
    }

    private EmailApiResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
    }

    public static EmailApiResponse<Void> success() {
        return new EmailApiResponse<Void>(true, null);
    }

    public static <T> EmailApiResponse<T> success(T data) {
        return new EmailApiResponse<T>(true, data);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
