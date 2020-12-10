# GraalVM NativeImage Builder Kubernetes Controller

Kubernetes Controller that automatically build GraalVM NativeImage apps from github public sources.

## Native Image BuildConfig
The purpose of the controller is to be able to schedule native image builds for input resources. 
The controller reads any NativeImageBuildConfig CRD object like the following one and schedule a kubernetes pod to build the Native binary and wrap it in an OCI "Docker" Image.

```yaml
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
    imageRegistry: docker.io
    imageRepository: nelvadas/graalvm-helloworld-nativeimage
    imageTag: '1.0.2'
    baseImage: ubuntu:18.04
    deployment: hello
    pushPullSecret: registrycreds
  options:
    debug: true
    mvnBuildCommand: ''
    nativeImageBuildOptions:
      - '-H:+ReportExceptionStackTraces'
      - '-H:+ReportUnsupportedElementsAtRuntime'
      - '-H:+AddAllCharsets'
      - '--no-server '
      - '--no-fallback'
```

The scheduled builder pods handle the following activities
- Check out the public `gitUri` source
- Build the  JAR package using Maven or a provided custom script
- Generate Native binary from JAR using GraalVM Native Image technology 
- Wrap the generated native image in an OCI "docker" image 
- Push the result it to a repository of your choice

## Setup the NativeImageBuidConfig CRD
The controller is built with [fabric8 Java kubernetes Client](https://github.com/fabric8io/kubernetes-client) .
Its provides a control loop for the following CRD Definition<br>
`NativeImageBuildConfig` object is a namespaced  custom type that act like `Deployment` ;
its defines  what github source we  want to build.

```yaml
        apiVersion: apiextensions.k8s.io/v1
        kind: CustomResourceDefinition
        metadata:
          name: nativeimagebuildconfigs.demos.graalvm.oracle.com
        spec:
          group: demos.graalvm.oracle.com
          scope: Namespaced
          names:
            categories:
              - all
            plural: nativeimagebuildconfigs
            singular: nativeimagebuildconfig
            kind: NativeImageBuildConfig
            shortNames:
              - ni
              - nib
              - nibc
          versions:
            - name: v1alpha1
              served: true
              storage: true
              additionalPrinterColumns:
                - jsonPath: .metadata.creationTimestamp
                  name: age
                  type: date
                - jsonPath: .status.buildCounter
                  name: counter
                  type: integer
                - jsonPath: .status.builderPod
                  name: builderPod
                  type: string
                - jsonPath: .status.status
                  name: status
                  type: string
                - jsonPath: .spec.source.gitUri
                  name: source
                  type: string
                - jsonPath: .spec.destination.imageRepository
                  name: target
                  type: string
                - jsonPath: .spec.destination.imageTag
                  name: tag
                  type: string
              schema:
                openAPIV3Schema:
                  type: object
                  properties:
                    status:
                      type: object
                      properties:
                        status:
                          type: string
                        buildCounter:
                          type: integer
                        buiderPod:
                          type: String
                    spec:
                      type: object
                      properties:
                        source:
                          type: object
                          required:
                            - gitUri
                            - mainClass
                          properties:
                            gitUri:
                              type: string
                            gitRef:
                              type: string
                            mainClass:
                              type: string
                            contextDir:
                              type: string
                        destination:
                          type: object
                          required:
                           - imageRepository
                          properties:
                            imageRegistry:
                              type: string
                            imageRepository:
                              type: string
                            imageTag:
                              type: string
                            baseImage:
                              type: string
                            pushPullSecret:
                              type: string
                            deployment:
                              type: string
                        options:
                          type: object
                          properties:
                            debug:
                              type: boolean
                            mvnBuildCommand:
                              type: string
                            nativeImageOptions:
                              type: array
                              items:
                                type: string

```
This CRD higligth tree main components in the `spec`
- The source:
- the destination: specify where we want the controller to push the final application Image
- options: contains various options to customize the native image generation process.  [refers](https://www.graalvm.org/reference-manual/native-image/) to ``GRAALVM_HOME/bin/nativeimage --help`` for more infos.

Add the CRD Definition to your kubernetes cluster  :

```
cd k8s
kubectl apply -f NativeImageBuildConfig-crd-v1.yaml
customresourcedefinition.apiextensions.k8s.io/nativeimagebuildconfigs.demos.graalvm.oracle.com created
```


## Build the controller
Controller can be run inside or outside kubernetes cluster.
With JDK11+
```bash
mvn package
```
Run the controller outside your cluster with
```
java -jar target/graalvm-nativeimagebuilder-k8scontroller.jar
```

For in-cluster deployment user 
```
kubectl apply -f k8s/controller-01.yaml
deployment.apps/graalvm-nativeimagebuilder-k8scontroller created
```
Check the running controller 
```bash
 $kubectl get pods
 NAME                                                    READY   STATUS    RESTARTS   AGE
 graalvm-nativeimagebuilder-controller-9db56c8fd-8dw4j   1/1     Running   0          14s

````

The controller starts and is looking for NativeImageBuildConfig to be processed in its queue.
```
$kubectl logs graalvm-nativeimagebuilder-controller-9db56c8fd-8dw4j
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
Dec 10, 2020 1:11:10 PM com.oracle.graalvm.demos.NativeImageBuildConfigOperator main
INFO: Using namespace : default
Dec 10, 2020 1:11:11 PM com.oracle.graalvm.demos.controller.NativeImageBuildConfigController loadGlobalConfiguration
INFO: Using ConfigMap ....nibc-cm
Dec 10, 2020 1:11:11 PM com.oracle.graalvm.demos.controller.NativeImageBuildConfigController init
INFO: Adding the NIBC Ressource Handler
Dec 10, 2020 1:11:11 PM com.oracle.graalvm.demos.controller.NativeImageBuildConfigController init
INFO: Adding the Pod Ressource Handler
Dec 10, 2020 1:11:11 PM com.oracle.graalvm.demos.controller.NativeImageBuildConfigController loop
INFO: Native Controller started ...
Dec 10, 2020 1:11:11 PM com.oracle.graalvm.demos.controller.NativeImageBuildConfigController loop
INFO: Fetching NativeImageBuildConfig from work queue ...
```


By default, the controller will schedule build pods with the base image ` nelvadas/native-image-builder:latest`
This image can be customized in various ways: 

### Controller  ConfigMap 
The controller will first check for the first configMap in the deployment namespace with `com.oracle.graalvm.nativeimagebuildconfig` label set to `true 
```
apiVersion: v1
kind: ConfigMap
metadata:
  creationTimestamp: "2020-11-10T12:28:25Z"
  labels:
    com.oracle.graalvm.nativeimagebuildconfig: "true"
  name: nibc-cm
data:
  application.properties: |
    app.builder.image=nelvadas/native-image-builder:1.0.0-ol8
```
the application.properties file in this configMap contains a specific key to customize the builder image.
### Helidon config file 
from file `src/main/resources/application.yaml`, change the `app.builder.image` property and rebuild the controller artifacts.



###  Customize the builder image
This image will be used by the controller to schedule nativeimage build pods.
build the image from the builder directory with the following instructions.
```
cd builder 
doccker build -t nelvadas/native-image-builder:latest .
```

# Demos
Pray the demo God :)

## Hello World Native Image 
In this first demo we want to use the controller to automatically build 
- source : https://github.com/nelvadas/graalvm-helloworld-nativeimage.git
- target : [docker.io/nelvadas/graalvm-helloworld-nativeimage:1.0.0-demo](https://hub.docker.com/r/nelvadas/graalvm-helloworld-nativeimage)

### STEP1: Create Registry Credential 
In order to push the result image in docker hub we need Kubernetes secret.
In our Native image build, we will pass this secret reference.

```
kubectl create secret docker-registry registrycreds \
--docker-server=docker.io \
--docker-username=YOUR_USERNAME \
--docker-password=YOUR_PASSWORD_OR_ACCESSTOKEN

secret/registrycreds created
```
Customize `YOUR_USERNAME` and `YOUR_PASSWORD_OR_ACCESSTOKEN` with your credentials.
The generated secret file we will be rely on has the following format. 
```
{"auths":{"docker.io":{"username":"YOUR_USERNAME","password":"YOUR_PASSWORD_OR_ACCESSTOKEN","auth":"YOUR_GENERATED_BASE64_AUTH"}}}
```


### STEP2 Create a Hello GraalVM NativeImage buildconfig  CRD object

```yaml
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
    imageTag: '1.0.0-demo'
    baseImage: ubuntu:18.04
    pushPullSecret: registrycreds
  options:
    mvnBuildCommand: 'mvn clean package'
    nativeImageBuildOptions:
      - '-H:+ReportExceptionStackTraces'
      - '-H:+ReportUnsupportedElementsAtRuntime'
      - '-H:+AddAllCharsets'

```

```
kubectl apply -f k8s/HelloNativeBuildConfig.yaml
```

The CRD is created
```
 $kubectl get ni
NAME        AGE   COUNTER   BUILDERPOD               STATUS    SOURCE                                                           TARGET                                    TAG
hello-nib   18s   1         hello-nib-builderpod-0   Running   https://github.com/nelvadas/graalvm-helloworld-nativeimage.git   nelvadas/graalvm-helloworld-nativeimage   1.0.0-demo3
``` 

The associated builder pod checks out the source and build the final docker image 
```
 $kubeclt logs -f hello-nib-builderpod-0
...
...
Writing manifest to image destination
Storing signatures
Image  docker.io/nelvadas/graalvm-helloworld-nativeimage:1.0.0-demo3 - Success
 Use docker run docker.io/nelvadas/graalvm-helloworld-nativeimage:1.0.0-demo3 to start a container with this application
(base) $docker pull docker.io/nelvadas/graalvm-helloworld-nativeimage:1.0.0-demo3 
```

The resulted image can be run as a standalone application 
```
docker run docker.io/nelvadas/graalvm-helloworld-nativeimage:1.0.0-demo3
Hello World!
``` 

### Demo Video
[![HelloWorld Demo in Video ](http://img.youtube.com/vi/wnA3CKuhzaE/0.jpg)](http://www.youtube.com/watch?v=wnA3CKuhzaE)



## Building the Native binary for the Controller itself with the running controller 



# Resources

Json validation https://tools.ietf.org/html/draft-wright-json-schema-validation-00#section-5.15

CRD Definition https://kubernetes.io/docs/tasks/extend-kubernetes/custom-resources/custom-resource-definitions

Fabric8  Kubernetes Java client https://github.com/fabric8io/kubernetes-client/blob/master/doc/CHEATSHEET.md

GraalVM Container Images https://github.com/graalvm/container/packages/237037?version=ol8-java11-20.3.0

Podman For Docker Users https://developers.redhat.com/blog/2019/02/21/podman-and-buildah-for-docker-users/
