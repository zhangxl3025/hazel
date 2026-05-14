package com.zxl.hazel.event;

import java.util.Objects;

public interface Event {
    String source();   // 事件来源

    static Event empty() {
        return () -> "";
    }
}