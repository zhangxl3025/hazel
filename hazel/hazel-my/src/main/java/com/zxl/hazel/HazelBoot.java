package com.zxl.hazel;

import com.zxl.hazel.annotation.*;
import com.zxl.hazel.bean.BeanContainer;
import com.zxl.hazel.event.EventTransfer;
import com.zxl.hazel.plugin.PluginManager;
import com.zxl.hazel.trace.Tracer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.jar.JarFile;

/**
 * Hazel 启动类
 */
public class HazelBoot {

    private static final Logger log = LoggerFactory.getLogger(HazelBoot.class);

    private static String[] scanPackages;
    private static final CountDownLatch shutdownLatch = new CountDownLatch(1);

    /**
     * 启动应用
     */
    public static void run(Class<?> primarySource, String[] args) {
        long startTime = System.currentTimeMillis();

        // 解析注解
        HazelBootApplication application = primarySource.getAnnotation(HazelBootApplication.class);
        if (application != null && application.scanPackages().length > 0) {
            scanPackages = application.scanPackages();
        } else {
            scanPackages = new String[]{primarySource.getPackage().getName()};
        }

        log.info("Hazel Boot starting, scan packages: {}", Arrays.toString(scanPackages));

        try {
            // 1. 加载配置
            loadConfig();

            // 2. 扫描类
            Set<Class<?>> classes = scanClasses();
            log.info("Scanned {} classes", classes.size());

            // 3. 注册 Bean
            registerBeansAndListeners(classes);

            // 4. 依赖注入
            injectDependencies();

            // 5. 执行 @PostConstruct
            invokePostConstruct();

            // 6. 加载并启动插件
            PluginManager.loadPlugins();
            PluginManager.startPlugins();

            // 7. 注册关闭钩子
            registerShutdownHook();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Hazel Boot started successfully in {} ms", elapsed);

            // 8. 等待应用关闭
            waitForShutdown();

        } catch (Exception e) {
            log.error("Hazel Boot failed to start", e);
            System.exit(1);
        }
    }

    /**
     * 等待应用关闭
     */
    private static void waitForShutdown() {
        try {
            shutdownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Hazel Boot interrupted");
        }
    }

    /**
     * 通知应用关闭
     */
    public static void shutdown() {
        log.info("Hazel Boot shutting down...");
        shutdownLatch.countDown();
    }

    /**
     * 加载配置
     */
    private static void loadConfig() {
        Tracer.beginChain("loadConfig run");
        // HazelConfig 静态块已自动加载
        log.info("Configuration loaded");
        Tracer.endSpan();
    }

    /**
     * 扫描所有类
     */
    private static Set<Class<?>> scanClasses() {
        Set<Class<?>> classes = new HashSet<>();
        for (String packageName : scanPackages) {
            scanPackage(packageName, classes);
        }
        return classes;
    }

