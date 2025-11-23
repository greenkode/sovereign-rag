FROM eclipse-temurin:21-jdk AS builder

RUN apt-get update && apt-get install -y curl && apt-get clean

COPY identity-app/target/*.jar /opt/app.jar

WORKDIR /opt

CMD ["java", "-jar" , "app.jar"]

EXPOSE 80