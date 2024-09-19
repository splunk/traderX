# Use an official Node.js image as the base image
FROM node:18-bullseye

# Set the working directory inside the container
WORKDIR /trade-feed

# Copy the package.json and package-lock.json (if available)
COPY package*.json ./


# Install Splunk OpenTelemetry Agent  & other dependencies for Node.js
RUN npm install @splunk/otel @opentelemetry/api @opentelemetry/instrumentation-socket.io


# Install dependencies
RUN npm install

# Copy the application code to the container
COPY . .

# Install Splunk OpenTelemetry Agent for Node.js
#RUN npm install @splunk/otel @opentelemetry/api @opentelemetry/instrumentation-socket.io


# Expose the application port
EXPOSE 18086

# Start the app with OpenTelemetry enabled
ENTRYPOINT ["npm", "start", "run"]

#ENTRYPOINT [ "tail", "-f", "/dev/null" ]