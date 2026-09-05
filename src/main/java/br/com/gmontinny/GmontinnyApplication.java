package br.com.gmontinny;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableBatchProcessing
@EnableScheduling
public class GmontinnyApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmontinnyApplication.class, args);
    }
}
