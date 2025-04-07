FROM ubuntu
# Install the xz-utils package
RUN \
  apt-get update && apt-get install -y git curl openjdk-21-jdk-headless
RUN mkdir -p /root/.m2
COPY resources/settings.xml /root/.m2
RUN \
  mkdir -p /opt/maven && \
  curl -sSLf https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz | tar -C /opt/maven --strip-components=1 -xzvf -
ENV PATH=/opt/maven/bin:${PATH}

