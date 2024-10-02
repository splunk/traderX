package finos.traderx.messaging.socketio;

import java.util.Date;

import finos.traderx.messaging.Envelope;

public class SocketIOEnvelope<T> implements Envelope<T> {
    private String topic;
    private T payload;
    private Date date = new Date();
    private String from;
    private String type;
    private String traceParent;   // OpenTelemetry Traceparent


    public SocketIOEnvelope() {}

    public SocketIOEnvelope(String topic, T payload) {
        this.payload = payload;
        this.topic = topic;
        this.type = payload.getClass().getSimpleName();
    }

    // Setters
    public void setType(String type) {
        this.type = type;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setTraceParent(String traceParent) {
        this.traceParent = traceParent;
    }

    // Getters
    public String getType() {
        return type;
    }

    public String getTopic() {
        return topic;
    }

    public T getPayload() {
        return payload;
    }

    public Date getDate() {
        return date;
    }

    public String getFrom() {
        return from;
    }

    public String getTraceParent() {
        return traceParent;
    }

}