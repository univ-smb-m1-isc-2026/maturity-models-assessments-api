FROM eclipse-temurin:17-jre-alpine

COPY ./target/maturity-models-assessments-api-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:InitialRAMPercentage=50", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]