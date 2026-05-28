package com.zrlog.plugin.mail.model;

import java.util.ArrayList;
import java.util.List;

public class EmailLogStore {

    private List<EmailLogEntry> items = new ArrayList<>();

    public List<EmailLogEntry> getItems() {
        return items;
    }

    public void setItems(List<EmailLogEntry> items) {
        this.items = items;
    }
}
