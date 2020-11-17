package com.oracle.graalvm.demos.crd;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class NativeImageBuildConfigStatus {

    private Integer buildCounter;
    private String status;
    private String builderPod;

    public NativeImageBuildConfigStatus(){
        buildCounter = Integer.valueOf(0);
    }

    public Integer getBuildCounter() {
        return buildCounter;
    }

    public void setBuildCounter(Integer buildCounter) {
        this.buildCounter = buildCounter;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBuilderPod() {
        return builderPod;
    }

    public void setBuilderPod(String builderPod) {
        this.builderPod = builderPod;
    }
}
