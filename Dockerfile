# Cloud 노드 Railway 배포용
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY build/libs/vending-all.jar /app/vending-all.jar

ENV SERVER_ROLE=CLOUD
ENV JAVA_OPTS=""

EXPOSE 9093

CMD sh -c "java $JAVA_OPTS -cp /app/vending-all.jar vending.server.ServerMain"
