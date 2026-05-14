package com.zxl.hazel;

import com.zxl.hazel.annotation.HazelBootApplication;
import com.zxl.hazel.trace.Tracer;

// 启动类
@HazelBootApplication
public class Application {

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        Tracer.beginChain("Application run");
        HazelBoot.run(Application.class, args);
        System.out.println("Application run in " + (System.currentTimeMillis() - startTime) + "ms");
        Tracer.clearChain();
    }
}