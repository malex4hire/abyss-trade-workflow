FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY pom.xml mvnw ./
COPY .mvn ./.mvn
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline
COPY src ./src
RUN ./mvnw -B -ntp package

FROM eclipse-temurin:25-jre-jammy
RUN apt-get update && apt-get upgrade -y --no-install-recommends && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /src/target/trade-demo.jar /app/trade-demo.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0"
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/trade-demo.jar"]
