FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/gestor-gastos-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65.0", "-jar", "app.jar"]
