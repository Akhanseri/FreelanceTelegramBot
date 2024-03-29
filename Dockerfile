FROM openjdk:17-slim
COPY target/*.jar application.jar
ENTRYPOINT ["java","-jar","application.jar"]
