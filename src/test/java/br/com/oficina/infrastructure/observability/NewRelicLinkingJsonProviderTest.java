package br.com.oficina.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.NewRelic;
import java.io.StringWriter;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NewRelicLinkingJsonProviderTest {

    @Test
    void deveSerializarMetadadosComoCamposJsonSemAlterarMensagem() throws Exception {
        var agent = mock(Agent.class);
        when(agent.getLinkingMetadata()).thenReturn(Map.of(
            "trace.id", "trace-123", "span.id", "span-456", "entity.name", "oficina-api"));
        try (var newRelic = mockStatic(NewRelic.class)) {
            newRelic.when(NewRelic::getAgent).thenReturn(agent);
            var result = new ObjectMapper().readTree(serialize());
            assertThat(result.get("trace.id").asText()).isEqualTo("trace-123");
            assertThat(result.get("span.id").asText()).isEqualTo("span-456");
            assertThat(result.get("entity.name").asText()).isEqualTo("oficina-api");
            assertThat(result.get("message").asText()).isEqualTo("teste \"JSON\"\nlinha 2");
        }
    }

    @Test
    void deveProduzirJsonValidoSemAgenteNewRelic() throws Exception {
        var result = new ObjectMapper().readTree(serialize());
        assertThat(result.get("message").asText()).contains("JSON");
        assertThat(result.has("trace.id")).isFalse();
    }

    private String serialize() throws Exception {
        var output = new StringWriter();
        try (var generator = new ObjectMapper().getFactory().createGenerator(output)) {
            generator.writeStartObject();
            generator.writeStringField("message", "teste \"JSON\"\nlinha 2");
            new NewRelicLinkingJsonProvider().writeTo(generator, mock(ILoggingEvent.class));
            generator.writeEndObject();
        }
        return output.toString();
    }
}
