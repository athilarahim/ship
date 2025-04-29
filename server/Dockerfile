# Using openjdk slim
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app
COPY ./server/src/Server.java .
RUN javac Server.java

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /app/Server.class .
EXPOSE 9000
CMD ["java", "Server"]