    /**
     * 扫描包下所有类（支持文件系统和 JAR）
     */
    private static void scanPackage(String packageName, Set<Class<?>> classes) {
        String path = packageName.replace('.', '/');

        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader()
                    .getResources(path);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String protocol = resource.getProtocol();

                if ("file".equals(protocol)) {
                    // 文件系统扫描
                    File directory = new File(resource.getFile());
                    scanDirectory(directory, packageName, classes);
                } else if ("jar".equals(protocol)) {
                    // JAR 包扫描
                    scanJar(resource, packageName, classes);
                }
            }
        } catch (Exception e) {
            log.error("Failed to scan package: {}", packageName, e);
        }
    }

    /**
     * 扫描文件目录
     */
    private static void scanDirectory(File directory, String packageName, Set<Class<?>> classes) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (ClassNotFoundException e) {
                    log.debug("Class not found: {}", className);
                }
            }
        }
    }

    /**
     * 扫描 JAR 包
     */
    private static void scanJar(URL jarUrl, String packageName, Set<Class<?>> classes) throws IOException, URISyntaxException {
        String jarPath = jarUrl.getFile();
        if (jarPath.contains("!")) {
            jarPath = jarPath.substring(0, jarPath.indexOf("!"));
        }
        
        // 处理 file: 前缀
        if (jarPath.startsWith("file:")) {
            jarPath = jarPath.substring(5);
        }
        
        String packagePath = packageName.replace('.', '/');
        
        try (JarFile jarFile = new JarFile(new File(new URI(jarPath).getPath()))) {
            jarFile.stream()
                    .filter(entry -> entry.getName().startsWith(packagePath) && entry.getName().endsWith(".class"))
                    .forEach(entry -> {
                        String className = entry.getName().replace("/", ".").replace(".class", "");
                        try {
                            Class<?> clazz = Class.forName(className);
                            classes.add(clazz);
                        } catch (ClassNotFoundException e) {
                            log.debug("Class not found: {}", className);
                        } catch (NoClassDefFoundError e) {
                            log.debug("Class definition not found: {}", className);
                        }
                    });
        }
    }

    /**
     * 注册 Bean
     */
    private static void registerBeansAndListeners(Set<Class<?>> classes) {
        for (Class<?> clazz : classes) {
            if (hasComponentAnnotation(clazz)) {
                try {
                    Object instance = clazz.getDeclaredConstructor().newInstance();
                    BeanContainer.register(clazz, instance);
                    EventTransfer.registerListenerMethods(instance);
                    log.debug("Registered bean: {}", clazz.getName());
                } catch (Exception e) {
                    log.error("Failed to create bean: {}", clazz.getName(), e);
                }
            }
        }
    }

    private static boolean hasComponentAnnotation(Class<?> clazz) {
        return clazz.isAnnotationPresent(Component.class) ||
                clazz.isAnnotationPresent(Service.class) ||
                clazz.isAnnotationPresent(Controller.class) ||
                clazz.isAnnotationPresent(Repository.class) ||
                clazz.isAnnotationPresent(Singleton.class);
    }

    /**
     * 依赖注入
     */
    private static void injectDependencies() {
        for (Class<?> clazz : BeanContainer.getAllClasses()) {
            Object instance = BeanContainer.getBean(clazz);
            if (instance == null) continue;

            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Inject.class)) {
                    Class<?> fieldType = field.getType();
                    Object dependency = BeanContainer.getBean(fieldType);

                    if (dependency == null) {
                        // 尝试按名称获取
                        String fieldName = field.getName();
                        dependency = BeanContainer.getBean(fieldName);
                    }

                    if (dependency == null) {
                        log.warn("No bean found for type: {}", fieldType.getName());
                        continue;
                    }

                    field.setAccessible(true);
                    try {
                        field.set(instance, dependency);
                        log.debug("Injected: {} -> {}", fieldType.getSimpleName(), clazz.getSimpleName());
                    } catch (IllegalAccessException e) {
                        log.error("Failed to inject: {}", field.getName(), e);
                    }
                }
            }
        }
    }

    /**
     * 执行 @PostConstruct
     */
    private static void invokePostConstruct() {
        for (Class<?> clazz : BeanContainer.getAllClasses()) {
            Object instance = BeanContainer.getBean(clazz);
            if (instance == null) continue;

            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(PostConstruct.class)) {
                    method.setAccessible(true);
                    try {
                        method.invoke(instance);
                        log.debug("PostConstruct invoked: {}.{}", clazz.getSimpleName(), method.getName());
                    } catch (Exception e) {
                        log.error("Failed to invoke @PostConstruct: {}.{}", clazz.getSimpleName(), method.getName(), e);
                    }
                }
            }
        }
    }

    /**
     * 注册关闭钩子
     */
    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Hazel Boot shutting down...");

            // 执行 @PreDestroy
            for (Class<?> clazz : BeanContainer.getAllClasses()) {
                Object instance = BeanContainer.getBean(clazz);
                if (instance == null) continue;

                Method[] methods = clazz.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.isAnnotationPresent(PreDestroy.class)) {
                        method.setAccessible(true);
                        try {
                            method.invoke(instance);
                        } catch (Exception e) {
                            log.error("Failed to invoke @PreDestroy: {}.{}", clazz.getSimpleName(), method.getName(), e);
                        }
                    }
                }
            }

            // 停止插件
            PluginManager.stopPlugins();

            // 通知关闭
            shutdown();

            log.info("Hazel Boot shutdown complete");
        }));
    }
}