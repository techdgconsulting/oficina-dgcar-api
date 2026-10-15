package br.com.oficina.infrastructure.observability;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import com.newrelic.api.agent.NewRelic;
import java.io.IOException;
import net.logstash.logback.composite.AbstractJsonProvider;

/** Preserves trace/entity metadata as JSON fields for the Kubernetes log collector. */
public class NewRelicLinkingJsonProvider extends AbstractJsonProvider<ILoggingEvent> {

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        for (var attribute : NewRelic.getAgent().getLinkingMetadata().entrySet()) {
            generator.writeStringField(attribute.getKey(), attribute.getValue());
        }
    }
}
