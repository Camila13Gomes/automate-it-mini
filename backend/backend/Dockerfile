FROM node:22-alpine AS frontend-build
WORKDIR /frontend

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

FROM eclipse-temurin:21-jdk AS backend-build
WORKDIR /app

COPY backend/backend/ ./
COPY --from=frontend-build /frontend/dist/ /app/src/main/resources/static/

RUN chmod +x mvnw
RUN ./mvnw -B clean verify

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=backend-build /app/target/*.jar app.jar
RUN groupadd --system app && useradd --system --gid app --home-dir /app app \
    && chown app:app /app/app.jar

USER app

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
