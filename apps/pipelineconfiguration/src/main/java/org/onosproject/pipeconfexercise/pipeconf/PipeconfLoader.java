package org.onosproject.pipeconfexercise.pipeconf;


//import org.apache.felix.scr.annotations.Component;
//changes here where OSGi plugin had replaced felix scr plugin(deprecated)
//@Component(immediate = true) //for public final class PipeconfLoader
//Now the package  “org.apache.felix.scr.annotations.*” will be replaced with
// “org.osgi.service.component.annotations.*”  and
// “org,osgi.service.metatype.annotations.*”

import org.onosproject.driver.pipeline.DefaultSingleTablePipeline;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.pi.model.DefaultPiPipeconf;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
//this is the pipeconf service exposed from ONOS
//where we are going to register to it
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.p4runtime.model.P4InfoParser;
import org.onosproject.p4runtime.model.P4InfoParserException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

//Adobe Experience Manager (AEM)
//This is to use the OSGi Declarative Service
//https://osgi.org/javadoc/r6/cmpn/org/osgi/service/component/annotations/Component.html
//Declares whether this Component must be immediately activated
// upon becoming satisfied or whether activation should be delayed.
@Component(immediate = true)
//final ; this class cannot be extend by others(inherited)
public final class PipeconfLoader {
    private final Logger log = LoggerFactory.getLogger(getClass());

    //an identifier for the created pipeconf to register with OSOS pipeconf service
    //What the custom created side pipeline is categorized under [ Pi ]
    public static final PiPipeconfId PIPECONF_ID= new PiPipeconfId("pipeconfexercise");

    //to get the p4info file
    private static final URL P4INFO_URL= PipeconfLoader.class.getResource("/p4info.txt");

    private static final URL BMV2_JSON_URL= PipeconfLoader.class.getResource("/bmv2.json");

    //Mandatory, unary reference: At least one service must be
    // available for the reference to be satisfied.
    //Only a single service is available through this reference.
    //This is a service exposed from ONOS ; the PipeconfService for us to register with.
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private PiPipeconfService piPipeconfService;

    //Once this class/component(in OSGi) activated, activate this method
    @Activate
    public void activate(){
        //By activating this method, register our pipeline defined to the pipeconf service
        //TODO
        try {
            piPipeconfService.register(buildPipeconf());
        } catch (P4InfoParserException e) {
            log.error("Fail to register {} - Exception: {} - Cause: {}",
                      PIPECONF_ID,e.getMessage(),e.getCause().getMessage());
        }
    }

    @Deactivate
    public void deactivate(){
        //When this component get deactivated, we need to unregister our pipeline from the
        //pipeconf service
        try{
            piPipeconfService.unregister(PIPECONF_ID);
        }catch(IllegalStateException e){
            log.warn("Sweet heart, this pipeconf {} haven't been registered",PIPECONF_ID);
        }
    }

    private PiPipeconf buildPipeconf() throws P4InfoParserException {

        //p4info file = PiPipelineModel in ONOS
        final PiPipelineModel pipelineModel = P4InfoParser.parse(P4INFO_URL);

        return DefaultPiPipeconf.builder()
                .withId(PIPECONF_ID)
                .withPipelineModel(pipelineModel)
                .addBehaviour(PiPipelineInterpreter.class, PipelineInterpreter.class)
                .addBehaviour(Pipeliner.class, DefaultSingleTablePipeline.class)
                .addBehaviour(PortStatisticsDiscovery.class,PortStatisticsDiscoveryImple.class)
                .addExtension(PiPipeconf.ExtensionType.P4_INFO_TEXT,P4INFO_URL)
                .addExtension(PiPipeconf.ExtensionType.BMV2_JSON,BMV2_JSON_URL)
                .build();


    }
}
