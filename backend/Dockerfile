# ---- Etapa 1: build do jar com Maven + JDK 17 ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Baixa dependencias em cache de camada separada
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Etapa 2: imagem de runtime enxuta (JRE 17) ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# Banco SQLite persistido pelo volume montado em /app/data (ver docker-compose.yml)
RUN mkdir -p /app/data

COPY --from=build /app/target/emprestimos-backend-1.0.0.jar app.jar

ENV JAVA_OPTS=""
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]