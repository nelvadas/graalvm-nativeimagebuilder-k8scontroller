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

#Clone the user repository
echo "Cloning the  source from Repository  [$GIT_URL] [$GIT_REF] [$GIT_CONTEXT_DIR] [$GIT_CREDS]"
git clone -v --branch ${GIT_REF}  ${GIT_URL} /opt/application/
cd /opt/application/${GIT_CONTEXT_DIR}
ls -rtl


# Retreive application parameters
echo " Retreiving application details with Maven helper plugin.."
#export APP_NAME=$(mvn org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.artifactId -q -DforceStdout)
#export APP_VERSION=$(mvn org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.version -q -DforceStdout)

# Docker
 buildah --version

# Echo Options
echo " Environment variables"
echo MAVEN_BUID_CMD=$MAVEN_BUID_CMD
echo NATIVE_IMAGE_BUID_OPTS=$NATIVE_IMAGE_BUID_OPTS
echo MAIN_CLASS=$MAIN_CLASS
echo DEBUG=$DEBUG

# Build the Java application
echo "Building JAR file ... "
if [ -z "$MAVEN_BUID_CMD" ]
then
      mvn clean package
else
      $MAVEN_BUID_CMD
fi


# Build the native binary .

$GRAALVM_HOME/bin/native-image -cp target/*.jar --verbose $NATIVE_IMAGE_BUID_OPTS $MAIN_CLASS app

# Build the result application Docker image

# Return
exit 0