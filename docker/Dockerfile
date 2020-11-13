FROM openjdk:8-jre-alpine

RUN apk add --update \
    curl \
    && rm -rf /var/cache/apk/*

ARG JAR_NAME

ADD ${JAR_NAME} ${JAR_NAME}
ADD ./start-app.sh start-app.sh

EXPOSE 8080

ENV JAR_NAME "${JAR_NAME}"
HEALTHCHECK --interval=10s --timeout=3s --retries=10 CMD curl -f http://localhost:8080/health || exit 1

CMD ["sh", "start-app.sh"]
