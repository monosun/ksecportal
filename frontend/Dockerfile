FROM node:20-alpine AS builder
WORKDIR /app
COPY frontend/package.json ./
RUN npm install
COPY frontend/ .
RUN npm run build

FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends \
        nginx \
        libnginx-mod-http-headers-more-filter \
    && rm -rf /var/lib/apt/lists/* \
    && rm -f /etc/nginx/sites-enabled/default
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx/error-pages /usr/share/nginx/html/error-pages
COPY nginx/nginx-http.conf /etc/nginx/conf.d/00-security.conf
COPY nginx/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
