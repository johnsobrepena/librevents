FROM openjdk:21-jdk-slim

ARG JAVA_PORT
ENV JAVA_PORT=${JAVA_PORT:-8086}

COPY build/libs/server-0.0.1.jar /app/librevents-server.jar

CMD ["bash", "-c", "java -Dserver.port=${JAVA_PORT} -jar /app/librevents-server.jar"]
