package prof.onto;

import jade.content.Concept;

public class Status implements Concept {

	private int code;
	private String message;

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "{Status: {code:" + code + ", message:" + message + "}}";
	}
	
}
