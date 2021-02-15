FROM mozilla/sbt as build
COPY . /workspace
WORKDIR /workspace
RUN sbt universal:packageZipTarball

FROM openjdk:8-alpine
RUN apk add --no-cache bash
COPY --from=build /workspace/target/universal/gss-jvm-build-tools-*.tgz /gss-jvm-tools.tgz
RUN tar xvfz gss-jvm-tools.tgz
RUN mv /gss-jvm-build-tools-0.1 /gss-jvm-build-tools
RUN rm gss-jvm-tools.tgz