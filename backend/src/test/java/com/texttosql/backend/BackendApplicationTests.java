package com.texttosql.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import javax.sql.DataSource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.temp.use_jdbc_metadata_defaults=false",
        "LLM_URL=http://localhost:8000",
        "FRONTEND_URL=http://localhost:4200",
        "POSTGRES_URL=jdbc:postgresql://localhost:5432/texttosql",
        "POSTGRES_USER=test",
        "POSTGRES_PASSWORD=test",
        "JWT_SECRET=myultra-secure-and-very-long-dummy-secret-key-for-testing-purposes",
        "JWT_REFRESH_EXPIRATION=86400000",
        "JWT_ACCESS_EXPIRATION=3600000",
        "MAIL_HOST=localhost",
        "MAIL_PORT=587",
        "MAIL_USERNAME=test",
        "MAIL_PASSWORD=test",
        "LOG_LEVEL=INFO"
})
class BackendApplicationTests {

    @MockitoBean
    private JavaMailSenderImpl javaMailSender;

    @MockitoBean
    private DataSource dataSource;

    @Test
    void contextLoads() {
    }

}
