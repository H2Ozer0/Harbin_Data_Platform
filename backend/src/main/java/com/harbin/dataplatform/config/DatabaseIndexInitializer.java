package com.harbin.dataplatform.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

/**
 * 应用启动后自动执行 ddl-indexes.sql（CREATE INDEX IF NOT EXISTS），
 * 幂等操作，可安全重复执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseIndexInitializer {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void createIndexes() {
        try {
            ClassPathResource resource = new ClassPathResource("ddl-indexes.sql");
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            // 按分号分割，逐条执行（跳过空行和注释）
            String[] statements = sql.split(";");
            for (String stmt : statements) {
                String trimmed = stmt.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                // 去掉行内注释后执行
                String clean = trimmed.replaceAll("--.*$", "").trim();
                if (!clean.isEmpty()) {
                    log.info("Executing: {}", clean.substring(0, Math.min(clean.length(), 120)));
                    jdbcTemplate.execute(clean);
                }
            }
            log.info("All performance indexes created/verified successfully.");
        } catch (Exception e) {
            log.warn("Failed to create indexes (non-fatal): {}", e.getMessage());
        }
    }
}
