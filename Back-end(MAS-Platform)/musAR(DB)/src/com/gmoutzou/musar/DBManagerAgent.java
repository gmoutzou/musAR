package com.gmoutzou.musar;

import prof.onto.Information;
import prof.onto.MakeDBOperation;
import prof.onto.Profile;
import prof.onto.ProfileOntology;
import prof.onto.ProfileVocabulary;
import prof.onto.Status;

import java.util.Iterator;

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
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class DBManagerAgent extends Agent implements ProfileVocabulary {

	private static final long serialVersionUID = -1639788290964311857L;

	private Codec codec = new SLCodec();
	private Ontology ontology = ProfileOntology.getInstance();
	private DBConnection dbConn;
	private boolean dbFlag = false;

	@Override
	protected void setup() {
		dbConn = new DBConnection();
		dbFlag = dbConn.createConnection();
		if (dbFlag) {
			System.out.println("DBManager agent connected successfully to database");
			DFAgentDescription dfd = new DFAgentDescription();
			dfd.setName(getAID());
			ServiceDescription sd = new ServiceDescription();
			sd.setType(DB_MANAGEMENT);
			sd.setName(getLocalName() + "-database-manager");
			dfd.addServices(sd);
			try {
				DFService.register(this, dfd);
			} catch (FIPAException fe) {
				fe.printStackTrace();
			}

			getContentManager().registerLanguage(codec);
			getContentManager().registerOntology(ontology);

			addBehaviour(new DBManagement(this));
		}
	}

	private class DBManagement extends CyclicBehaviour {

		private static final long serialVersionUID = -808480682927421608L;

		public DBManagement(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			ACLMessage msg = receive();
			if (msg != null) {
				if (msg.getPerformative() == ACLMessage.REQUEST) {
					try {
						Information info = new Information();
						Profile profile = new Profile();
						Status status = new Status();
						status.setCode(SUCCESS);
						status.setMessage(OK);
						ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
						reply.setLanguage(codec.getName());
						reply.setOntology(ontology.getName());
						reply.setConversationId(msg.getConversationId());
						reply.addReceiver(msg.getSender());
						Iterator it = msg.getAllReplyTo();
						while (it.hasNext()) {
							reply.addReplyTo((AID) it.next());
						}
						ContentElement content = getContentManager().extractContent(msg);
						MakeDBOperation op = (MakeDBOperation) ((Action) content).getAction();
						profile = op.getProfile();
						int type = op.getType();
						switch (type) {
						case CREATE_PROFILE:
							if (!dbConn.createProfile(profile)) {
								status.setCode(ERROR);
								status.setMessage(UNKNOWN_ERROR);
							}
							break;
						case READ_PROFILE:
							profile = dbConn.readProfile(profile.getAccount());
							if (profile == null) {
								status.setCode(ERROR);
								status.setMessage(ACCOUNT_NOT_FOUND);
							}
							break;
						case UPDATE_PROFILE:
							if (!dbConn.updateProfile(profile)) {
								status.setCode(ERROR);
								status.setMessage(UNKNOWN_ERROR);
							}
							break;
						case DELETE_PROFILE:
							if (!dbConn.deleteProfile(profile)) {
								status.setCode(ERROR);
								status.setMessage(UNKNOWN_ERROR);
							}
							break;
						case OTHER_OPERATION:
							status.setCode(ERROR);
							status.setMessage(ILLEGAL_OPERATION);
							break;
						default:
							break;
						}
						info.setType(type);
						info.setProfile(profile);
						info.setStatus(status);
						getContentManager().fillContent(reply, new Action(msg.getSender(), info));
						send(reply);
					} catch (CodecException ce) {
						ce.printStackTrace();
					} catch (OntologyException oe) {
						oe.printStackTrace();
					}
				}
			} else {
				block();
			}
		}
	}

	@Override
	protected void takeDown() {
		if (dbFlag) {
			dbConn.closeConnection();
			try {
				DFService.deregister(this);
			} catch (FIPAException fe) {
				fe.printStackTrace();
			}
		}
	}

}
