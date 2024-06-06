/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lanchat.net;

import java.net.InetAddress;

/**
 *
 * @author Mear
 */
 public interface NetPacketListener {
     
     
         void addPacketObserver(PacketObserver pObs);
        
         void removePacketObserver(PacketObserver pObs);
         
         ReceiverStatus getStatus();

         int getListeningPort();

         InetAddress getBoundAddress();

         boolean isMulticastListener();
         
         void start();

         void terminate();

         
         
    enum ReceiverStatus {
        INITIALIZED, RECEIVING, LISTENING, DOWN, SOCKET_ISSUE, TERMINATED, UNKNOWN
    }

    }