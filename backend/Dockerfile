# ---- Etapa 1: build do jar com Maven + JDK 17 ----
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Baixa dependencias em cache de camada separada
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Etapa 2: imagem de runtime enxuta (JRE 17) ----
# Base Debian (e NÃO Alpine): a lib nativa do org.xerial:sqlite-jdbc e compilada
# contra glibc; em musl (Alpine) o driver falha com UnsatisfiedLinkError no
# primeiro uso do SQLite, derrubando o boot no container.
FROM eclipse-temurin:17-jre
WORKDIR /app

# Diretorios que podem ser montados como volume (ex.: disco persistente do Render)
RUN mkdir -p /tmp /app/data

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]