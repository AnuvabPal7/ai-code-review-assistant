# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Run stage - JDK (not JRE) needed for javac + SpotBugs
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Download and bundle SpotBugs CLI
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