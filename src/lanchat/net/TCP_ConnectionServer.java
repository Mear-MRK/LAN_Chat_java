/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lanchat.net;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import lanchat.ChatApp;
import lanchat.ChatPeer;

/**
 *
 * @author Mear
 */
public class TCP_ConnectionServer implements NetStreamConnector, Runnable {

    final private String srvName;
    final static AtomicInteger nextSerial = new AtomicInteger();
    final int serial;
    final private Map<ChatPeer, Connection> peerConnections = new HashMap<>();

    final private ExecutorService transPool = Executors.newFixedThreadPool(2);
    final private Thread srvThread;

    public TCP_ConnectionServer(String serverName) {
        serial = nextSerial.getAndIncrement();
        if (serverName == null || serverName.isEmpty()) {
            srvName = "TCP Conn Server #" + serial;
        } else {
            srvName = serverName;
        }

        this.srvThread = new Thread(this, serverName);
    }

    public boolean connectWith(ChatPeer peer) {
        if (isConnectedTo(peer)) {
            return true;
        } else {
            ChatApp peerApp = peer.getApp();
            Connection peerConn = new Connection(peerApp.srvAddress, peerApp.srvPort);
            if (peerConn.txtConnected()) {
                peerConnections.put(peer, peerConn);
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean isConnectedTo(ChatPeer peer) {
        Connection peerConn = peerConnections.get(peer);
        if (peerConn == null) {
            return false;
        }
        if (!peerConn.txtConnected()) {
            peerConnections.remove(peer);
            return false;
        }
        return true;
    }

    public boolean isBinAvailible(ChatPeer peer) {
        Connection peerConn = peerConnections.get(peer);
        return peerConn != null && peerConn.binConnected();
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}

enum ContentType {
    TEXT, FILE
}
