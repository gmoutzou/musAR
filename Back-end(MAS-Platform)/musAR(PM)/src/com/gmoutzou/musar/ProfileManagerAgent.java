package com.gmoutzou.musar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import prof.onto.Information;
import prof.onto.MakeDBOperation;
import prof.onto.MakeOperation;
import prof.onto.Profile;
import prof.onto.ProfileOntology;
import prof.onto.ProfileVocabulary;

public class ProfileManagerAgent extends Agent implements ProfileVocabulary, ProfileProcessor {

	private static final long serialVersionUID = 5498058238099533885L;
	private Codec codec = new SLCodec();
	private Ontology ontology = ProfileOntology.getInstance();

	@Override
	protected void setup() {

		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);

		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType(PROFILE_MANAGEMENT);
		sd.setName(getLocalName() + "-profile-manager");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}

		addBehaviour(new ProfileManagement(this));
	}

	private class ProfileManagement extends CyclicBehaviour {

		private static final long serialVersionUID = 4722532886889416583L;

		private List<AID> dbManagerAgents = new ArrayList<AID>();
		private String dbManagerAgentName = "";
		AID dbManager = null;

		public ProfileManagement(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			// receive the client agent message
			ACLMessage msg = receive();
			if (msg != null) {
				System.out.println("[New message received]" + "\nContent: " + msg.getContent() + "\nSender: "
						+ msg.getSender() + "\nCommunicative act: " + msg.getPerformative() + "\nConversationId: "
						+ msg.getConversationId() + "\nLanguage: " + msg.getLanguage() + "\nOntology: "
						+ msg.getOntology() + "\n---------------------");
				if (msg.getPerformative() == ACLMessage.REQUEST) {
					// find from DF an DB Manager agent
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType(DB_MANAGEMENT);
					template.addServices(sd);
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template);
						dbManagerAgents.clear();
						for (int i = 0; i < result.length; i++) {
							dbManagerAgents.add(result[i].getName());
						}
					} catch (FIPAException fe) {
						fe.printStackTrace();
					}
					if (!dbManagerAgents.isEmpty()) {
						dbManagerAgentName = dbManagerAgents.get(0).getLocalName();
						System.out.println("found " + dbManagerAgentName);
						dbManager = new AID(dbManagerAgentName, AID.ISLOCALNAME);
					} else {
						System.out.println("DBManager not found!");
					}
					try {
						ContentElement content = getContentManager().extractContent(msg);
						MakeOperation op = (MakeOperation) ((Action) content).getAction();
						MakeDBOperation dbOp = new MakeDBOperation();
						Profile profile = new Profile();
						int type = op.getType();
						switch (type) {
						case CREATE_PROFILE: {
							profile = createProfileFromUserChoice(op.getAccount(), op.getUserChoice());
						}
							break;
						case READ_PROFILE: {
							profile.setAccount(op.getAccount());
						}
							break;
						case UPDATE_PROFILE: {
							profile = createProfileFromUserChoice(op.getAccount(), op.getUserChoice());
						}
							break;
						case DELETE_PROFILE: {
							profile.setAccount(op.getAccount());
						}
							break;
						case OTHER_OPERATION: {
							profile.setAccount(op.getAccount());
						}
							break;
						default:
							break;
						}
						dbOp.setType(type);
						dbOp.setProfile(profile);
						ACLMessage aclMsg = new ACLMessage(ACLMessage.REQUEST);
						aclMsg.addReceiver(dbManager);
						aclMsg.setLanguage(codec.getName());
						aclMsg.setOntology(ontology.getName());
						aclMsg.setConversationId(msg.getConversationId());
						aclMsg.addReplyTo(msg.getSender());
						getContentManager().fillContent(aclMsg, new Action(dbManager, dbOp));
						send(aclMsg);
					} catch (CodecException ce) {
						ce.printStackTrace();
					} catch (OntologyException oe) {
						oe.printStackTrace();
					}
				} else if (msg.getPerformative() == ACLMessage.INFORM) {
					try {
						ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
						ContentElement content = getContentManager().extractContent(msg);
						Information info = (Information) ((Action) content).getAction();
						reply.setConversationId(msg.getConversationId());
						reply.setLanguage(codec.getName());
						reply.setOntology(ontology.getName());
						AID receiver = new AID();
						Iterator it = msg.getAllReplyTo();
						while (it.hasNext()) {
							receiver = (AID) it.next();
							reply.addReceiver(receiver);
						}
						// send message to User
						getContentManager().fillContent(reply, new Action(receiver, info));
						send(reply);
					} catch (CodecException ce) {
						ce.printStackTrace();
					} catch (OntologyException oe) {
						oe.printStackTrace();
					}
				} else {
					ACLMessage reply = msg.createReply();
					reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
					reply.setContent("Unknown Communicative Act!");
					send(reply);
				}
			} else {
				block();
			}
		}
	}

	@Override
	protected void takeDown() {
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}

	@Override
	public Profile createProfileFromUserChoice(String account, String userChoice) {
		Profile profile = new Profile();
		String name = "";
		int uc = Integer.parseInt(userChoice);
		if (uc % 2 == 0) {
			name = "Pro-A";
		} else {
			name = "Pro-B";
		}
		profile.setAccount(account);
		profile.setName(name);
		return profile;
	}

}