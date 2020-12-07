# GraalVM NativeImage Builder Kubernetes Controller

Kubernetes Controller that automatically build GraalVM NativeImage apps from github public sources
Accelerating Kubernetes controller with GraalVM Native images

## Build the controller

With JDK11+
```bash
mvn package
java -jar target/graalvm-nativeimagebuilder-k8scontroller.jar
```

## Build Native Image pod's image

```
cd builder 
doccker build -t nelvadas/native-image-builder:1.0.0-ol8 .

```

## Setup the CRD

```
kubectl apply -f src/main/resources/NativeImageBuildConfig-crd-v1.yaml
customresourcedefinition.apiextensions.k8s.io/nativeimagebuildconfigs.demos.graalvm.oracle.com created
```




## Create Registry Credential 
```
kubectl create secret docker-registry registrycreds \
--docker-server=docker.io \
--docker-username=YOUR_USERNAME \
--docker-password=YOUR_PASSWORD_OR_ACCESSTOKEN

secret/registrycreds created
```

## Create a Native Image build 

```python
apiVersion: demos.graalvm.oracle.com/v1alpha1
kind: NativeImageBuildConfig
metadata:
  name: hello-nib
  labels:
    app: graal-demos
spec:
  source:
    gitUri: "https://github.com/nelvadas/graalvm-helloworld-nativeimage.git"
    gitRef: "main"
    contextDir: "."
    mainClass: com.oracle.graalvm.demos.hellonative.App
  destination:
    imageRepository: nelvadas/graalvm-helloworld-nativeimage
    imageTag: '1.0.0'
    baseImage: alpine
    deployment: hello
    pushPullSecret: registrycreds
  options:
    debug: true
    mvnBuildCommand: 'mvn clean package'
    nativeImageBuildOptions:
      - '-H:+ReportExceptionStackTraces'
      - '-H:+ReportUnsupportedElementsAtRuntime'
      - '-H:+AddAllCharsets'

```

```
kubectl apply -f HelloNativeBuildConfig.yaml
```




# Resources

Json validation https://tools.ietf.org/html/draft-wright-json-schema-validation-00#section-5.15

CRD Definition https://kubernetes.io/docs/tasks/extend-kubernetes/custom-resources/custom-resource-definitions

Fabric8  Kubernetes Java client https://github.com/fabric8io/kubernetes-client/blob/master/doc/CHEATSHEET.md

GraalVM Container Images https://github.com/graalvm/container/packages/237037?version=ol8-java11-20.3.0

Podman For Docker Users https://developers.redhat.com/blog/2019/02/21/podman-and-buildah-for-docker-users/
