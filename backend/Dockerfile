# ============================================================
# Build : Maven + JDK, cache Maven persistant via BuildKit
# (les rebuilds ne retéléchargent pas les dépendances)
# ============================================================
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# 1) pom.xml d'abord pour maximiser le cache des couches
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B dependency:go-offline || true

# 2) Code source et build du jar (tests : exécutés en CI, pas au build image)
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -q -B package -DskipTests

# ============================================================
# Runtime : JRE seul, on ne copie que le jar
# ============================================================
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
