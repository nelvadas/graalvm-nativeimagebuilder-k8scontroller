package com.oracle.graalvm.demos.crd;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.KubernetesResource;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "source",
        "destination",
        "options"

})
@JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class NativeImageBuildConfigSpec implements KubernetesResource {


    private SourceSpec source;
    private DestinationSpec destination;
    private OptionSpec options;



    @Override
    public String toString() {
        return "NativeImageBuildConfigSpec{" +

                "source=" + source +
                ",destination=" + destination +
                ",options=" + options +
                '}';
    }

    public SourceSpec getSource() {
        return source;
    }

    public void setSource(SourceSpec source) {
        this.source = source;
    }

    public void setDestination(DestinationSpec destination) {
        this.destination = destination;
    }

    public OptionSpec getOptions() {
        return options;
    }

    public void setOptions(OptionSpec options) {
        this.options = options;
    }
}
