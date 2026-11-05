# Imagen del frontend en dos etapas: se compila con Node y se sirve con Nginx,
# así la imagen final no lleva Node ni node_modules.

# --- Construcción ---
FROM node:24-alpine AS construccion
WORKDIR /app
# Dependencias primero para aprovechar la caché de capas de Docker
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# --- Servidor ---
FROM nginx:1.29-alpine
# La imagen oficial de Nginx sustituye las variables de entorno en las plantillas de
# /etc/nginx/templates al arrancar: la dirección del backend se configura sin reconstruir
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=construccion /app/dist/frontend/browser /usr/share/nginx/html
ENV BACKEND_URL=http://backend:8080
EXPOSE 80
