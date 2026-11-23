FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
ARG MODULE
RUN mvn -q -pl ${MODULE} -am -DskipTests package

FROM eclipse-temurin:17-jre
ARG MODULE
WORKDIR /app
COPY --from=build /app/${MODULE}/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]