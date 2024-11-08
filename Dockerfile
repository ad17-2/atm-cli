FROM maven:3.8.4-openjdk-11 as builder

WORKDIR /build

COPY pom.xml .

COPY src ./src

RUN mvn clean install -DskipTests

FROM openjdk:11-slim

WORKDIR /app

COPY --from=builder /build/target/atm-cli-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar

ENV TERM=xterm-256color

CMD ["java", "-jar", "app.jar"]