package com.zxl.hazel.plugin;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zxl.hazel.properties.PropertiesConfig;
import com.zxl.hazel.bean.BeanContainer;
import com.zxl.hazel.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * JDBC 数据源插件
 * SPI 自动加载，用户只需在配置文件中启用
 */
public class JdbcPlugin implements Plugin {

    private static final Logger log = LoggerFactory.getLogger(JdbcPlugin.class);
    private HikariDataSource dataSource;

    @Override
    public String name() {
        return "jdbc";
    }


    @Override
    public int order() {
        return 1; // 优先启动
    }

    @Override
    public Event doStart() {
        // 检查是否启用
        boolean enabled = PropertiesConfig.getBoolean("hazel.plugin.jdbc.enabled", true);
        if (!enabled) {
            log.info("JDBC Plugin is disabled");
            return Event.empty();
        }

        // 从配置读取 JDBC 参数
        String url = PropertiesConfig.get("hazel.plugin.jdbc.url", "jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1");
        if (url == null || url.isEmpty()) {
            log.warn("JDBC URL is not configured, skipping plugin");
            return Event.empty();
        }

        String driver = PropertiesConfig.get("hazel.plugin.jdbc.driver", "org.h2.Driver");
        String username = PropertiesConfig.get("hazel.plugin.jdbc.username", "sa");
        String password = PropertiesConfig.get("hazel.plugin.jdbc.password", "");
        int maxPoolSize = PropertiesConfig.getInt("hazel.plugin.jdbc.maxPoolSize", 10);
        int minIdle = PropertiesConfig.getInt("hazel.plugin.jdbc.minIdle", 5);

        // 创建连接池
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setDriverClassName(driver);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(PropertiesConfig.getLong("hazel.plugin.jdbc.connectionTimeout", 30000));
        config.setIdleTimeout(PropertiesConfig.getLong("hazel.plugin.jdbc.idleTimeout", 600000));

        try {
            dataSource = new HikariDataSource(config);

            // 验证连接
            try (Connection conn = dataSource.getConnection()) {
                log.info("Database connection verified");
            }

            // 注册到容器
            BeanContainer.register(DataSource.class, dataSource);

            // 初始化数据库脚本
            initializeDatabase();

            log.info("JDBC Plugin started: {}", url);
        } catch (Exception e) {
            log.error("Failed to start JDBC Plugin", e);
            throw new RuntimeException("JDBC Plugin startup failed", e);
        }

        return PluginEvent.JDBC_STARTED;
    }


    private void initializeDatabase() {
        String schemaFile = PropertiesConfig.get("hazel.plugin.jdbc.schema", "schema.sql");
        String dataFile = PropertiesConfig.get("hazel.plugin.jdbc.data", "data.sql");
        boolean initOnStart = PropertiesConfig.getBoolean("hazel.plugin.jdbc.initOnStart", true);

        if (!initOnStart) {
            log.info("Database initialization skipped (initOnStart=false)");
            return;
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 执行 schema.sql
            String schema = readResourceFile(schemaFile);
            if (schema != null && !schema.isEmpty()) {
                log.info("Executing schema: {}", schemaFile);
                executeSql(stmt, schema);
            }

            // 执行 data.sql
            String data = readResourceFile(dataFile);
            if (data != null && !data.isEmpty()) {
                log.info("Executing data: {}", dataFile);
                executeSql(stmt, data);
            }

            log.info("Database initialized successfully");
        } catch (SQLException | IOException e) {
            log.error("Failed to initialize database: {}", e.getMessage(), e);
        }
    }

    private void executeSql(Statement stmt, String sqlScript) throws SQLException {
        // 简单的分号分割执行，复杂场景建议使用专业的SQL解析器
        String[] statements = sqlScript.split(";");
        for (String sql : statements) {
            String trimmed = sql.trim();
            if (!trimmed.isEmpty()) {
                try {
                    stmt.execute(trimmed);
                } catch (SQLException e) {
                    log.warn("Failed to execute SQL: {}", trimmed, e);
                    // 继续执行其他语句
                }
            }
        }
    }

    private String readResourceFile(String fileName) throws IOException {
        // 支持 classpath: 前缀
        String path = fileName;
        if (fileName.startsWith("classpath:")) {
            path = fileName.substring(10);
        }

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                log.warn("Resource file not found: {}", fileName);
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }

    @Override
    public void stop() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("JDBC Plugin stopped");
        }
    }
}