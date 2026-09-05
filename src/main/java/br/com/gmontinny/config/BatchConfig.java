package br.com.gmontinny.config;

import br.com.gmontinny.batch.CnaeRow;
import br.com.gmontinny.batch.processor.CnaeItemProcessor;
import br.com.gmontinny.batch.reader.CnaeExcelReader;
import br.com.gmontinny.batch.writer.CnaeItemWriter;
import br.com.gmontinny.domain.entity.Cnae;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class BatchConfig extends DefaultBatchConfiguration {

    private final CnaeItemProcessor processor;
    private final CnaeItemWriter writer;
    private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;

    public BatchConfig(CnaeItemProcessor processor,
                       CnaeItemWriter writer,
                       DataSource dataSource,
                       PlatformTransactionManager transactionManager) {
        this.processor = processor;
        this.writer = writer;
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
    }

    public static final String JOB_NAME = "cnaeImportJob";

    @Value("${app.batch.chunk-size:100}")
    private int chunkSize;

    @Value("${app.batch.skip-limit:10}")
    private int skipLimit;

    @Value("${app.batch.thread-pool-size:4}")
    private int threadPoolSize;

    @Override
    public JobRepository jobRepository() {
        try {
            JdbcJobRepositoryFactoryBean factory = new JdbcJobRepositoryFactoryBean();
            factory.setDataSource(dataSource);
            factory.setTransactionManager(transactionManager);
            factory.setTablePrefix("BATCH_");
            factory.afterPropertiesSet();
            JobRepository repo = factory.getObject();
            log.info("[BATCH-DIAG] JobRepository class: {}", repo.getClass().getName());
            return repo;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao criar JdbcJobRepository", e);
        }
    }

    @Override
    protected PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Override
    protected TaskExecutor getTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolSize);
        executor.setMaxPoolSize(threadPoolSize * 2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("job-launcher-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    @Bean
    public CnaeExcelReader cnaeExcelReader() {
        return new CnaeExcelReader();
    }

    @Bean
    public Step cnaeImportStep(JobRepository jobRepository) {
        return new StepBuilder("cnaeImportStep", jobRepository)
                .<CnaeRow, Cnae>chunk(chunkSize)
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

    @Bean
    public Job cnaeImportJob(JobRepository jobRepository, Step cnaeImportStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(cnaeImportStep)
                .build();
    }

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
