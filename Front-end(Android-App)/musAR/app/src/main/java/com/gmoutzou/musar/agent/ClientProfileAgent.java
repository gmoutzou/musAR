package com.gmoutzou.musar.agent;

import com.gmoutzou.musar.utils.CallbackInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.Logger;
import prof.onto.Information;
import prof.onto.MakeOperation;
import prof.onto.ProfileOntology;
import prof.onto.ProfileVocabulary;

public class ClientProfileAgent extends Agent implements ClientProfileInterface, ProfileVocabulary {

    private static final long serialVersionUID = 1399145666034808797L;
    private Logger logger = Logger.getJADELogger(this.getClass().getName());

    private Codec codec = new SLCodec();
    private Ontology ontology = ProfileOntology.getInstance();

    private CallbackInterface callbackInterface;

    @Override
    protected void setup() {

        // Activate the GUI
        registerO2AInterface(ClientProfileInterface.class, this);

        // Register codec and ontology
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

    }

    private class ProfileRequest extends OneShotBehaviour {

        private static final long serialVersionUID = 3567060938019966328L;

        private String pManagerAgentName = "";
        private List<AID> pManagerAgents = new ArrayList<AID>();
        private MakeOperation op;

        public ProfileRequest(Agent a, MakeOperation op) {
            super(a);
            this.op = op;
        }

        @Override
        public void action() {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(PROFILE_MANAGEMENT);
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                pManagerAgents.clear();
                for (int i = 0; i < result.length; i++) {
                    pManagerAgents.add(result[i].getName());
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
            if (!pManagerAgents.isEmpty()) {
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                pManagerAgentName = pManagerAgents.get(0).getLocalName();
                System.out.println("found " + pManagerAgentName);
                AID receiver = new AID(pManagerAgentName, AID.ISLOCALNAME);
                msg.addReceiver(receiver);
                msg.setLanguage(codec.getName());
                msg.setOntology(ontology.getName());
                String uniqueID = UUID.randomUUID().toString();
                msg.setConversationId(uniqueID);
                try {
                    getContentManager().fillContent(msg, new Action(receiver, op));
                    send(msg);
                    ACLMessage aclMsg = blockingReceive(MessageTemplate.MatchConversationId(uniqueID));
                    if (aclMsg != null) {
                        logger.log(Level.INFO,"[New Message received]"
                                + "\nContent: "
                                + aclMsg.getContent()
                                + "\nSender: "
                                + aclMsg.getSender()
                                + "\nCommunicative act: "
                                + aclMsg.getPerformative()
                                + "\nConversationId: "
                                + aclMsg.getConversationId()
                                + "\nLanguage: "
                                + aclMsg.getLanguage()
                                + "\nOntology: "
                                + aclMsg.getOntology()
                                + "\n--------------------------------");
                        if (aclMsg.getPerformative() == ACLMessage.INFORM) {
                            ContentElement content = getContentManager().extractContent(aclMsg);
                            Information info = (Information) ((Action) content).getAction();
                            if (info != null) {
                                callbackInterface.callbackMethod(info);
                            }
                        } else {
                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                            reply.setContent("Unknown Communicative Act!");
                            send(reply);
                        }
                    }
                } catch (Codec.CodecException ce) {
                    logger.log(Level.WARNING, ce.getMessage());
                } catch (OntologyException oe) {
                    logger.log(Level.WARNING, oe.getMessage());
                }
            } else {
                logger.log(Level.WARNING, "Profile Manager not found!");
            }
        }
    }

    @Override
    protected void takeDown() {
        //TODO
    }

    // //////////////////////////////////
    // Methods called by the interface
    // //////////////////////////////////
    @Override
    public void registerCallbackInterface(CallbackInterface callbackInterface) {
        this.callbackInterface = callbackInterface;
    }

    @Override
    public void requestProfileOperation(MakeOperation op) {
        addBehaviour(new ProfileRequest(this, op));
    }
}