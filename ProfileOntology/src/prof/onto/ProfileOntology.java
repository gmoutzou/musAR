package prof.onto;

import jade.content.onto.BasicOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.schema.AgentActionSchema;
import jade.content.schema.ConceptSchema;
import jade.content.schema.ObjectSchema;
import jade.content.schema.PrimitiveSchema;

public class ProfileOntology extends Ontology implements ProfileVocabulary {

	// ----------> The name identifying this ontology
	public static final String ONTOLOGY_NAME = "Profile-Ontology";

	// ----------> The singleton instance of this ontology
	private static Ontology instance = new ProfileOntology();

	// ----------> Method to access the singleton ontology object
	public static Ontology getInstance() {
		return instance;
	}

	// Private constructor
	private ProfileOntology() {

		super(ONTOLOGY_NAME, BasicOntology.getInstance());

		try {
			
			// ------- Add Concepts
			
			// Profile
			ConceptSchema cs = new ConceptSchema(PROFILE);
			add(cs, Profile.class);
			cs.add(PROFILE_ACCOUNT, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
			cs.add(PROFILE_NAME, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);

			// Status
			add(cs = new ConceptSchema(STATUS), Status.class);
			cs.add(STATUS_CODE, (PrimitiveSchema) getSchema(BasicOntology.INTEGER), ObjectSchema.MANDATORY);
			cs.add(STATUS_MESSAGE, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);

			// ------- Add AgentActions

			// MakeOperation
			AgentActionSchema as = new AgentActionSchema(MAKE_OPERATION);
			add(as, MakeOperation.class);
			as.add(MAKE_OPERATION_TYPE, (PrimitiveSchema) getSchema(BasicOntology.INTEGER), ObjectSchema.MANDATORY);
			as.add(MAKE_OPERATION_ACCOUNT, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.MANDATORY);
			as.add(MAKE_OPERATION_USER_CHOICE, (PrimitiveSchema) getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
			
			// MakeDBOperation
			add(as = new AgentActionSchema(MAKE_DB_OPERATION), MakeDBOperation.class);
			as.add(MAKE_DB_OPERATION_TYPE, (PrimitiveSchema) getSchema(BasicOntology.INTEGER), ObjectSchema.MANDATORY);
			as.add(MAKE_DB_OPERATION_PROFILE, (ConceptSchema) getSchema(PROFILE), ObjectSchema.MANDATORY);
			
			// Information
			add(as = new AgentActionSchema(INFORMATION), Information.class);
			as.add(INFORMATION_TYPE, (PrimitiveSchema) getSchema(BasicOntology.INTEGER), ObjectSchema.MANDATORY);
			as.add(INFORMATION_PROFILE, (ConceptSchema) getSchema(PROFILE), ObjectSchema.OPTIONAL);
			as.add(INFORMATION_STATUS, (ConceptSchema) getSchema(STATUS), ObjectSchema.MANDATORY);
		
		} catch (OntologyException oe) {
			oe.printStackTrace();
		}
	}
}
