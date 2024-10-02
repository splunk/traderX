package finos.traderx.messaging.socketio;

import java.net.URI;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import finos.traderx.messaging.Envelope;
import finos.traderx.messaging.PubSubException;
import finos.traderx.messaging.Subscriber;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Simple socketIO Subscriber, which uses 3 commands - 'subscribe',
 * 'unsubscribe', and 'publish' followed by payload.
 * Publish events consist of an envelope and an internal payload.
 */
public abstract class SocketIOJSONSubscriber<T> implements Subscriber<T>, InitializingBean {
    private static ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public SocketIOJSONSubscriber(Class<T> typeClass) {
        JavaType type = objectMapper.getTypeFactory().constructParametricType(SocketIOEnvelope.class, typeClass);
        this.envelopeType = type;
        this.objectType = typeClass;
    }

    protected IO.Options getIOOptions() {
        return new IO.Options();
    }

    final JavaType envelopeType;
    final Class<T> objectType;

    org.slf4j.Logger log = LoggerFactory.getLogger(this.getClass().getName());

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

    private String defaultTopic = "/default";

    public void setDefaultTopic(String topic) {
        defaultTopic = topic;
    }

    public abstract void onMessage(Envelope<?> envelope, T message);

    @Override
    public void subscribe(String topic) throws PubSubException {
        log.info("Subscribing to " + topic);
        socket.emit("subscribe", topic);
    }

    @Override
    public void unsubscribe(String topic) throws PubSubException {
        socket.emit("unsubscribe", topic);
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
        Socket s = IO.socket(uri, getIOOptions());
        s.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                SocketIOJSONSubscriber.this.connected = true;
                log.info("Socket Connected");
            }
        });

        s.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                SocketIOJSONSubscriber.this.connected = false;
                log.info("Socket Disconnected");
            }
        });

        s.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                SocketIOJSONSubscriber.this.connected = false;
                log.info("Connection Error");
            }
        });

        // Handle 'publish' events
        s.on("publish", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try {
                    JSONObject json = (JSONObject) args[0]; 
                    log.info("Raw Payload " + args[0].toString());

                    // Check if the message type matches the expected objectType
                    if (!objectType.getSimpleName().equals(json.get("type"))) {
                        log.info("System Message>>>>> " + args[0].toString());
                    } else {
                        // Parse the JSON object into a SocketIOEnvelope
                        SocketIOEnvelope<T> envelope = objectMapper.readValue(json.toString(), envelopeType);

                        // Extract the traceParent if it exists in the incoming JSON
                        if (json.has("traceParent")) {
                            String traceParent = json.getString("traceParent");
                            log.info("Extracted traceParent: " + traceParent);

                            // Set the traceParent in the envelope using the setter
                            envelope.setTraceParent(traceParent);
                            json.remove("traceParent"); // Remove traceParent field from the raw JSON
                            log.info("traceParent removed from the raw message");
                        }

                        log.info("Incoming Payload: " + envelope.getPayload());
                        SocketIOJSONSubscriber.this.onMessage(envelope, envelope.getPayload());
                    }

                } catch (Exception x) {
                    log.error("Threw exception while handling incoming message", x);
                }
            }
        });

        s.connect();
        return s;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        connect();
        subscribe(defaultTopic);
    }

    // Helper function to log a JSONObject and its structure recursively
    private void logJSON(JSONObject json) {
        for (String key : json.keySet()) {
            Object value = json.get(key);

            if (value instanceof JSONObject) {
                log.info("Key: " + key + " (nested JSONObject):");
                logJSON((JSONObject) value);  // Recursively log nested JSONObject
            } else if (value instanceof JSONArray) {
                log.info("Key: " + key + " (JSONArray):");
                logJSONArray((JSONArray) value);  // Log JSONArray contents
            } else {
                log.info("Key: " + key + ", Value: " + value);
            }
        }
    }

    // Helper function to log a JSONArray and its structure
    private void logJSONArray(JSONArray array) {
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONObject) {
                log.info("Index [" + i + "] is a JSONObject:");
                logJSON((JSONObject) value);  // Recursively log JSONObject in the array
            } else if (value instanceof JSONArray) {
                log.info("Index [" + i + "] is a JSONArray:");
                logJSONArray((JSONArray) value);  // Recursively log JSONArray in the array
            } else {
                log.info("Index [" + i + "] Value: " + value);
            }
        }
    }
}