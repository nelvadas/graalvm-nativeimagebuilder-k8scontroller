#!/bin/bash
#$1 GitUrl
#$2 GitRef
#$3 Git Context Dir
#$4 Git Credentials --- Not yet used

set -e

echo " received parameters $1 $2 "
export GIT_URL=$1
export GIT_REF=${2:-"main"}
export GIT_CONTEXT_DIR=${3:-"."}
export GIT_CREDS=$4
export MAIN_CLASS=$5
export DOCKER_CONFIG_JSON=$6
export DOCKER_REGISTRY=${7:-"docker.io"}
export DOCKER_BASE_IMAGE_APP=$8
export XX=$9


#Clone the user repository
echo " **************** Cloning the repository"

echo "Cloning the  source from Repository  [$GIT_URL] [$GIT_REF] [$GIT_CONTEXT_DIR] [$GIT_CREDS]"
git clone -v --branch ${GIT_REF}  ${GIT_URL} /opt/application/
cd /opt/application/${GIT_CONTEXT_DIR}
ls -rtl


# Retreive application parameters

# Echo Options
echo " **************** Container Environment Variables"

echo " Environment variables"
echo MAVEN_BUID_CMD=$MAVEN_BUID_CMD
echo NATIVE_IMAGE_BUID_OPTS=$NATIVE_IMAGE_BUID_OPTS
echo MAIN_CLASS=$MAIN_CLASS
echo DEBUG=$DEBUG


# Build the Java application
echo " **************** Building the JAR with Maven, you can use gradle with your custom Mvn command"

echo "Building JAR file ... "
if [ -z "$MAVEN_BUID_CMD" ]
then
      mvn clean package
else
      $MAVEN_BUID_CMD
fi


# Build the native binary .
echo " ****************Building the Native binary with GraalVM Native Image"
echo " native image command  $GRAALVM_HOME/bin/native-image -cp target/*.jar --verbose $NATIVE_IMAGE_BUID_OPTS $MAIN_CLASS app "

$GRAALVM_HOME/bin/native-image -cp target/*.jar --verbose $NATIVE_IMAGE_BUID_OPTS $MAIN_CLASS app
ls -rtl
# Build the result application Docker image
echo IMAGE_REPO=$IMAGE_REPO
echo IMAGE_TAG=${IMAGE_TAG:-"latest"}

# Build the result docker image
echo " ****************Building the result OCI Image with the Native binary"
mkdir docker-native-tmp
mv app  docker-native-tmp
mv /Dockerfile.builder.native  docker-native-tmp
cd docker-native-tmp
podman --version
podman build  --cgroup-manager=cgroupfs -t ${DOCKER_REGISTRY}/${IMAGE_REPO}:${IMAGE_TAG} -f Dockerfile.builder.native --build-arg BASE_IMAGE_APP=${DOCKER_BASE_IMAGE_APP} .


## Podman login to registry
mkdir -p /run/user/0/containers/
export REGISTRY_AUTH_FILE=/run/user/0/containers/auth.json
echo $DOCKER_CONFIG_JSON | base64 -d > $REGISTRY_AUTH_FILE

podman login --log-level error --authfile=$REGISTRY_AUTH_FILE  $DOCKER_REGISTRY
## Push the image
podman push  ${DOCKER_REGISTRY}/${IMAGE_REPO}:${IMAGE_TAG}
# Return
echo "Image  ${DOCKER_REGISTRY}/${IMAGE_REPO}:${IMAGE_TAG} - Success  "
echo " Use docker run ${DOCKER_REGISTRY}/${IMAGE_REPO}:${IMAGE_TAG} to start a container with this application "

exit 0