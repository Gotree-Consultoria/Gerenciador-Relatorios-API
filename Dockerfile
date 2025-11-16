# =========================================================================
# ESTÁGIO 1: BUILD (Compilação e Empacotamento)
# =========================================================================
FROM eclipse-temurin:21-jdk AS build

# Define o diretório de trabalho DENTRO do contêiner.
WORKDIR /app

# Copia os arquivos de build do subdiretório 'backend/API' para o WORKDIR.
COPY API/.mvn/ .mvn
COPY API/mvnw .
COPY API/pom.xml .

# Baixa todas as dependências do projeto.
RUN ./mvnw dependency:go-offline

# Copia o código-fonte do subdiretório 'backend/API' para o WORKDIR.
COPY API/src ./src

# Compila o projeto e gera o arquivo .jar.
RUN ./mvnw package -DskipTests


# =========================================================================
# ESTÁGIO 2: RUN (Execução)
# =========================================================================
FROM eclipse-temurin:21-jre

# Define o diretório de trabalho.
WORKDIR /app

# Copia APENAS o arquivo .jar gerado no estágio 'build'.
# O caminho de origem '/app/target/*.jar' está correto, pois o build
# foi executado dentro do WORKDIR do estágio anterior.
COPY --from=build /app/target/*.jar app.jar

# Expõe a porta que a aplicação roda dentro do contêiner.
EXPOSE 8081

# Comando final que inicia a sua API.
ENTRYPOINT ["java", "-jar", "app.jar"]