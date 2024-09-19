const sockio = require("socket.io");
const app = require('express')();
const winston = require('winston');
const http = require('http').createServer(app);

// OpenTelemetry setup
const { trace, context } = require("@opentelemetry/api");

const io = new sockio.Server(http, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"]
  }
});

const port = 18086;

const log = winston.createLogger({
  transports: [
    new winston.transports.Console()
  ]
});

// command names
const SUBSCRIBE = "subscribe";
const UNSUBSCRIBE = "unsubscribe";
const PUBLISH = "publish";

app.get('/', (req, res) => {
  res.sendFile(__dirname + '/index.html');
});

// Updated wrapMessage to include traceParent in W3C format
function wrapMessage(sender, topic, payloadType, payload) {
  const activeSpan = trace.getSpan(context.active());
  
  // Default traceParent values if there's no active span
  const traceParent = activeSpan 
    ? `00-${activeSpan.spanContext().traceId}-${activeSpan.spanContext().spanId}-${activeSpan.spanContext().traceFlags.toString(16).padStart(2, '0')}`
    : "00-00000000000000000000000000000000-0000000000000000-00";

  return {
    type: payloadType || "message",
    from: sender,
    topic: topic,
    date: new Date().getTime(),
    payload: payload,
    traceParent: traceParent
  };
}

function joinMessage(user, topic) {
  return wrapMessage('System', topic, 'message', { message: `New Joiner ${user} to topic ${topic}` });
}

function leaveMessage(user, topic) {
  return wrapMessage('System', topic, 'message', { message: `${user} has left ${topic}` });
}

function broadcast(from, data) {
  var message = wrapMessage(from, data.topic, data.type, data.payload);
  log.info(`Publish ${data.topic} -> ${JSON.stringify(message)}`);
  io.sockets.in([data.topic, "/*"]).emit(PUBLISH, message);
}

io.on('connection', (socket) => {
  log.info(`New Connection from ${socket.id}`);

  socket.on(SUBSCRIBE, (topic) => {
    log.info(`Subscribe ${topic}`);
    socket.join(topic);
    broadcast('System', joinMessage(socket.id, topic));
  });

  socket.on(UNSUBSCRIBE, (topic) => {
    log.info(`Unsubscribe ${topic}`);
    broadcast('System', leaveMessage(socket.id, topic));
    socket.leave(topic);
  });

  socket.on(PUBLISH, (data) => {
    broadcast(socket.id, data);
  });
});

http.listen(port, () => {
  log.info(`Socket.IO server running at http://localhost:${port}/`);
});