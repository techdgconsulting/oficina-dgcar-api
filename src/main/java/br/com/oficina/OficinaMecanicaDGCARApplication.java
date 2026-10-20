package br.com.oficina;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Sistema Integrado de Atendimento e Execução de Serviços
 * Oficina Mecânica DGCar - MVP (Fase 1)
 */
@SpringBootApplication
@EnableScheduling
public class OficinaMecanicaDGCARApplication {

    public static void main(String[] args) {
        SpringApplication.run(OficinaMecanicaDGCARApplication.class, args);
    }
}
