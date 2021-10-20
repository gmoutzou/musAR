package prof.onto;

import jade.content.AgentAction;

public class MakeOperation implements AgentAction {

	private int type;
	private String account;
	private String userChoice;
	
	public int getType() {
		return type;
	}
	
	public void setType(int type) {
		this.type = type;
	}
	
	public String getAccount() {
		return account;
	}
	
	public void setAccount(String account) {
		this.account = account;
	}
	
	public String getUserChoice() {
		return userChoice;
	}
	
	public void setUserChoice(String userChoice) {
		this.userChoice = userChoice;
	}
	
}
