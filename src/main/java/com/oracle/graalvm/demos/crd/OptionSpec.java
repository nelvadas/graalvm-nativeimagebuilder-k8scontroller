package com.oracle.graalvm.demos.crd;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

import java.util.ArrayList;
import java.util.List;

@JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class OptionSpec implements KubernetesResource {


    private String mvnBuildCommand="mvn clean package";
    private List<String> nativeImageBuildOptions = new ArrayList<String>();
    private boolean debug=true;


    public String getMvnBuildCommand() {
        return mvnBuildCommand;
    }

    public void setMvnBuildCommand(String mvnBuildCommand) {
        this.mvnBuildCommand = mvnBuildCommand;
    }

    public List<String> getNativeImageBuildOptions() {
        return nativeImageBuildOptions;
    }

    public void setNativeImageBuildOptions(List<String> nativeImageBuildOptions) {
        this.nativeImageBuildOptions = nativeImageBuildOptions;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public String toString() {
        return "OptionSpec{" +
                "mvnBuildCommand='" + mvnBuildCommand + '\'' +
                ", nativeImageBuildOptions=" + nativeImageBuildOptions +
                ", debug=" + debug +
                '}';
    }
}
