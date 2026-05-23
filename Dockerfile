FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S koins && adduser -S koins -G koins
COPY --from=builder /app/target/koins-backend-1.0.0.jar app.jar
RUN mkdir -p logs && chown -R koins:koins /app
USER koins
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
