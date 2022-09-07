FROM mozilla/sbt:8u232_1.4.9 as build
COPY . /workspace
WORKDIR /workspace
RUN sbt universal:packageZipTarball

FROM openjdk:17-alpine
RUN apk add --no-cache bash pigz

# Install Apache JENA tools for this.
ADD https://downloads.apache.org/jena/binaries/apache-jena-4.6.0.tar.gz /apache-jena.tar.gz
RUN tar xvfz /apache-jena.tar.gz && \
    cd /apache-jena-* && \
    cp -r * /usr/local/ && \
    cd .. && \
    rm -rf /apache-jena-* && \
    rm /apache-jena.tar.gz

COPY --from=build /workspace/target/universal/gss-jvm-build-tools-*.tgz /gss-jvm-tools.tgz
RUN tar xvfz gss-jvm-tools.tgz
RUN mv /gss-jvm-build-tools-* /gss-jvm-build-tools
RUN rm gss-jvm-tools.tgz
ENV PATH="/gss-jvm-build-tools/bin:$PATH"
