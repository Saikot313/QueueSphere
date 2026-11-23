package com.qflow.queue;

import com.qflow.common.web.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(GlobalExceptionHandler.class)
public class QueueServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(QueueServiceApplication.class, args);
    }
}