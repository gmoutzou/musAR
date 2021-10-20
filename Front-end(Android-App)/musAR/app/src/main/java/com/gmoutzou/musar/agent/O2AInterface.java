package com.gmoutzou.musar.agent;

import com.gmoutzou.musar.utils.CallbackInterface;

import java.util.logging.Level;

import jade.core.MicroRuntime;
import jade.util.Logger;
import jade.wrapper.ControllerException;
import jade.wrapper.O2AException;
import jade.wrapper.StaleProxyException;
import prof.onto.MakeOperation;

public class O2AInterface {
	private Logger logger = Logger.getJADELogger(this.getClass().getName());
	private ClientProfileInterface clientProfileInterface;
	
	public O2AInterface(String agentname) {
		try {
			clientProfileInterface = MicroRuntime.getAgent(agentname)
					.getO2AInterface(ClientProfileInterface.class);
		} catch (StaleProxyException e) {
			logger.log(Level.SEVERE, "Error!" + e.getMessage());
		} catch (ControllerException e) {
			logger.log(Level.SEVERE, "Error!" + e.getMessage());
		}
	}

	public void registerCallbackInterface(CallbackInterface callbackInterface) {
		try {
			clientProfileInterface.registerCallbackInterface(callbackInterface);
		} catch (O2AException e) {
			logger.log(Level.WARNING, "Error!" + e.getMessage());
		}
	}
	
	public void requestProfileOperation(MakeOperation op) {
		try {
			clientProfileInterface.requestProfileOperation(op);
		} catch (O2AException e) {
			logger.log(Level.WARNING, "Error!" + e.getMessage());
		}
	}

}
