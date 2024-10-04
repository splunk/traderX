const { start } = require('@splunk/otel');
const { getInstrumentations } = require('@splunk/otel/lib/instrumentations');

const instrumentations = getInstrumentations();
const code_version = "0.1.4"

for (const instrumentation of instrumentations) {
  if (instrumentation.instrumentationName === '@opentelemetry/instrumentation-socket.io') {
    instrumentation.setConfig({
        emitHook: (span, { moduleVersion, payload }) => {
          //console.log("===== Inside EmitHook =====");
          //console.log("Module Version: ", moduleVersion);
          //console.log("Payload:", JSON.stringify(payload, null, 2)); // Log the entire payload

          if (!span) {    
            console.log('No active span found in emitHook.');
          }
       
          if (Array.isArray(payload) && payload.length > 0) {
            const firstPayload = payload[0]; // Access the first element
  
            // Check if the traceParent exists in the firstPayload and update it
            if (typeof firstPayload === 'object' && firstPayload !== null) {
              // Get the active span from the current context
              const activeSpan = trace.getSpan(context.active());
  
              // Generate the traceParent string
              const traceParent = activeSpan 
                ? `00-${activeSpan.spanContext().traceId}-${activeSpan.spanContext().spanId}-${activeSpan.spanContext().traceFlags.toString(16).padStart(2, '0')}`
                : "00-00000000000000000000000000000000-0000000000000000-00";
  
              // Safely set the traceParent in the first element of the payload
              firstPayload.traceParent = traceParent;
  
              console.log(`Updated traceParent: ${traceParent}`);
            } else {
              console.log("First payload is not an object. Cannot set traceParent.");
            }
          } else {
            console.log("Payload is not an array or is empty.");
          }
  
          console.log("===== Exiting EmitHook =====");
        }
    });
  }
}

start({
  serviceName: 'trade-feed',
  tracing: {
    instrumentations,
  },
});

const sockio = require("socket.io");
const app = require('express')();
const winston = require('winston');
const http = require('http').createServer(app);
const { trace, context } = require('@opentelemetry/api');

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

// Command names
const SUBSCRIBE = "subscribe";
const UNSUBSCRIBE = "unsubscribe";
const PUBLISH = "publish";

app.get('/', (req, res) => {
  res.sendFile(__dirname + '/index.html');
});

function wrapMessage(sender, topic, payloadType, payload) {
  return {
    type: payloadType || "message",
    from: sender,
    topic: topic,
    date: new Date().getTime(),
    payload: payload,
    traceParent: ""
  };
}

function joinMessage(user, topic) {
  return wrapMessage('System', topic, 'message', { message: `New Joiner ${user} to topic ${topic}` });
}

function leaveMessage(user, topic) {
  return wrapMessage('System', topic, 'message', { message: `${user} has left ${topic}` });
}

function broadcast(from, data) {
  let message = wrapMessage(from, data.topic, data.type, data.payload);
  log.info(`Publish ${data.topic} -> ${JSON.stringify(message)}`);
  //io.sockets.in([data.topic, "/*"]).emit(PUBLISH, message);
  //io.to(data.topic).emit(PUBLISH, message);    // Emit to the topic room
  io.emit(PUBLISH, message);
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
  log.info(`Socket.IO server ${code_version} running at http://localhost:${port}/`);
});