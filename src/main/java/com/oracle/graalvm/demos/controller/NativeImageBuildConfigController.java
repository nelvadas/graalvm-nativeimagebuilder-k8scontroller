package com.oracle.graalvm.demos.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.graalvm.demos.crd.*;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.internal.SerializationUtils;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.config.spi.ConfigSource;
import io.netty.handler.logging.LogLevel;
import io.netty.util.internal.StringUtil;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;


public class NativeImageBuildConfigController  {
    public static final String APP_BUILDER_IMAGE = "app.builder.image";
    public static final String COM_ORACLE_GRAALVM_NATIVEIMAGEBUILDCONFIG = "com.oracle.graalvm.nativeimagebuildconfig";
    public static final String ENV_MAVEN_BUID_CMD = "MAVEN_BUID_CMD";
    public static final String LABEL_BUILD_CONFIG = "buildConfig";
    public static Logger logger = Logger.getLogger(NativeImageBuildConfigController.class.getName());

    // CRD informer
    private final SharedIndexInformer<NativeImageBuildConfig> nibcInformer;
    // Builder pods informer
    private final SharedIndexInformer<Pod> podInformer;

    private final BlockingQueue<NativeImageBuildConfig> workQueue;

    private final KubernetesClient client;

    private final CustomResourceDefinitionContext nibcCrdContext;
    //CRD Client
    private final MixedOperation<NativeImageBuildConfig, NativeImageBuildConfigList, DoneableNativeImageBuildConfig, Resource<NativeImageBuildConfig, DoneableNativeImageBuildConfig>> nibCrdClient;


    private ConfigMap globalBuilderConfigMap;


    private String builderImage;


    public NativeImageBuildConfigController(KubernetesClient kubernetesClient,
                                            MixedOperation<NativeImageBuildConfig, NativeImageBuildConfigList, DoneableNativeImageBuildConfig, Resource<NativeImageBuildConfig, DoneableNativeImageBuildConfig>> nibCrdClient,
                                            CustomResourceDefinitionContext nibcCrdContext,
                                            SharedIndexInformer<NativeImageBuildConfig> nibInformer,
                                            SharedIndexInformer<Pod> podInformer){
        this.client = kubernetesClient;
        this.nibCrdClient = nibCrdClient;
        this.nibcCrdContext = nibcCrdContext;
        this.nibcInformer=nibInformer;
        this.podInformer = podInformer;
        this.workQueue = new ArrayBlockingQueue<>(10);
        this.init();

    }


