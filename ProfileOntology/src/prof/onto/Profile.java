package prof.onto;

import jade.content.Concept;

public class Profile implements Concept {

	private String account;
	private String name;

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "{Profile: {profile-account:" + account + ", profile-name:" + name + "}}";
	}
}