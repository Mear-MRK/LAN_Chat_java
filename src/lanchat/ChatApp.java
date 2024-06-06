/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lanchat;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.UUID;

/**
 *
 * @author Mear
 */
public class ChatApp implements Serializable {

    final public UUID uuid;
    final public int version;
    final public String signature;
    final public InetAddress srvAddress;
    final public int srvPort;

    public ChatApp(UUID uuid, int version, String signature,
            InetAddress srvAddress, int srvPort) {
        if (uuid == null) {
            throw new NullPointerException("Null uuid!");
        }
        if (signature == null) {
            throw new NullPointerException("Null signature!");
        }
        if (srvAddress == null)
            throw new NullPointerException("Server Address is null!");
        if (srvPort < 1 || srvPort > 65535) {
            throw new IllegalArgumentException("Illegal port: " + srvPort);
        }
        this.uuid = uuid;
        this.version = version;
        this.signature = signature;
        this.srvAddress = srvAddress;
        this.srvPort = srvPort;        

    }
    public final ChatApp newAddressPort(InetAddress srvAddress, int srvPort) {
        return new ChatApp(uuid, version, signature, srvAddress, srvPort);
    }
    
    boolean deeplyEquals(ChatApp chA) {
        if (chA == null)
            return false;
        return uuid.equals(chA.uuid) && version==chA.version && srvPort == chA.srvPort
                && signature.equals(chA.signature) && srvAddress.equals(chA.srvAddress);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        else if (obj instanceof ChatApp)
            return uuid.equals(((ChatApp)obj).uuid);
        else
            return false;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    
    @Override
    public String toString() {
        return "ChatApp: " + uuid.toString() +
                " ver: " + version +
                        " sig: " +  signature +
                        " srvAddr: "+ srvAddress + 
                " srvPort: " + srvPort; 
    }
}
