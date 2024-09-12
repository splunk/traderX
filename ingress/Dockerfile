#FROM nginx:1.27.1-otel
FROM nginx

EXPOSE 8080
ARG NGINX_HOST="localhost"
ENV NGINX_HOST=$NGINX_HOST

# This is a workaround for the dollar sign in the envsubst command
ARG DOLLAR="$"
ENV DOLLAR=$DOLLAR

#COPY nginx.traderx.nginx.conf.template /etc/nginx/nginx.traderx.nginx.conf.template
COPY nginx.traderx.conf.template /etc/nginx/conf.d/nginx.traderx.conf.template

RUN envsubst < /etc/nginx/conf.d/nginx.traderx.conf.template > /etc/nginx/conf.d/default.conf
