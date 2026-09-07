# Stage 1: Build stage
FROM node:22-alpine AS build

WORKDIR /app

# Copy package files and install dependencies (layer caching)
COPY package.json package-lock.json ./
RUN npm ci

# Copy source and build
COPY . .
RUN npm run build

# Stage 2: Runtime stage
FROM nginx:alpine

# Copy built Angular app
COPY --from=build /app/dist/account-app/browser /usr/share/nginx/html

# Copy custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
