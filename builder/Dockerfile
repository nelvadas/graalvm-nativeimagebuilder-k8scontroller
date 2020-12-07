FROM oracle/graalvm-ce:20.2.0-java11-ol8

LABEL maintainer=Elvadas<elvadas.nono.woguia@oracle.com>

# Install native image feature
RUN gu install native-image

# Install utils
#RUN yum -y update && \
#    yum -y install zip unzip curl git   && \
#    yum clean all

RUN microdnf update
RUN microdnf  install gzip unzip curl git


# Install Maven
WORKDIR /opt
RUN curl -L  https://www-eu.apache.org/dist/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz -o apache-maven-3.6.3-bin.tar.gz

RUN    tar xzf apache-maven-3.6.3-bin.tar.gz &&\
       ln -s apache-maven-3.6.3 maven

# Set environment variable

ENV PATH=/opt/maven/bin:${PATH}
ENV M2_HOME=/opt/maven

#DockerBuildWithBuildah
RUN  microdnf install -y podman

# Source to Native image Process
COPY native-image-build.sh /native-image-build.sh
COPY Dockerfile.builder.native /Dockerfile.builder.native
RUN chmod +x /native-image-build.sh

# Prepare checkout folder
RUN mkdir /opt/application


ENTRYPOINT ["/native-image-build.sh"]
CMD ["https://github.com/nelvadas/graalvm-helloworld-nativeimage.git","main","","/"]