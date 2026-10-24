FROM eclipse-temurin:21-jre
WORKDIR /app
COPY build/install/backend/ /app/
COPY docker/prepare-startup-archive.sh /tmp/prepare-startup-archive.sh
# The JVM entrypoint does not use the service manager inherited from the base image.
RUN rm -f /usr/bin/pebble \
    && bash /tmp/prepare-startup-archive.sh /app \
    && rm /tmp/prepare-startup-archive.sh \
    && chmod -R a=rX /app
ENV JAVA_OPTS="-Xshare:auto -XX:SharedArchiveFile=/app/startup.jsa"
ENV PORT=8080
EXPOSE 8080
USER 10001:10001
ENTRYPOINT ["/app/bin/backend"]
