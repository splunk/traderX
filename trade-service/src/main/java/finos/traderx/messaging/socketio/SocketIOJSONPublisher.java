package finos.traderx.messaging.socketio;

import java.net.URI;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import finos.traderx.messaging.PubSubException;
import finos.traderx.messaging.Publisher;
import io.opentelemetry.api.trace.Span;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.slf4j.Logger;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

public abstract class SocketIOJSONPublisher<T> implements Publisher<T>, InitializingBean {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private static final Logger log = LoggerFactory.getLogger(SocketIOJSONPublisher.class);

    boolean connected = false;

    @Override
    public boolean isConnected() {
        return connected;
    }

    Socket socket;

    String socketAddress = "http://localhost:3000";

    public void setSocketAddress(String addr) {
        socketAddress = addr;
    }

    String topic = "/default";

    public void setTopic(String t) {
        topic = t;
    }

    @Override
    public void publish(T message) throws PubSubException {
        publish(topic, message);
    }

    @Override
    public void publish(String topic, T message) throws PubSubException {
        if (!isConnected()) {
            throw new PubSubException("Cannot send %s on topic %s - not connected".formatted(message, topic));
        }
        try {
            // Create the envelope
            SocketIOEnvelope<T> envelope = new SocketIOEnvelope<>(topic, message);

            // Convert envelope to a JSON object
            String msgString = objectMapper.writerFor(SocketIOEnvelope.class).writeValueAsString(envelope);
            JSONObject obj = new JSONObject(msgString);
            log.info("PUBLISH-> Raw Payload: {}", obj.toString());

            // Extract the current span and build the traceparent
            Span currentSpan = Span.current();
            String traceParent = null;
            if (currentSpan.getSpanContext().isValid()) {
                String traceId = currentSpan.getSpanContext().getTraceId();
                String spanId = currentSpan.getSpanContext().getSpanId();
                String traceFlags = currentSpan.getSpanContext().getTraceFlags().asHex();

                // Construct the traceparent in the W3C Trace Context format
                traceParent = String.format("00-%s-%s-%s", traceId, spanId, traceFlags);
                log.info("Constructed traceParent: {}", traceParent);
            } else {
                log.warn("No valid span context available");
            }

            // Add traceParent to the message
            if (traceParent != null) {
                obj.put("traceParent", traceParent);
                log.info("Added traceParent to message: {}", traceParent);
            }

            // Emit the message to the socket
            socket.emit("publish", obj);
            log.info("Published message to topic {}: {}", topic, obj.toString());

        } catch (Exception x) {
            log.error("Error publishing message to topic {}: {}", topic, x.getMessage(), x);
            throw new PubSubException("Error publishing message", x);
        }
    }

    @Override
    public void disconnect() throws PubSubException {
        if (socket != null && isConnected())
            socket.disconnect();
        socket = null;
    }

    @Override
    public void connect() throws PubSubException {
        if (socket != null)
            socket.disconnect();
        try {
            socket = internalConnect(URI.create(socketAddress));
        } catch (Exception x) {
            throw new PubSubException("Cannot establish socket connection at " + socketAddress, x);
        }
    }

    protected Socket internalConnect(URI uri) throws Exception {
        return IO.socket(uri, getIOOptions());
    }

    protected IO.Options getIOOptions() {
        return new IO.Options();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        connect();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                SocketIOJSONPublisher.this.connected = true;
                log.info("Socket Connected {}", args);
            }
        });

        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                SocketIOJSONPublisher.this.connected = false;
                log.info("Socket Disconnected {}", args);
            }
        });

        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                SocketIOJSONPublisher.this.connected = false;
                log.info("Connection Error {}", args);
            }
        });
        socket.connect();
    }
}