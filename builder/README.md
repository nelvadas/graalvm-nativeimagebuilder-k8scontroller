# Builder 
This folder contains the resources to build the builder docker image.
This image will be use by the controller to schedule Native Image build Pod/Containers.

```
cd builder
docker build -t nelvadas/native-image-builder:latest .
docker build -t nelvadas/native-image-builder:1.0.0-ol8 .
```
: