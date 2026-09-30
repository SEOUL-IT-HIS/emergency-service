FROM eclipse-temurin:17-jdk-alpine

run mkdir /app
workdir /app

add ./build/libs/*.jar /app/app.jar

EXPOSE 8089
entrypoint ["java", "-jar", "app.jar"]