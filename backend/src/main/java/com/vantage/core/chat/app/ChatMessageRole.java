package com.vantage.core.chat.app;

public enum ChatMessageRole {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system"),
    TOOL("tool");

    private final String value;

    ChatMessageRole(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
