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

# Diretorios utilitarios (o banco em producao e PostgreSQL via perfil 'prod').
RUN mkdir -p /tmp /app/data

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Orcamento de memoria para instancias pequenas (Render free: 512 MB RAM):
# limita heap, metaspace e code cache para o JVM nao estourar a cgroup na
# inicializacao (escape de OOM -> "Exited with status 128"). UseSerialGC
# mantem o RSS baixo removendo threads paralelas de GC.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=40 -XX:MaxMetaspaceSize=192m -XX:ReservedCodeCacheSize=96m -XX:+UseSerialGC"

# ENTRYPOINT com diagnostico: imprime marcadores antes de subir o app para
# evidenciar no log do Render onde ocorre a falha (build vs JVM vs jar).
ENTRYPOINT ["sh", "-c", \
  "echo '[diag] orcamento JVM:'; echo \"$JAVA_TOOL_OPTIONS\"; " \
  "echo '[diag] conteudo de /app:'; ls -la /app; " \
  "echo '[diag] java:'; java -version 2>&1; " \
  "echo '[diag] iniciando app.jar...'; " \
  "exec java -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]