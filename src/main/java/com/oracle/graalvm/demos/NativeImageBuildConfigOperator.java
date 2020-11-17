
package com.oracle.graalvm.demos;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.oracle.graalvm.demos.controller.NativeImageBuildConfigController;
import com.oracle.graalvm.demos.crd.DoneableNativeImageBuildConfig;
import com.oracle.graalvm.demos.crd.NativeImageBuildConfig;
import com.oracle.graalvm.demos.crd.NativeImageBuildConfigList;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;

/**
 * The application main class.
 */
public class NativeImageBuildConfigOperator {

    public static Logger logger = Logger.getLogger(NativeImageBuildConfigOperator.class.getName());
    private  static KubernetesClient client;
    private NativeImageBuildConfigOperator() {
    }

    /**
     * Application main entry point.
     * @param args command line arguments.
     * @throws IOException if there are problems reading logging properties
     */
    public static void main(final String[] args) throws IOException {

        try  {
             client = new DefaultKubernetesClient() ;
            String namespace = client.getNamespace();
            if (namespace == null) {
                logger.warning("No namespace found via config, using default.");
                namespace = "default";
                logger.info("Using namespace : " + namespace);
            } else {
                logger.info("Using namespace : " + client.getNamespace());
            }

            SharedInformerFactory informerFactory = client.informers();

            //create the CRD Context
            CustomResourceDefinitionContext nibcCrdContext = new CustomResourceDefinitionContext.Builder()
                    .withVersion("v1alpha1")
                    .withScope("Namespaced")
                    .withGroup("demos.graalvm.oracle.com")
                    .withPlural("nativeimagebuildconfigs")
                    .build();



            // Create the PodInformer who will be listening to Builderpods statuses
            SharedIndexInformer<Pod> podSharedIndexInformer = informerFactory.sharedIndexInformerFor(Pod.class, PodList.class,10*60*1000);

            // nibcInformer will listen to NativeImageBuildConfig objects
            SharedIndexInformer<NativeImageBuildConfig> nibcSharedIndexInformer = informerFactory.sharedIndexInformerForCustomResource(nibcCrdContext, NativeImageBuildConfig.class, NativeImageBuildConfigList.class, 10*60*1000);

            MixedOperation<NativeImageBuildConfig, NativeImageBuildConfigList,DoneableNativeImageBuildConfig, Resource<NativeImageBuildConfig,DoneableNativeImageBuildConfig>> nibClient = client.customResources(
                    nibcCrdContext,
                    NativeImageBuildConfig.class,
                    NativeImageBuildConfigList.class,
                    DoneableNativeImageBuildConfig.class);

            NativeImageBuildConfigController nibcController = new NativeImageBuildConfigController(client,nibClient,nibcCrdContext, nibcSharedIndexInformer, podSharedIndexInformer);
            //Start all registered informers
            informerFactory.startAllRegisteredInformers();

            nibcController.loop();
        }
        catch( RuntimeException re){

            logger.log(Level.SEVERE,"Error in the controller ", re );
        }
    }



}
