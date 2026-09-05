package br.com.gmontinny.batch;

import br.com.gmontinny.domain.repository.CnaeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.cloud.vault.enabled=false",
        "spring.config.import=",
        "spring.liquibase.enabled=false",
        "spring.batch.job.enabled=false",
        "spring.batch.jdbc.initialize-schema=always",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:batchtest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "app.jwt.secret=3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b",
        "app.rate-limit.login-capacity=100",
        "app.rate-limit.api-capacity=1000",
        "server.port=0"
})
@DisplayName("Batch Job — Integração com JobRepository")
class BatchJobIntegrationTest {

    @MockitoBean ConnectionFactory connectionFactory;
    @MockitoBean RabbitTemplate rabbitTemplate;
    @MockitoBean ProxyManager<String> rateLimitProxyManager;

    @Autowired JobLauncher asyncJobLauncher;
    @Autowired Job cnaeImportJob;
    @Autowired JobRepository jobRepository;
    @Autowired CnaeRepository cnaeRepository;

    @Test
    @DisplayName("Job deve completar e persistir metadados no JobRepository")
    void jobDeveCompletarEPersistirMetadados() throws Exception {
        var params = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = asyncJobLauncher.run(cnaeImportJob, params);

        // Aguarda conclusão (AsyncJobLauncher é não-bloqueante)
        long deadline = System.currentTimeMillis() + 30_000;
        while (execution.isRunning() && System.currentTimeMillis() < deadline) {
            Thread.sleep(500);
            execution = jobRepository.getLastJobExecution(cnaeImportJob.getName(), params);
            if (execution == null) break;
        }

        assertThat(execution).isNotNull();
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(cnaeRepository.count()).isGreaterThan(0);

        var step = execution.getStepExecutions().iterator().next();
        assertThat(step.getReadCount()).isGreaterThan(0);
        assertThat(step.getWriteCount()).isEqualTo(step.getReadCount());
    }
}
