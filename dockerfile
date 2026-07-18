# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Run stage - JDK (not JRE) needed for javac + SpotBugs
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

RUN apk add --no-cache wget unzip && \
    wget -q https://github.com/spotbugs/spotbugs/releases/download/4.8.6/spotbugs-4.8.6.zip -O /tmp/spotbugs.zip && \
    unzip -q /tmp/spotbugs.zip -d /opt && \
    rm /tmp/spotbugs.zip && \
    apk del wget unzip

ENV SPOTBUGS_HOME=/opt/spotbugs-4.8.6

RUN mkdir -p /app/uploads /app/reports
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]