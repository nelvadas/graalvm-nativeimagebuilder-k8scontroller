package com.oracle.graalvm.demos.crd;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class DoneableNativeImageBuildConfig extends CustomResourceDoneable<NativeImageBuildConfig> {
    public DoneableNativeImageBuildConfig(NativeImageBuildConfig resource, Function<NativeImageBuildConfig, NativeImageBuildConfig> function) {
        super(resource, function);
    }
}
