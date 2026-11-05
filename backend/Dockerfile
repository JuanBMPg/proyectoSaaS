# Imagen del backend en dos etapas: se compila con el JDK y se ejecuta solo con el JRE,
# así la imagen final no lleva Maven, el código fuente ni el compilador.

# --- Construcción ---
FROM eclipse-temurin:21-jdk-alpine AS construccion
WORKDIR /app

# Primero solo lo que define las dependencias: Docker reutiliza esta capa mientras no
# cambie el pom.xml, y no las vuelve a descargar en cada build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
# Los tests se ejecutan en el CI; aquí solo se empaqueta
RUN ./mvnw -B -q package -DskipTests

# --- Ejecución ---
FROM eclipse-temurin:21-jre-alpine
# Sin root: si alguien comprometiera la aplicación, no tendría privilegios en el contenedor
RUN addgroup -S reservas && adduser -S reservas -G reservas
WORKDIR /app
COPY --from=construccion /app/target/reservas-*.jar app.jar
USER reservas

EXPOSE 8080
# MaxRAMPercentage: la JVM ajusta su memoria al límite del contenedor
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
