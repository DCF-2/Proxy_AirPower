# Etapa 1: Compilação (Build)
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Execução (Run)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copia apenas o JAR gerado na etapa anterior
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080

# O ENTRYPOINT diz como o contentor deve arrancar
ENTRYPOINT ["java", "-jar", "app.jar"]