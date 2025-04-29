# Using openjdk slim
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app
COPY ./client/src/Client.java .
RUN javac Client.java

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/Client.class .
EXPOSE 8080
CMD ["java", "Client"]