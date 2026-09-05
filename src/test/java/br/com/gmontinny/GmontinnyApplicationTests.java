package br.com.gmontinny;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.core.launch.JobLauncher;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.cloud.vault.enabled=false",
        "spring.config.import=",
        "spring.liquibase.enabled=false",
        "spring.batch.job.enabled=false",
        "spring.batch.jdbc.initialize-schema=always",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "app.jwt.secret=3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b",
        "app.jwt.expiration-ms=900000",
        "app.jwt.refresh-expiration-ms=604800000",
        "app.rate-limit.login-capacity=100",
        "app.rate-limit.api-capacity=1000",
        "server.port=0"
})
@DisplayName("GmontinnyApplication — Teste de Contexto")
class GmontinnyApplicationTests {

    @MockitoBean ConnectionFactory connectionFactory;
    @MockitoBean RabbitTemplate rabbitTemplate;
    @MockitoBean JobLauncher asyncJobLauncher;
    @MockitoBean ProxyManager<String> rateLimitProxyManager;

    @Test
    @DisplayName("Contexto Spring deve carregar sem erros")
    void contextLoads() {
    }
}
