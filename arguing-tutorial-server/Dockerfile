FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 80
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-jar", "app.jar"]