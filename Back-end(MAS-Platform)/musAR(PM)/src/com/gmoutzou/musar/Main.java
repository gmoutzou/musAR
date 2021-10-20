package com.gmoutzou.musar;

public class Main {

	public static void main(String[] args) {
		String container = "-container";
		String defaultHost = "localhost";
		String defaultPort = "1099";
		String agent = "ProfileManager:com.gmoutzou.musar.ProfileManagerAgent";
		String[] args0 = new String[7];
		args0[0] = container;
		args0[1] = "-host";
		args0[2] = defaultHost;
		args0[3] = "-port";
		args0[4] = defaultPort;
		args0[5] = "-agents";
		args0[6] = agent;
		if (args.length == 2) {
			args0[2] = args[0];
			args0[4] = args[1];
		}
		jade.Boot.main(args0);
	}
	
}