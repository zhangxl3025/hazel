package com.zxl.hazel.event;

@FunctionalInterface
public interface EventCallback {
    void onEvent(Event event);
}