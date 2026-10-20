package br.com.oficina.adapters.in.web.controller;

import br.com.oficina.infrastructure.demo.DashboardHistoricoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/homologacao/dashboard")
@ConditionalOnProperty(name = "oficina.dashboard.massa-historica.enabled", havingValue = "true")
public class DashboardHistoricoController {

    private final DashboardHistoricoService service;

    @PostMapping("/massa-historica")
    @PreAuthorize("hasRole('GESTOR')")
    public List<DashboardHistoricoService.Cenario> gerar() {
        return service.gerar();
    }
}
