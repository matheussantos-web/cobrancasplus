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

COPY --from=build /app/target/*.jar app.jar

# Variavel de porta padrao (o Render injeta PORT dinamicamente).
ENV PORT=8080

EXPOSE 8080

# Orcamento de memoria para instancias pequenas (Render free: 512 MB RAM):
# limita heap (~40%), metaspace e code cache para o JVM nao estourar a cgroup
# na inicializacao (RSS medido ~297MB em boot local com estas flags).
# UseSerialGC mantem o RSS baixo removendo threads paralelas de GC.
# NOTA: usa 40% (≈204MB de heap) em vez de MaxRAMPercentage=75, que num
# container de 512MB deixaria o heap crescer a ~384MB + overhead nativo e
# reintroduziria o OOM que causava "Exited with status 128" no Render.
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=40 -XX:MaxMetaspaceSize=192m -XX:ReservedCodeCacheSize=96m -XX:+UseSerialGC"

# ENTRYPOINT com diagnostico: imprime marcadores antes de subir o app para
# evidenciar no log do Render onde ocorre a falha (build vs JVM vs jar).
# 'set -e' garante que qualquer erro do shell derrube o container (nao mascara).
# Nao usa forma exec (JSON) para -Dserver.port=${PORT}: lá o ${PORT} nao
# expandiria; o shell resolve a variavel do Render.
ENTRYPOINT ["sh", "-c", \
  "set -e; " \
  "echo '[diag] orcamento:'; echo \"$JAVA_TOOL_OPTIONS\"; " \
  "echo '[diag] app:'; ls -la /app; " \
  "echo '[diag] java:'; java -version 2>&1; " \
  "echo '[diag] iniciando app.jar (port ${PORT:-8080})...'; " \
  "exec java -Dserver.port=${PORT} " \
    "-Dlogging.level.root=INFO -Dlogging.level.com.emprestimos=DEBUG " \
    "-Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]