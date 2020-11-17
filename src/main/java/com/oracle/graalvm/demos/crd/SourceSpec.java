package com.oracle.graalvm.demos.crd;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;

@JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class SourceSpec  implements KubernetesResource {

    private String gitUri;
    private String gitRef="main";
    private String mainClass;
    private String contextDir=".";


    public String getGitUri() {
        return gitUri;
    }

    public void setGitUri(String gitUri) {
        this.gitUri = gitUri;
    }

    public String getGitRef() {
        return gitRef;
    }

    public void setGitRef(String gitRef) {
        this.gitRef = gitRef;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public String getContextDir() {
        return contextDir;
    }

    public void setContextDir(String contextDir) {
        this.contextDir = contextDir;
    }

    @Override
    public String toString() {
        return "SourceSpec{" +
                "gitUri='" + gitUri + '\'' +
                ", gitRef='" + gitRef + '\'' +
                ", mainClass='" + mainClass + '\'' +
                ", contextDir='" + contextDir + '\'' +
                '}';
    }
}
