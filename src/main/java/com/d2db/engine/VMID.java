package com.d2db.engine;

import java.net.InetAddress;

public class VMID {
    
    public static String resolveMachineIdentity() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (java.net.UnknownHostException e) {
            return "UNKNOWN_VM_" + System.currentTimeMillis();
        }
    }
}
