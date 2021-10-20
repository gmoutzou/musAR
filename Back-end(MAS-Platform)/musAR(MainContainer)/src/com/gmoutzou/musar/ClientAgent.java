package com.gmoutzou.musar;

import prof.onto.MakeOperation;
import prof.onto.ProfileOntology;
import prof.onto.ProfileVocabulary;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class ClientAgent extends Agent implements ProfileVocabulary {

	private static final long serialVersionUID = 1399145666034808797L;

	private Codec codec = new SLCodec();
	private Ontology ontology = ProfileOntology.getInstance();

	@Override
	protected void setup() {
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		sendMsg();
	}

	@Override
	protected void takeDown() {

	}

	public void sendMsg() {
		addBehaviour(new OneShotBehaviour() {

			private static final long serialVersionUID = 3567060938019966328L;

			private String pManagerAgentName = "";
			private List<AID> pManagerAgents = new ArrayList<AID>();

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
					ACLMessage msg = new ACLMessage(ACLMessage.INFORM_IF);
					pManagerAgentName = pManagerAgents.get(0).getLocalName();
					System.out.println("found " + pManagerAgentName);
					AID receiver = new AID(pManagerAgentName, AID.ISLOCALNAME);
					msg.addReceiver(receiver);
					msg.setLanguage(codec.getName());
					msg.setOntology(ontology.getName());
					String uniqueID = UUID.randomUUID().toString();
					msg.setConversationId(uniqueID);
					MakeOperation op = new MakeOperation();
					op.setType(OTHER_OPERATION);
					op.setAccount("gmoutzou@gmail.com");
					op.setUserChoice("Sel-B");
					try {
						getContentManager().fillContent(msg, new Action(receiver, op));
						send(msg);
						msg = null;
						msg = blockingReceive();
						if (msg != null) {
							System.out.println("[Client - FINAL Message received]" 
									+ "\nContent: "
									+ msg.getContent() 
									+ "\nSender: "
									+ msg.getSender() 
									+ "\nCommunicative act: " 
									+ msg.getPerformative()
									+ "\nConversationId: "  
									+ msg.getConversationId()
									+ "\nLanguage: " 
									+ msg.getLanguage() 
									+ "\nOntology: "  
									+ msg.getOntology() 
									+ "\n--------------------------------");
						}
					} catch (CodecException ce) {
						ce.printStackTrace();
					} catch (OntologyException oe) {
						oe.printStackTrace();
					}
				} else {
					System.out.println("Profile Manager not found!");
				}
			}
		});
	}

}

