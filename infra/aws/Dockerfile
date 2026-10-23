# Build on the developer machine or CI; EC2 only pulls this ARM64 image.
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar
USER spring:spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

