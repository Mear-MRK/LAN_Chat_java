/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lanchat.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mear
 */
public class UDP_SendServer implements NetPacketSender, Runnable {

    final private Object statusLOCK = new Object();
    final private Object queueLOCK = new Object();
    final private Thread serverThread;
    final String srvName;
    final AtomicBoolean terminate = new AtomicBoolean();
    final static private AtomicInteger nxtSerial = new AtomicInteger();
    final private int serial;
    final private int maxUdpPackSize = 16 * 1024;
    final private int qSize = 4;
    private final byte[] buffer = new byte[maxUdpPackSize];
    final private DataPacket[] packQue = new DataPacket[qSize];
    final private SentStatus[] sentStat = new SentStatus[qSize];
    private int nextIndex = 0;
    private int qPointer = 0;
    private int qEnd = 0;
    private boolean wait = true;
    private DatagramSocket udpSocket;
    private int udpLocalPort = -1;
    NetPacketSender.SenderStatus status = SenderStatus.UNKNOWN;
    final private AtomicBoolean isRunning = new AtomicBoolean();

    public UDP_SendServer(String name) {
        boolean constructionProblem = false;
        serial = nxtSerial.getAndIncrement();
        if (name != null && !name.isEmpty()) {
            srvName = name;
        } else {
            srvName = "UDP Send Server #" + serial;
        }
        for (int i = 0; i < qSize; i++) {
            sentStat[i] = SentStatus.NO_INFO;
        }
        try {
            udpSocket = new DatagramSocket();
            udpLocalPort = udpSocket.getLocalPort();
        } catch (SocketException se) {
            Logger.getLogger(UDP_SendServer.class.getName()).log(Level.SEVERE, null, se);
            status = SenderStatus.SOCKET_ISSUE;
            constructionProblem = true;
            terminate.set(true);
        }
        serverThread = new Thread(this, name);
        if (!constructionProblem) {
            status = SenderStatus.INITIALIZED;
        }
    }

    public UDP_SendServer() {
        this("");
    }

    private boolean send(int index) {
        index %= qSize;
        if (index < 0)
            return false;
        boolean sentSucceed = false;
        DataPacket dPack;
        synchronized (queueLOCK) {
            dPack = packQue[index];
        }
        if (dPack != null) {
            boolean serverReady = getStatus() == SenderStatus.IDLE;
            if (serverReady) {
                synchronized (queueLOCK) {
                    sentStat[index] = SentStatus.SENDING;
                }
                int numPacks;
                DatagramPacket dgPacket;
                byte[] data = dPack.getPayload();
                InetAddress destAddress = dPack.address;
                int destPort = dPack.port;
                int size = dPack.length;
                dgPacket = new DatagramPacket(buffer, Math.min(size, maxUdpPackSize),
                        destAddress, destPort);
                numPacks = (int) Math.ceil((double) size / maxUdpPackSize);
                if (size == 0) {
                    numPacks = 1;
                }
                int remSize = size;
                sentSucceed = true;
                setStatus(SenderStatus.SENDING);
                for (int i = 0; i < numPacks; i++) {
                    int pckSize = Math.min(remSize, maxUdpPackSize);
                    int offset = i * maxUdpPackSize;
                    if (data != null) {
                        System.arraycopy(data, offset, buffer, 0, pckSize);
                    }
                    dgPacket.setLength(pckSize);
                    try {
                        udpSocket.send(dgPacket);
                        remSize -= pckSize;
                    } catch (IOException ex) {
                        Logger.getLogger(UDP_SendServer.class.getName()).log(Level.WARNING, null, ex);

                        if (getStatus() != SenderStatus.TERMINATED) {
                            setStatus(SenderStatus.SOCKET_ISSUE);

                        }
                        synchronized (queueLOCK) {
                            sentStat[index] = SentStatus.FAILED;
                        }
                        sentSucceed = false;
                        break;
                    }
                }
            }
        }
        if (sentSucceed) {
            setStatus(SenderStatus.IDLE);
            synchronized (queueLOCK) {
                sentStat[index] = SentStatus.SUCCESSFUL;
            }
        }
        return sentSucceed;
    }

