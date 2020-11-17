package com.oracle.graalvm.demos.crd;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.CustomResourceList;

@JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class NativeImageBuildConfigList extends CustomResourceList<NativeImageBuildConfig> {
}
