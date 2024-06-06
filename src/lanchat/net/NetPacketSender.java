/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lanchat.net;

/**
 *
 * @author Mear
 */
public interface NetPacketSender {

    /**
     *
     * @param dataPack
     * @return index of the data in the queue
     */
    int enQData(DataPacket dataPack);

    SentStatus sentStatus(int index);

    void removeData(int index);

    SenderStatus getStatus();
    
    void start();
    
    void terminate();

    enum SenderStatus {
        INITIALIZED, SENDING, IDLE, DOWN, TERMINATED, SOCKET_ISSUE, UNKNOWN
    }
    
    enum SentStatus {
        SUCCESSFUL, FAILED, WAITING, SENDING, REMOVED, NO_INFO
    }

}