    public void loop(){
        logger.info("Native Controller started ...");

        while (! nibcInformer.hasSynced());
        while(true){

            logger.info("Fetching NativeImageBuildConfig from work queue ...");
            try {
                NativeImageBuildConfig nativeImageBuildConfig = workQueue.take();
                reconcile(nativeImageBuildConfig);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


        }
    }





    private void init(){

       //  Get the global configMap

        loadGlobalConfiguration();

        logger.info("Adding the NIBC Ressource Handler");
        nibcInformer.addEventHandler(new ResourceEventHandler<NativeImageBuildConfig>() {


            @Override
            public void onAdd(NativeImageBuildConfig nibc) {
                try {
                    logger.info("New NativeImageBuildConfig created "+ nibc);
                    addToQueue(nibc);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onUpdate(NativeImageBuildConfig oldNibc, NativeImageBuildConfig newNibc) {
                //logger.info("New NativeImageBuildConfig updated "+newNibc);
            }

            @Override
            public void onDelete(NativeImageBuildConfig obj, boolean deletedFinalStateUnknown) {
                logger.info(" NativeImageBuildConfig deleted "+obj + " status="+deletedFinalStateUnknown);
            }


        });

        logger.info("Adding the Pod Ressource Handler");

        podInformer.addEventHandler(new ResourceEventHandler<Pod>() {
            @Override
            public void onAdd(Pod nibcPod) {
                try {
                    Map<String, String> labels = nibcPod.getMetadata().getLabels();
                    if(labels!= null && labels.containsKey(COM_ORACLE_GRAALVM_NATIVEIMAGEBUILDCONFIG)) {

                        logger.info("New Pod created " + nibcPod.getMetadata().getName());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onUpdate(Pod oldNibc, Pod newNibc) {

                try {
                    Map<String, String> labels = newNibc.getMetadata().getLabels();
                    if(labels!= null && labels.containsKey(COM_ORACLE_GRAALVM_NATIVEIMAGEBUILDCONFIG)) {

                        logger.info("Builder Pod updated " + newNibc.getMetadata().getName() +" Phase"+newNibc.getStatus().getPhase() );
                        String ownerBuildConfigName = newNibc.getMetadata().getOwnerReferences().get(0).getName();
                        String namespace = newNibc.getMetadata().getNamespace();
                        //Load the nibc object
                        NativeImageBuildConfig nibcObj = nibCrdClient.inNamespace(namespace).withName(ownerBuildConfigName).get();
                        if(nibcObj.getStatus()!=null){ // avoid overlaps with first builder pod creation
                            addToQueue(nibcObj);
                        }

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDelete(Pod obj, boolean deletedFinalStateUnknown) {
                //logger.info(" Pod deleted "+obj + " status="+deletedFinalStateUnknown);

            }


        });
    }

    private void addToQueue(NativeImageBuildConfig nibc) {
        logger.info("Adding new nibc to queue "+nibc.getMetadata().getName());
        workQueue.add(nibc);
    }

    private void reconcile(NativeImageBuildConfig nibc) {

        //String Namespace;
        String namespace = nibc.getMetadata().getNamespace();
        String nativeImageBuildConfigName = nibc.getMetadata().getName();
        String nibcName = nativeImageBuildConfigName;

        //Load the nibc object
        NativeImageBuildConfig nibcObj = nibCrdClient.inNamespace(namespace).withName(nibcName).get();

        //Existing Status
        NativeImageBuildConfigStatus status = nibcObj.getStatus();
        if(status == null ){
            status= new   NativeImageBuildConfigStatus();
            nibcObj.setStatus(status);
        }

        try {


            //get the existing builder pod
            String lastBuilderPodName = status.getBuilderPod();
            Pod lastBuilderPod = null;
            if(  StringUtil.isNullOrEmpty(lastBuilderPodName)){
                //First time we have to create a builder pod
                status.setStatus("Pending");
                lastBuilderPod=createNewBuilderPod(nibcObj);

            }
            else{ // podName existing already existing in the status.

                lastBuilderPod = client.pods().inNamespace(namespace).withName(lastBuilderPodName).get();
                if( lastBuilderPod == null){
                    //fake builder pod name, pod deleted , need to be recreate
                    status.setStatus("Pending");
                    logger.info("Invalid builder pod found - "+lastBuilderPodName);
                    lastBuilderPod=createNewBuilderPod(nibcObj);

                }
                else{ // builder pod already exist update the status and exit. : Build status updated
                    logger.info("already runnig pod - "+lastBuilderPod.getMetadata().getName());
                    status.setStatus(lastBuilderPod.getStatus().getPhase().toString());
                    logger.info("Builder "+ nativeImageBuildConfigName+ "status changed to "+status.getStatus());
                    // Delete existing pod ?
                }



            }
            //update the CRD Status
            nibcObj.setStatus(status);
            client.customResource(nibcCrdContext).edit(namespace, nibcName, new ObjectMapper().writeValueAsString(nibcObj));
            logger.info("---Builder Updated  "+ SerializationUtils.dumpAsYaml(nibcObj));



        }catch (Exception e){
            logger.log(Level.SEVERE," error while creating builder pods",e );
        }

    }


    /**
     * Create new builder pod in the same namespace as the NativeImabeBuild Config CRD object
     * @param nibc the Native Image CRD build config
     * @return The created Pod
     */
    private Pod createNewBuilderPod(NativeImageBuildConfig nibc) {

        String nativeImageBuildConfigName = nibc.getMetadata().getName();
        String namespace = nibc.getMetadata().getNamespace();
        int counter=nibc.getStatus().getBuildCounter().intValue();
        //Create the builder pod
        String builderPodName =  nativeImageBuildConfigName +"-builderpod-"+counter;
        nibc.getStatus().setBuilderPod(builderPodName);

        // Get Source
        SourceSpec source = nibc.getSpec().getSource();

        //builder container environment variable
        List<EnvVar> builderEnv= new ArrayList<EnvVar>();
        builderEnv.add(new EnvVar(ENV_MAVEN_BUID_CMD,nibc.getSpec().getOptions().getMvnBuildCommand(),null));


        //Create a owner reference for the builder pod
        OwnerReference owner = new OwnerReference();
        owner.setKind(nibc.getKind());
        owner.setApiVersion(nibc.getApiVersion());
        owner.setName(nativeImageBuildConfigName);
        owner.setUid(nibc.getMetadata().getUid());

        //create new builder pod
        Pod builderPod = new PodBuilder()
                .withNewMetadata()
                    .withName(builderPodName)
                    .withNamespace(namespace)
                    .addToLabels(LABEL_BUILD_CONFIG, nativeImageBuildConfigName)
                    .addToLabels(COM_ORACLE_GRAALVM_NATIVEIMAGEBUILDCONFIG, "true")
                    .withOwnerReferences(owner)
                .endMetadata()
                .withNewSpec()
                    .withNewRestartPolicy("Never")
                .withContainers()
                .addNewContainer()
                .withName("builder")
                .withImage(builderImage)
                .withEnv(builderEnv)
                .withCommand("/native-image-build.sh")
                .withArgs(source.getGitUri(), source.getGitRef(), source.getContextDir(), null)
                .endContainer()
                .endSpec()
                .build();

        Pod resultPod = client.pods().inNamespace(namespace).create(builderPod);
        if(resultPod!=null){
            nibc.getStatus().setBuildCounter(counter+1);
            nibc.getStatus().setBuilderPod(resultPod.getMetadata().getName());
            nibc.getStatus().setStatus(resultPod.getStatus().getPhase());
        }
        logger.info(String.format("Build Pod  %d with Name %s and image %s",counter,builderPodName,builderImage) );
        return resultPod;
    }

    /**
     *
     */
    private void loadGlobalConfiguration() {

        //get the configmap list with
        ConfigMapList globalBuildConfigMapList = client.configMaps().inNamespace(client.getNamespace()).withLabelIn(COM_ORACLE_GRAALVM_NATIVEIMAGEBUILDCONFIG,"true" ).list();
        if(globalBuildConfigMapList!=null &&
                globalBuildConfigMapList.getItems()!=null &&
                 globalBuildConfigMapList.getItems().size()>0){

            globalBuilderConfigMap = globalBuildConfigMapList.getItems().get(0);
            logger.info("Using ConfigMap ...."+globalBuilderConfigMap.getMetadata().getName());
            Map<String, String> data = (Map<String, String>) globalBuilderConfigMap.getData();
            String result = data.get("application.properties");
            String[] props = result.split("\n");
            Map<String,String> keys = new HashMap<>();
            for (int i = 0; i < props.length; i += 1) {
                if(props[i].trim().startsWith("#")) continue;
                String[] kv = props[i].split("=");
                keys.put(kv[0],kv[1]);
            }
            //Read the properties
            builderImage= keys.get(APP_BUILDER_IMAGE).toString();

        }else{
            logger.info("No configMap defined using the default config from application.yaml");
            //load default Configuration parameters.
            ConfigSource configSource = ConfigSources.classpath("application.yaml").build();
            Config config = Config.builder()
                    .sources(configSource)
                    .build();

            builderImage= config.asMap().get().get(APP_BUILDER_IMAGE).toString();
            logger.info("No configMap defined --"+builderImage);

        }

    }


    public String getBuilderImage() {
        return builderImage;
    }

    public void setBuilderImage(String builderImage) {
        this.builderImage = builderImage;
    }
}
