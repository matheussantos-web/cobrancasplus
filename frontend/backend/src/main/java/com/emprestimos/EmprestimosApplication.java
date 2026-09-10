package com.emprestimos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EmprestimosApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmprestimosApplication.class, args);
    }
}