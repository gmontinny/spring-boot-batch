package br.com.gmontinny;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GmontinnyApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmontinnyApplication.class, args);
    }
}
