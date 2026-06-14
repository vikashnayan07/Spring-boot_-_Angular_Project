package com.tcs.Machcare.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationSchemaInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(NotificationSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public NotificationSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        run("create schema if not exists dev");
        run("""
                create table if not exists dev.notification (
                    notification_id bigserial primary key,
                    recipient_emp_id bigint,
                    recipient_role_id integer,
                    title varchar(255) not null,
                    message varchar(1000) not null,
                    category varchar(255) not null,
                    severity varchar(255),
                    reference_type varchar(255),
                    reference_id varchar(255),
                    is_read boolean not null default false,
                    created_at timestamp not null default now()
                )
                """);
        run("alter table dev.notification add column if not exists recipient_emp_id bigint");
        run("alter table dev.notification add column if not exists recipient_role_id integer");
        run("alter table dev.notification add column if not exists title varchar(255)");
        run("alter table dev.notification add column if not exists message varchar(1000)");
        run("alter table dev.notification add column if not exists category varchar(255)");
        run("alter table dev.notification add column if not exists severity varchar(255)");
        run("alter table dev.notification add column if not exists reference_type varchar(255)");
        run("alter table dev.notification add column if not exists reference_id varchar(255)");
        run("alter table dev.notification add column if not exists is_read boolean not null default false");
        run("alter table dev.notification add column if not exists created_at timestamp not null default now()");
        run("create index if not exists idx_notification_emp_created on dev.notification (recipient_emp_id, created_at desc)");
        run("create index if not exists idx_notification_emp_unread on dev.notification (recipient_emp_id, is_read)");
    }

    private void run(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (RuntimeException ex) {
            log.warn("Notification schema initialization skipped statement: {}", ex.getMessage());
        }
    }
}
