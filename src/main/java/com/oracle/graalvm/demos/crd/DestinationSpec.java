package com.oracle.graalvm.demos.crd;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

@JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class DestinationSpec implements KubernetesResource {

    private String imageRegistry="docker.io";
    private String imageRepository;
    private String imageTag;
    private String baseImage;
    private String pushPullSecret;
    private String deployment;

    public String getImageRepository() {
        return imageRepository;
    }

    public void setImageRepository(String imageRepository) {
        this.imageRepository = imageRepository;
    }

    public String getImageTag() {
        return imageTag;
    }

    public void setImageTag(String imageTag) {
        this.imageTag = imageTag;
    }

    public String getBaseImage() {
        return baseImage;
    }

    public void setBaseImage(String baseImage) {
        this.baseImage = baseImage;
    }

    public String getPushPullSecret() {
        return pushPullSecret;
    }

    public void setPushPullSecret(String pushPullSecret) {
        this.pushPullSecret = pushPullSecret;
    }

    public String getDeployment() {
        return deployment;
    }

    public void setDeployment(String deployment) {
        this.deployment = deployment;
    }





    @Override
    public String toString() {
        return "DestinationSpec{" +
                "imageRegistry='" + imageRegistry + '\'' +
                "imageRepository='" + imageRepository + '\'' +
                ", imageTag='" + imageTag + '\'' +
                ", baseImage='" + baseImage + '\'' +
                ", pushSecret='" + pushPullSecret + '\'' +
                ", deployment='" + deployment + '\'' +
                '}';
    }

    public String getImageRegistry() {
        return imageRegistry;
    }

    public void setImageRegistry(String imageRegistry) {
        this.imageRegistry = imageRegistry;
    }
}
