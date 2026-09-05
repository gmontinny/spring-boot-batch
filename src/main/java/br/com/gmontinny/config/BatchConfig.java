package br.com.gmontinny.config;

import br.com.gmontinny.batch.CnaeRow;
import br.com.gmontinny.batch.processor.CnaeItemProcessor;
import br.com.gmontinny.batch.reader.CnaeExcelReader;
import br.com.gmontinny.batch.writer.CnaeItemWriter;
import br.com.gmontinny.domain.entity.Cnae;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private final CnaeItemProcessor processor;
    private final CnaeItemWriter writer;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    public static final String JOB_NAME = "cnaeImportJob";

    @Value("${app.batch.chunk-size:100}")
    private int chunkSize;

    @Value("${app.batch.skip-limit:10}")
    private int skipLimit;

    @Value("${app.batch.thread-pool-size:4}")
    private int threadPoolSize;

    // -------------------------------------------------------------------------
    // TaskExecutor — processamento paralelo (Processamento Paralelo)
    // -------------------------------------------------------------------------
    @Bean
    public TaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolSize);
        executor.setMaxPoolSize(threadPoolSize * 2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("batch-worker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    // -------------------------------------------------------------------------
    // AsyncJobLauncher — execução assíncrona (Utilização de Recursos / Não bloqueia HTTP)
    // -------------------------------------------------------------------------
    @Bean
    @Primary
    public JobLauncher asyncJobLauncher() throws Exception {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(new SimpleAsyncTaskExecutor("job-launcher-"));
        launcher.afterPropertiesSet();
        return launcher;
    }

    // -------------------------------------------------------------------------
    // Reader
    // -------------------------------------------------------------------------
    @Bean
    public CnaeExcelReader cnaeExcelReader() {
        return new CnaeExcelReader();
    }

    // -------------------------------------------------------------------------
    // Step — chunk + faultTolerant + retry + skip (Tolerância a Falhas / Reinício)
    // -------------------------------------------------------------------------
    @Bean
    public Step cnaeImportStep() {
        return new StepBuilder("cnaeImportStep", jobRepository)
                .<CnaeRow, Cnae>chunk(chunkSize, transactionManager)
                .reader(cnaeExcelReader())
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                    .skipLimit(skipLimit)
                    .skip(Exception.class)
                    .retryLimit(3)
                    .retry(Exception.class)
                .listener(stepExecutionListener())
                .build();
    }

    // -------------------------------------------------------------------------
    // Job — RunIdIncrementer garante nova instância (Capacidade de Reiniciar)
    // -------------------------------------------------------------------------
    @Bean
    public Job cnaeImportJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(cnaeImportStep())
                .build();
    }

    // -------------------------------------------------------------------------
    // Listener — métricas de execução (Consistência / Observabilidade)
    // -------------------------------------------------------------------------
    @Bean
    public StepExecutionListener stepExecutionListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                log.info("[BATCH] Iniciando step '{}' — jobExecutionId={}",
                        stepExecution.getStepName(),
                        stepExecution.getJobExecutionId());
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                log.info("[BATCH] Step '{}' finalizado — lidos={}, gravados={}, filtrados={}, pulados={}, status={}",
                        stepExecution.getStepName(),
                        stepExecution.getReadCount(),
                        stepExecution.getWriteCount(),
                        stepExecution.getFilterCount(),
                        stepExecution.getSkipCount(),
                        stepExecution.getStatus());
                return stepExecution.getExitStatus();
            }
        };
    }
}