    @Override
    public int enQData(DataPacket dataPack) {
        if (dataPack == null) {
            return -1;

        }        
        if (getStatus() != SenderStatus.IDLE
                && getStatus() != SenderStatus.SENDING) {
            Logger.getLogger(UDP_SendServer.class.getName())
                    .log(Level.WARNING, "SenderStatus: " + getStatus());
            return -2;
        }
        synchronized (queueLOCK) {
            qEnd = nextIndex % qSize;
            packQue[qEnd] = dataPack;
            sentStat[qEnd] = SentStatus.WAITING;
            wait = false;
            queueLOCK.notifyAll();
        }
        /* System.out.println(srvName + "; enqued, indx# " + nextIndex + ": " +
                dataPack); */
        return nextIndex++;

    }

    @Override
    public void removeData(int index) {
        if (index >= 0 && index + qSize >= nextIndex && index < nextIndex) {
            index %= qSize;
            synchronized (queueLOCK) {
                packQue[index] = null;
                if (sentStat[index] == SentStatus.WAITING) {
                    sentStat[index] = SentStatus.REMOVED;
                }
            }

        }
    }

    private void setStatus(SenderStatus status) {
        synchronized (statusLOCK) {
            this.status = status;
        }
    }

    @Override
    public SenderStatus getStatus() {
        synchronized (statusLOCK) {
            return status;
        }
    }

    @Override
    public SentStatus sentStatus(int index) {
        SentStatus stat = SentStatus.NO_INFO;
        if (index >= 0 && index + qSize >= nextIndex && index < nextIndex) {
            synchronized (queueLOCK) {
                stat = sentStat[index % qSize];
            }
        }

        return stat;
    }

    @Override
    public void run() {
        if (!Thread.currentThread().equals(serverThread)) {
            throw new IllegalAccessError("run() should not be called directly. Instead call start().");
        }
        isRunning.set(true);
        //System.out.println(" is running");
        while (!terminate.get()) {
            try {
                synchronized (queueLOCK) {
                    while (wait) {
                        //System.out.println(": waiting; counter: " + counter);
                        queueLOCK.wait();
                    }
                }
            } catch (InterruptedException ex) {
                //System.out.println(
                //       srvName + ": interrupted waiting; counter: " + counter, ex);
            }
            boolean shouldSend;
            while (true) {
                synchronized (queueLOCK) {
                    if (qPointer == qEnd) {
                        break;
                    }
                    shouldSend = sentStat[qPointer] == SentStatus.WAITING;
                    /* System.out.println(srvName + "; " +
                            qPointer +"/"+ qEnd + " shouldSend: " + shouldSend); */
                }
                if (shouldSend) {
                    boolean suc = send(qPointer);
                    /* System.out.println(srvName + "; " +
                            qPointer +"/"+ qEnd + "; " + packQue[qPointer]                           
                            +"; sent success: " + suc); */
                }
                qPointer = ++qPointer % qSize;
            }
            wait = true;
        }
        isRunning.set(false);
        //System.out.println(srvName + " is done");
    }

    @Override
    public void terminate() {

        terminate.set(true);
        setStatus(SenderStatus.TERMINATED);

        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }

        synchronized (queueLOCK) {
            wait = false;
        }
        serverThread.interrupt();

    }

    @Override
    public String toString() {
        return srvName + ": " + getStatus();
    }

    @Override
    public void start() {
        if (!isRunning.get()) {
            if (getStatus() == SenderStatus.INITIALIZED
                    || getStatus() == SenderStatus.DOWN) {
                setStatus(SenderStatus.IDLE);
                serverThread.start();
            }
        }
    }
}
