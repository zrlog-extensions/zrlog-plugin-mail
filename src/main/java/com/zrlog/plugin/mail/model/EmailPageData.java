package com.zrlog.plugin.mail.model;

import com.zrlog.plugin.message.Plugin;

public class EmailPageData {

    private boolean dark;
    private String colorPrimary;
    private Plugin plugin;
    private EmailConfig config;
    private Object summary;
    private Object trend;
    private Object logs;

    public boolean isDark() {
        return dark;
    }

    public void setDark(boolean dark) {
        this.dark = dark;
    }

    public String getColorPrimary() {
        return colorPrimary;
    }

    public void setColorPrimary(String colorPrimary) {
        this.colorPrimary = colorPrimary;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    public EmailConfig getConfig() {
        return config;
    }

    public void setConfig(EmailConfig config) {
        this.config = config;
    }

    public Object getSummary() {
        return summary;
    }

    public void setSummary(Object summary) {
        this.summary = summary;
    }

    public Object getTrend() {
        return trend;
    }

    public void setTrend(Object trend) {
        this.trend = trend;
    }

    public Object getLogs() {
        return logs;
    }

    public void setLogs(Object logs) {
        this.logs = logs;
    }
}
