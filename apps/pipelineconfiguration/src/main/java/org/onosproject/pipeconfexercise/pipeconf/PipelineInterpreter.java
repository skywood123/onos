package org.onosproject.pipeconfexercise.pipeconf;


import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MplsLabel;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiPacketMetadataId;
import org.onosproject.net.pi.model.PiPacketOperationType;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.onosproject.net.pi.runtime.PiPacketMetadata;
// With the help of static import, we can access the static members of a class directly
// without class name or any object. For Example: we always use sqrt() method of Math class by using Math class
// i.e. Math.sqrt(), but by using static import we can access sqrt() method directly.
//t will lead to confusion and not good for programming.
// If there is no specific requirement then we should not go for static import
import static java.lang.String.format;
import static org.onosproject.net.flow.instructions.Instruction.Type.L2MODIFICATION;
import static org.onosproject.net.flow.instructions.Instruction.Type.OUTPUT;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

//use the PiPipelineInterpreter Interface to do the necessary jobs required
//AbstractHandlerBehaviour : Base implementation of a driver handler behaviour.
public class PipelineInterpreter
        extends AbstractHandlerBehaviour
        implements PiPipelineInterpreter {


    //Eventually an PiMatchFieldId/PiActionId
    // looks like
    //standard_metadata.ingress_port
    //hdr.ethernet.dst_addr ( for ETH_DST)
    //something.something.something
    private static final String DOT = ".";
    private static final String HDR = "hdr";
    private static final String ETHERNET = "ethernet";
    private static final String STANDARD_METADATA = "standard_metadata";
    //private static final String INGRESS_CONTROL = "ingress_control";
    private static final String INGRESS_CONTROL = "ingress";
    //private static final String L2_FWD = "forwarding.l2_fwd";
    private static final String L2_FWD = "table0_control.table0";
    private static final String INGRESS_PORT = "ingress_port";
    private static final String EGRESS_PORT = "egress_port";
    private static final int PORT_FIELD_BITWIDTH = 16;
    private static final String IPV4 = "ipv4";
    private static final String MPLS = "mpls";
    private static final String MPLS_PROCESSING_TABLE = "forwarding.mpls_forwarding_table";
    /*
    private static final String
    private static final String
    private static final String
    private static final String
    private static final String
    private static final String
    private static final String
    */

    //declare the matching field for table in p4 in something.something.something form
    //the naming of ETH_DST_ID , ETH_SRC_ID is to give a good practice that
    //it resemble the official enum type for the Criterion.type
    //the Criterion.type looks like
    // ETH_SRC , ETH_DST, ETH_TYPE
    private static final PiMatchFieldId INGRESS_PORT_ID=
            PiMatchFieldId.of(STANDARD_METADATA + DOT + INGRESS_PORT);
    private static final PiMatchFieldId ETH_DST_ID=
            PiMatchFieldId.of(HDR + DOT + ETHERNET +DOT+ "dst_addr");
    private static final PiMatchFieldId ETH_SRC_ID=
            PiMatchFieldId.of(HDR + DOT + ETHERNET + DOT + "src_addr");
    private static final PiMatchFieldId ETH_TYPE_ID=
            PiMatchFieldId.of(HDR +  DOT + ETHERNET + DOT + "ether_type");
    private static final PiMatchFieldId MPLS_LABEL_ID=
            PiMatchFieldId.of(HDR +  DOT + MPLS + DOT + "label");
    private static final PiMatchFieldId MPLS_BOS_ID=
            PiMatchFieldId.of(HDR +  DOT + MPLS + DOT + "s");
    private static final PiMatchFieldId IPV4_DEST_ID=
            PiMatchFieldId.of(HDR +  DOT + IPV4 + DOT + "dst_addr");

    private static final PiTableId L2_FWD_ID =
            PiTableId.of(INGRESS_CONTROL + DOT + L2_FWD);
    private static final PiTableId MPLS_PROCESSING_TABLE_ID =
            PiTableId.of(INGRESS_CONTROL + DOT + MPLS_PROCESSING_TABLE);

    private static final PiActionId ACT_ID_NOP=
            PiActionId.of("NoAction");
    private static final PiActionId ACT_ID_SEND_TO_CPU=
            PiActionId.of(INGRESS_CONTROL + DOT +"table0_control.send_to_cpu"); //action must specify for which pipeline (ingress pipeline)
    private static final PiActionId ACT_ID_SET_EGRESS_PORT=      //because this action is defined inside this ingress pipeline
            PiActionId.of(INGRESS_CONTROL + DOT + "table0_control.set_egress_port"); //need to specify the full "url" that indicate it's under this pipeline

    //FIXME needed?
    private static final PiActionId ACT_ID_MPLS_PUSH=
            PiActionId.of(INGRESS_CONTROL + DOT + "table0_control.mpls_push");
    private static final PiActionId ACT_ID_MPLS_POP=
            PiActionId.of(INGRESS_CONTROL + DOT + "table0_control.mpls_pop");
    private static final PiActionId ACT_ID_MPLS_SWAP=
            PiActionId.of(INGRESS_CONTROL + DOT + "table0_control.mpls_swap");
    private static final PiActionParamId ACT_PARAM_ID_PORT=
            PiActionParamId.of("port");
    private static final PiActionParamId ACT_PARAM_ID_MPLS_LABEL_ID=
            PiActionParamId.of("mpls_label_id");

    //create a mapping between ONOS table(the integer) with Our P4 Program table that provide
    //similar functions, if additional function is added, no need to map
    //only map what P4 program wish to support the original ONOS table function
    //Onos represent their table in the form of interger, like table=0;table =1


    //TODO table for metering need to add in?
    //Guava bidirectional map
    private static final BiMap<Integer, PiTableId> TABLE_MAP=
            new ImmutableBiMap.Builder<Integer,PiTableId>()
            .put(0,L2_FWD_ID).build();

    //Guava bidirectional map
    //Key = Criterion.Type
    //Value= PiMatchFieldId
    private static final BiMap<Criterion.Type,PiMatchFieldId> CRITERION_MAP=
            new ImmutableBiMap.Builder<Criterion.Type,PiMatchFieldId>()
                    .put(Criterion.Type.IN_PORT,INGRESS_PORT_ID)
                    .put(Criterion.Type.ETH_DST,ETH_DST_ID)
                    .put(Criterion.Type.ETH_SRC,ETH_SRC_ID)
                    .put(Criterion.Type.ETH_TYPE,ETH_TYPE_ID)
                    .put(Criterion.Type.MPLS_LABEL,MPLS_LABEL_ID)
                    .put(Criterion.Type.MPLS_BOS,MPLS_BOS_ID)
                    .put(Criterion.Type.IPV4_DST,IPV4_DEST_ID).build();

    //Criterion is the match in ONOS term
    //Due to the fact that we are actually mapping many matching/criterion, we use a map
    //to cover multiple entry of criterion to PiMatchField
    //list of criterion used by ONOS can be found at
    //http://api.onosproject.org/1.15.0/apidocs/      //search criterion.type

    //by asking for the Criterion.Type entry from the map, should get back the
    //corresponding PiMatchFieldId from the mapping.
    //Key = Criterion.Type
    //Value = PiMatchFieldId

    //Optional : A container object which may or may not contain a non-null value.
    // If a value is present, isPresent() will return true and get() will return the value.

    //ofNullable : Returns an Optional describing the specified value ,if non-null,
    // otherwise returns an empty Optional.
    @Override
    public Optional<PiMatchFieldId> mapCriterionType(Criterion.Type type) {
        return Optional.ofNullable(CRITERION_MAP.get(type));
    }

    //Deprecated in ONOS2.1
  //  @Override
  //  public Optional<Criterion.Type> mapPiMatchFieldId(PiMatchFieldId headerFieldId){
  //      return Optional.ofNullable(CRITERION_MAP.inverse().get(headerFieldId));
  //  }
    @Override
    public Optional<PiTableId> mapFlowRuleTableId(int flowRuleTableId) {
        return Optional.ofNullable(TABLE_MAP.get(flowRuleTableId));
    }
    //Deprecated in ONOS2.1
  //  @Override
  //  public Optional<Integer> mapPiTableId(PiTableId piTableId){
  //      return Optional.ofNullable(TABLE_MAP.inverse().get(piTableId));
  //  }

    //In here, we are doing the mapping between the action that callable / supported by the DataPlane / our P4 program
    //TELLING DATAPLANE
    //What it should do for every packet that passing through this L2_FWD table

    //for this incoming treatment/insturction to this table, what is the correspond Action in P4 dataplane
    //Eventually we want the PiAction for the dataplane
    //TODO add in mpls processing for the instruction
    //L2modificationInstruction.modMPLSbos and modMPLSLabel
    //popmpls pushmpls

    @Override
    public PiAction mapTreatment(TrafficTreatment treatment, PiTableId piTableId) throws PiInterpreterException {
        //global or local?
        List<Instruction> temp;
        temp=(treatment.allInstructions());
        //since now only has a piTable (L2_FWD)
        //we filter and make sure the targetting pitable is targeting to L2_FWD only
        if(piTableId!= L2_FWD_ID ){
            throw new PiInterpreterException("Can map treatments only for 'l2_fwd' table");
        }
        //.allInstructions(): Return the list of all instructions in the treatment, both immediate and deferred.
        if (treatment.allInstructions().size()==0)
            return PiAction.builder().withId(ACT_ID_NOP).build();

        //data plane only support a single action for an entry
        //but in ONOS it might come with multiple action for a single matching
        else if(treatment.allInstructions().size()>=1){
            //ensure only contain L2MODIFICATION(for mpls) and OUTPUT

            for(int i =0;i<treatment.allInstructions().size();i++) {
            //    System.out.println(treatment.allInstructions().get(i).type().toString());

                if(temp.get(i).type().toString().equalsIgnoreCase("L2MODIFICATION")==false && temp.get(i).type().toString().equalsIgnoreCase("OUTPUT")==false)
                    throw new PiInterpreterException("Treatment has instructions different from [L2MODIFICATION,OUTPUT]: FAULTY instruction " +
                                                             "= " + temp.get(i));
            }
        }
        //support only 1 single instruction
        //In here will get a Instruction object with supported method Instruction.Type() only
        Instruction instruction=treatment.allInstructions().get(treatment.allInstructions().size()-1);

        //refer to Instruction.type in ONOS API DOCS
        //OUTPUT means the traffic should be output to a port
        //table means the traffic should pass to another table
        //METER means the traffic should be metered according to a meter
    //    if(instruction.type()!= OUTPUT){
    //        throw new PiInterpreterException(format("Instruction of type '%s' not supported",instruction.type()));
    //    }

        //if 2 , MPLS POP(IPV4) ,OUTPUT
        //if 3 , MPLS_PUSH,MPLS_LABEL,OUTPUT

        //Until now, we can ensure the instruction type we get confirm is the OUTPUT
        //the above Instruction is an interface allowed only to access the instruction type only
        //Now need to access more information for the instruction
        //So cast the original instruction into the OutputInstruction Type
        //Our data plane action need to access the egressing port number from the instruction

        Instructions.OutputInstruction outInstruction = (Instructions.OutputInstruction) instruction;
        PortNumber port = outInstruction.port();

        //TODO where declared the logical port
        //guess : From ONOS view, ONOS know how many port from the device is physical
        //If this instruction is configuring a matching entry to send to port that is not equal to
        //the port on that device or other specific port known, then call exception;
        //Because not supporting FLOOD in dataplane, so not capturing FLOOD port
        if(temp.size()==1) {
            if (!port.isLogical()) {
                return PiAction.builder()
                        .withId(ACT_ID_SET_EGRESS_PORT)
                        .withParameter(new PiActionParam(
                                ACT_PARAM_ID_PORT, ImmutableByteSequence.copyFrom(port.toLong())))
                        .build();
            } else if (port.equals(PortNumber.CONTROLLER)) {
                return PiAction.builder()
                        .withId(ACT_ID_SEND_TO_CPU)
                        .build();

            } else {
                throw new PiInterpreterException(format("Output on logical port '%s' is not supported", port));

            }
        }else if(temp.size()==2){
           // if(temp.get(0).type().getClass().equals(L2ModificationInstruction.ModMplsHeaderInstruction.class))
            if(temp.get(0).type().toString().equalsIgnoreCase("L2MODIFICATION")) {
                L2ModificationInstruction checker = (L2ModificationInstruction)temp.get(0);
                if(checker.subtype().toString().equals("MPLS_POP"))
                return PiAction.builder()
                        .withId(ACT_ID_MPLS_POP)
                        .withParameter(new PiActionParam(
                                ACT_PARAM_ID_PORT, ImmutableByteSequence.copyFrom(port.toLong()))).build();

                else if (checker.subtype().toString().equals("MPLS_LABEL")) {
                    MplsLabel mpls_label = ((L2ModificationInstruction.ModMplsLabelInstruction) treatment.allInstructions().get(0)).label();
                    return PiAction.builder()
                            .withId(ACT_ID_MPLS_SWAP)
                            .withParameter(new PiActionParam(
                                    ACT_PARAM_ID_PORT, ImmutableByteSequence.copyFrom(port.toLong())))
                            .withParameter(new PiActionParam(ACT_PARAM_ID_MPLS_LABEL_ID, ImmutableByteSequence.copyFrom(mpls_label.toInt())))
                            .build();
                } else
                    throw new PiInterpreterException("Not understand instruction size 2 with subtype" + ((L2ModificationInstruction) temp.get(0)).subtype().toString());
            }
                else
                    throw new PiInterpreterException("Instruction is not for mpls, not understand " + temp.get(0).type().toString());
            }else if(temp.size()==3){
            MplsLabel mpls_label =((L2ModificationInstruction.ModMplsLabelInstruction)treatment.allInstructions().get(1)).label();
            return PiAction.builder()
                    .withId(ACT_ID_MPLS_PUSH)
                    .withParameter(new PiActionParam(ACT_PARAM_ID_MPLS_LABEL_ID,ImmutableByteSequence.copyFrom(mpls_label.toInt())))
                    .withParameter(new PiActionParam(ACT_PARAM_ID_PORT,ImmutableByteSequence.copyFrom(port.toLong()))).build();
        }else
            throw new PiInterpreterException("Error in number of instruction contained, max =3 but get size = " + temp.size());
    }

    //because in packet_out and packet_in, the metadata fields is possibly modified by the programmer
    //so need to do a mapping
    //PACKET_OUT ; ONOS is replying to the packet_IN from switches
    //ONOS ask the switch to send packet to somewhere
    //a single port, flooding, etc
    //could be a list of actions to do, hence using Collection

    //OutboundPacket API:
    //data() : return immutable view of the raw data to be sent
    //sendThrough : return destination deviceID
    //treatment() : return a traffictreatment Type of how this packet should be treated.
    @Override
    public Collection<PiPacketOperation> mapOutboundPacket(OutboundPacket packet) throws PiInterpreterException {
        TrafficTreatment treatment = packet.treatment();

        //only supporting OUTPUT instruction here (sending to port)
        if(treatment.allInstructions().size()!=1 || treatment.allInstructions().get(0).type()!= OUTPUT)
            throw new PiInterpreterException("Treatment not supported " + treatment.toString());
        Instruction instruction = treatment.allInstructions().get(0);
        PortNumber port = ((Instructions.OutputInstruction)instruction).port();
        List<PiPacketOperation> piPacketOps = Lists.newArrayList();

        if(!port.isLogical()){
            piPacketOps.add(createPiPacketOp(packet.data(),port.toLong()));
        }else if(port.equals(PortNumber.FLOOD)){
            //This flood depend on our dataplane as well
            //I didnt put flood function in the data plane
            // then we instruct the dataplane to send out unicastly into everyport from here then
            DeviceService deviceService = handler().get(DeviceService.class);
            //sendthrough is getting the destination of original packet
            DeviceId deviceId = packet.sendThrough();
            for(Port p : deviceService.getPorts(deviceId)){
                piPacketOps.add(createPiPacketOp(packet.data(),p.number().toLong()));
            }
        }else{
            throw new PiInterpreterException(format("Output on logical port '%s' not supported",port));
        }
        return piPacketOps;
    }

    //extracting a packet from P4RUNTIME and send as a InBoundPacket to ONOS for further processing
    @Override
    public InboundPacket mapInboundPacket(PiPacketOperation packetIn, DeviceId deviceId) throws PiInterpreterException {
        //For simplicity, assume packet_in is only ethernet.
        Ethernet ethPkt;
        try{
            //to deserialize a packet from a byte-based input stream.
            //deserialize(byte[] data, int offset, int length)
            ethPkt = Ethernet.deserializer().deserialize(packetIn.data().asArray(),0,packetIn.data().size());

        }catch(DeserializationException dex){
            throw new PiInterpreterException(dex.getMessage());
        }

        //extract the ingress port from packet metadata
        //PiPacketMetadata ;Instance of a packet I/O operation, and its control metadatas,
        //for a protocol-independent pipeline.

        //A PiPacketMetadata object , allow to do
        //data()
        //deviceId()
        //metadatas() :Returns all metadatas of this packet.
        //            Returns an empty collection if the packet doesn't have any metadata.
        //            Return Collection of metadatas
        Optional<PiPacketMetadata> packetMetadata = packetIn.metadatas().stream()
                //.filter ; Returns a stream consisting of the elements of this stream that match the given predicate.
                //lambda operator ->
                //lambad parameter -> lambda body
                .filter(metadata -> metadata.id().toString().equals(INGRESS_PORT))
                //.findFirst()
                //Returns an Optional describing the first element of this stream,
                // or an empty Optional if the stream is empty.
                .findFirst();

        if (packetMetadata.isPresent()){
            short s = packetMetadata.get().value().asReadOnlyBuffer().getShort();
            //which connection point is this packet come from, the device, and the port that trigger this
            //packet-in packet
            ConnectPoint receivedFrom = new ConnectPoint(
                    deviceId,PortNumber.portNumber(s));
            return new DefaultInboundPacket(
                    receivedFrom, ethPkt, packetIn.data().asReadOnlyBuffer());
        }else{
            throw new PiInterpreterException(format("Missing metadata '%s' in packet-in received from '%s':'%s'",
                                             INGRESS_PORT,deviceId,packetIn));
        }
    }

    private PiPacketOperation createPiPacketOp (ByteBuffer data, long portNumber) throws PiInterpreterException {
        PiPacketMetadata metadata = createControlMetadata(portNumber);
        return PiPacketOperation.builder()
                //This is missing since ONOS 1.15 to later
                //RUN and see how first
                //.forDevice(this.data().deviceId())
                .withType(PiPacketOperationType.PACKET_OUT)
                .withData(ImmutableByteSequence.copyFrom(data))
                .withMetadatas(ImmutableList.of(metadata))
                .build();

    }

    private PiPacketMetadata createControlMetadata (long portNumber) throws PiInterpreterException{
        try{
            return PiPacketMetadata.builder()
                    .withId(PiPacketMetadataId.of(EGRESS_PORT))
                    .withValue(ImmutableByteSequence.copyFrom(portNumber).fit(PORT_FIELD_BITWIDTH))
                    .build();
        }catch(ImmutableByteSequence.ByteSequenceTrimException e){
            throw new PiInterpreterException(format("Port number %d too big ,%s",portNumber,e.getMessage()));
        }
    }
}
