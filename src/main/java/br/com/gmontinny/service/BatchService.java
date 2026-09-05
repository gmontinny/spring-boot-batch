package br.com.gmontinny.service;

import br.com.gmontinny.domain.repository.CnaeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class BatchService {

    private final JobOperator jobOperator;
    private final Job cnaeImportJob;
    private final JobRepository jobRepository;
    private final CnaeRepository cnaeRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public BatchService(JobOperator jobOperator,
                        Job cnaeImportJob,
                        JobRepository jobRepository,
                        CnaeRepository cnaeRepository) {
        this.jobOperator = jobOperator;
        this.cnaeImportJob = cnaeImportJob;
        this.jobRepository = jobRepository;
        this.cnaeRepository = cnaeRepository;
    }

    public String runCnaeImport() {
        try {
            log.info("[BATCH] Limpando tabela CNAE antes da importação");
            cnaeRepository.deleteAllInBatch();
            JobExecution execution = jobOperator.start(cnaeImportJob, buildParams());
            log.info("[BATCH] Job disparado — id={}, status={}", execution.getId(), execution.getStatus());
            return "Job iniciado com id: " + execution.getId() + " | Status: " + execution.getStatus();
        } catch (Exception e) {
            log.error("[BATCH] Falha ao disparar job", e);
            throw new RuntimeException("Falha ao iniciar o job de importação CNAE", e);
        }
    }

    // @Scheduled(cron = "${app.batch.cron:0 0 2 * * *}")
    public void runScheduled() {
        log.info("[BATCH] Execução agendada iniciada");
        runCnaeImport();
    }

    public Map<String, Object> getStatus(Long jobExecutionId) {
        JobExecution execution = jobRepository.getJobExecution(jobExecutionId);
        if (execution == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job execution não encontrado: " + jobExecutionId);

        var step = execution.getStepExecutions().stream().findFirst();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jobExecutionId", execution.getId());
        result.put("status", execution.getStatus().name());
        result.put("exitCode", execution.getExitStatus().getExitCode());
        result.put("startTime", execution.getStartTime() != null ? execution.getStartTime().format(FMT) : null);
        result.put("endTime", execution.getEndTime() != null ? execution.getEndTime().format(FMT) : null);
        step.ifPresent(s -> {
            result.put("lidos", s.getReadCount());
            result.put("gravados", s.getWriteCount());
            result.put("pulados", s.getSkipCount());
            result.put("filtrados", s.getFilterCount());
        });
        if (!execution.getAllFailureExceptions().isEmpty())
            result.put("erro", execution.getAllFailureExceptions().get(0).getMessage());
        return result;
    }

    private JobParameters buildParams() {
        return new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
    }
}
