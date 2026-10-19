package br.com.oficina.adapters.in.web.controller;

import br.com.oficina.application.port.out.TokenProviderPort;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = SanitizedErrorController.class,
    excludeAutoConfiguration = {
        UserDetailsServiceAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
    }
)
@Tag("TratamentoDeErros")
class SanitizedErrorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenProviderPort tokenProviderPort;

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE"})
    void acessoDiretoNaoRepresentaFalhaInterna(String method) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), "/error"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.erro").value("Recurso nao encontrado"))
            .andExpect(jsonPath("$.trace").doesNotExist())
            .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void acessoDiretoHeadRetorna404() throws Exception {
        mockMvc.perform(head("/error"))
            .andExpect(status().isNotFound());
    }

    @Test
    void encaminhamentoDeErroSemStatusPreservaFalhaInterna() throws Exception {
        mockMvc.perform(get("/error").with(request -> {
                request.setDispatcherType(DispatcherType.ERROR);
                return request;
            }))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.erro").value("Erro interno do servidor"));
    }

    @Test
    void erroPadraoNaoExpoeCamposSensiveis() throws Exception {
        mockMvc.perform(get("/error")
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/veiculos/10")
                .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new RuntimeException("falha interna"))
                .requestAttr(RequestDispatcher.ERROR_MESSAGE, "detalhe tecnico"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.erro").value("Erro interno do servidor"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").doesNotExist())
            .andExpect(jsonPath("$.error").doesNotExist())
            .andExpect(jsonPath("$.exception").doesNotExist())
            .andExpect(jsonPath("$.trace").doesNotExist())
            .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    void erroPadrao404NaoExpoePath() throws Exception {
        mockMvc.perform(get("/error")
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/rota-interna"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.erro").value("Recurso nao encontrado"))
            .andExpect(jsonPath("$.path").doesNotExist())
            .andExpect(jsonPath("$.error").doesNotExist());
    }
}
