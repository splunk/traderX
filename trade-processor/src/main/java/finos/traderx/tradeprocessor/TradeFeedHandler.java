package finos.traderx.tradeprocessor;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.TextMapGetter;

import org.springframework.beans.factory.annotation.Autowired;

import finos.traderx.messaging.Envelope;
import finos.traderx.messaging.socketio.SocketIOJSONSubscriber;
import finos.traderx.tradeprocessor.model.TradeOrder;
import finos.traderx.tradeprocessor.service.TradeService;

import java.util.Date;  // Importing java.util.Date for working with date objects
import java.util.HashMap;
import java.util.Map;

public class TradeFeedHandler extends SocketIOJSONSubscriber<TradeOrder> {
    static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TradeFeedHandler.class);
    private static final Tracer tracer = GlobalOpenTelemetry.getTracer("trade-feed-handler");

    @Autowired
    private TradeService tradeService;

    public TradeFeedHandler() {
        super(TradeOrder.class);
    }

    @Override
    public void onMessage(Envelope<?> envelope, TradeOrder order) {
        try {
            // Extract traceparent and tracestate from the envelope
            String traceparent = envelope.getTraceParent();

            if (traceparent != null) {
                // Create a headers map to hold traceparent and tracestate
                Map<String, String> headers = new HashMap<>();
                headers.put("traceparent", traceparent);
       

                // Extract the context from the traceparent using W3C Trace Context Propagator
                Context extractedContext = W3CTraceContextPropagator.getInstance().extract(
                    Context.current(), headers, new TextMapGetter<Map<String, String>>() {
                        @Override
                        public Iterable<String> keys(Map<String, String> carrier) {
                            return carrier.keySet();
                        }

                        @Override
                        public String get(Map<String, String> carrier, String key) {
                            return carrier.get(key);
                        }
                    });

                // Create a child span using the extracted context
                // Extract necessary fields from the envelope
                String topic = envelope.getTopic();
                String messageType = envelope.getType();
                String from = envelope.getFrom();
                Date date = envelope.getDate();

                // Create a child span using the extracted context
                SpanBuilder spanBuilder = tracer.spanBuilder("process-trade")
                                .setParent(extractedContext)
                                .setSpanKind(SpanKind.CONSUMER) // This is a message consumption span

                                // Set messaging attributes from the envelope
                                .setAttribute("messaging.system", "socket.io") // The messaging system is socket.io
                                .setAttribute("messaging.destination", topic) // The topic (namespace or room in socket.io)
                                .setAttribute("messaging.destination_kind", "topic") // It's a topic-based messaging system
                                .setAttribute("messaging.operation", "process") // The operation being performed is processing
                                .setAttribute("messaging.protocol", "socket.io") // The protocol in use is socket.io
                                .setAttribute("messaging.message_id", traceparent) // Use the traceparent as the message ID
                                .setAttribute("messaging.message_type", messageType) // The type of message (e.g., "TradeOrder")
                                .setAttribute("messaging.conversation_id", from) // Optional: Use the "from" field as conversation ID if applicable
                                .setAttribute("messaging.message_date", date.toString()); // Capture the message date for context

                Span span = spanBuilder.startSpan();
                try {
                    log.info("Started child span with traceparent: {}", traceparent);
                    
                    // Process the trade order
                    tradeService.processTrade(order);
                } finally {
                    span.end();
                    log.info("Child span ended.");
                }
            } else {
                log.warn("No traceparent found in the envelope.");
                // Proceed without tracing
                tradeService.processTrade(order);
            }
        } catch (Exception ex) {
            log.error("Error processing trade order {} in envelope {}", order, envelope);
            log.error("Error handling incoming trade order:", ex);
        }
    }
}