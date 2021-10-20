package prof.onto;

import jade.content.AgentAction;

public class MakeDBOperation implements AgentAction {

	private int type;
	private Profile profile;
	
	public int getType() {
		return type;
	}
	
	public void setType(int type) {
		this.type = type;
	}
	
	public Profile getProfile() {
		return profile;
	}
	
	public void setProfile(Profile profile) {
		this.profile = profile;
	}
	
}
