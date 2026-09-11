# ---- Etapa 1: build do jar com Maven + JDK 17 ----
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Baixa dependencias em cache de camada separada
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Etapa 2: imagem de runtime enxuta (JRE 17, Alpine) ----
# Alpine: imagem muito menor que a base Debian/Ubuntu (~50% do tamanho) ->
# menos memoria/CPU para iniciar o container no plano free (512 MB), onde
# ocorria "Exited with status 128" antes de qualquer output do app.
# Nota: em 'prod' o banco e PostgreSQL (Neon via SPRING_DATASOURCE_URL). A lib
# nativa do org.xerial:sqlite-jdbc nao e usada no container (perfil default
# fica restrito a dev/testes fora de Docker), entao musl/Alpine e seguro.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Diretorios utilitarios com permissao de escrita (evita falha de boot)
RUN mkdir -p /app/data /tmp && chmod -R 777 /app/data /tmp

# Marcador para invalidar cache do Render: v4.0
RUN echo "Rebuild Render v4.0"

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Execucao direta do Java (exec form): sem shell 'sh' intermediario, sinais
# SIGTERM/SIGKILL chegam direto no JVM e a porta NÃO e passada via -Dserver.port
# (em forma exec ${PORT} nao expandiria). O server.port=${PORT:8080} em
# application.properties resolve a porta do ambiente do Render.
# Orcamento de memoria validado localmente (RSS ~297MB em boot):
#   -Xmx256m + UseSerialGC (serial usa 1 thread de GC, nao estoura CPU 0.1)
#   + metaspace/codecache limitados. G1GC com -Xmx350m estouraria os 512MB.
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-XX:MaxMetaspaceSize=192m", "-XX:ReservedCodeCacheSize=96m", "-XX:+UseSerialGC", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]