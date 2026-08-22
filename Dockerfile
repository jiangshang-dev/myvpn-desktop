FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

ARG JAR_FILE=target/myvpn-1.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

EXPOSE 9010

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
