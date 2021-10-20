package com.gmoutzou.musar.agent;

import com.gmoutzou.musar.utils.CallbackInterface;

import prof.onto.MakeOperation;

public interface ClientProfileInterface {
    public void registerCallbackInterface(CallbackInterface callbackInterface);
    public void requestProfileOperation(MakeOperation op);
}
