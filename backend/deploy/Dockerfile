FROM eclipse-temurin:21-jre-jammy AS skywalking-agent
ARG SKYWALKING_AGENT_VERSION=9.6.0
ARG SKYWALKING_AGENT_SHA512=64346286924aafcbd5e44358e4fd720a52900192bc5a32846283feee728aad90454ac8574683599b7fe4d59d587a4c1ae58744ed01028d5010847b1277906afa
COPY .runtime/skywalking/apache-skywalking-java-agent-9.6.0.tgz /tmp/skywalking-agent.tgz
RUN echo "${SKYWALKING_AGENT_SHA512}  /tmp/skywalking-agent.tgz" | sha512sum --check --strict \
    && mkdir -p /opt/skywalking \
    && tar -xzf /tmp/skywalking-agent.tgz -C /opt/skywalking \
    && mv /opt/skywalking/skywalking-agent /opt/skywalking/agent \
    && rm /tmp/skywalking-agent.tgz

FROM eclipse-temurin:21-jre-jammy
ARG MODULE
WORKDIR /app
COPY ${MODULE}/target/${MODULE}-*.jar /app/service.jar
COPY --from=skywalking-agent /opt/skywalking/agent /opt/skywalking/agent
ENV SW_AGENT_NAME=refine-service \
    SW_AGENT_COLLECTOR_BACKEND_SERVICES=skywalking-oap:11800
EXPOSE 8080
ENTRYPOINT ["java","-javaagent:/opt/skywalking/agent/skywalking-agent.jar","-XX:MaxRAMPercentage=75.0","-jar","/app/service.jar"]
