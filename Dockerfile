FROM adoptopenjdk/openjdk11 as build

RUN mkdir /tmp/build

WORKDIR /tmp/build

COPY . /tmp/build

RUN ./mvnw package

FROM adoptopenjdk/openjdk11

RUN mkdir -p /deployments/lib/

COPY --from=build /tmp/build/adoptopenjdk-api-v3-frontend/target/lib/* /deployments/lib/

COPY --from=build /tmp/build/adoptopenjdk-api-v3-updater/target/adoptopenjdk-api-v3-updater-*-jar-with-dependencies.jar /deployments/adoptopenjdk-api-v3-updater-runner.jar
COPY --from=build /tmp/build/adoptopenjdk-api-v3-frontend/target/adoptopenjdk-api-*-runner.jar /deployments/adoptopenjdk-api-v3-frontend.jar

CMD ["java", "-jar", "/deployments/adoptopenjdk-api-v3-frontend.jar"]

