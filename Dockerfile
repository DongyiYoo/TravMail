# build
FROM maven:3.9.6-eclipse-temurin-21 AS build
COPY . .
RUN mvn clean package -DskipTests

# RUN
FROM eclipse-temurin:21-jre
COPY --from=build /target/*.jar app.jar

ENV PORT=10000
EXPOSE 10000

# Run application
ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-jar", "app.jar"]