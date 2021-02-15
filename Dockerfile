FROM mozilla/sbt as build
COPY . /workspace
WORKDIR /workspace
RUN sbt assembly

FROM openjdk:8-alpine
RUN apk add --no-cache bash
COPY --from=build /workspace/target/scala-*/gss-jvm-build-tools-assembly-*.jar /gss-jvm-tools.jar
#ENTRYPOINT ["java", "-jar", "/gss-jvm-tools.jar"]