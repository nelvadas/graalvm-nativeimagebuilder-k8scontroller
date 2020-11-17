package com.oracle.graalvm.demos.crd;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "apiVersion",
        "kind",
        "metadata",
        "spec",
        "status"
})
@JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class NativeImageBuildConfig extends CustomResource implements KubernetesResource {

    @JsonProperty("spec")
    private NativeImageBuildConfigSpec spec;

    @JsonProperty("status")
    private NativeImageBuildConfigStatus status;




    @Override
    public ObjectMeta getMetadata() {
        return super.getMetadata();
    }


    public NativeImageBuildConfigStatus getStatus() {
        return status;
    }

    public void setStatus(NativeImageBuildConfigStatus status) {
        this.status = status;
    }


    public NativeImageBuildConfigSpec getSpec() {
        return spec;
    }

    public void setSpec(NativeImageBuildConfigSpec spec) {
        this.spec = spec;
    }


    @Override
    public String toString() {
        return "NativeImageBuildConfig{" +
                "status=" + status +
                ", spec=" + spec +
                '}';
    }
}
