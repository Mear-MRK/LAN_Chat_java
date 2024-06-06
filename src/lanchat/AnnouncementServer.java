/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lanchat;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import lanchat.net.DataPacket;
import lanchat.net.NetPacketSender;

/**
 *
 * @author Mear
 */
public class AnnouncementServer implements Runnable {

    final public static int MAX_ANNOUNCE_BYTES_LEN = 4 * 1024;
    final private Object payloadLOCK = new Object();
    final private String srvName;
    final private byte[] announcementBytes = new byte[MAX_ANNOUNCE_BYTES_LEN];
    private int annBytesSize = 0;
    final private AtomicLong averageWait = new AtomicLong();
    final private AtomicLong deltaWait = new AtomicLong();
    final private AtomicLong numRepeating = new AtomicLong();
    final private AtomicBoolean infiniteLoop = new AtomicBoolean();
    final private AtomicBoolean running = new AtomicBoolean();
    final private NetPacketSender UDP_Sender;
    final private InetAddress groupAddress;
    final private int annPort;
    final private Thread srvThread;
    final private AtomicBoolean terminated = new AtomicBoolean();
    final private AtomicLong numTrys = new AtomicLong();

    AnnouncementServer(String serverName, long averageWait, long deltaWait, long numRepeating,
            InetAddress groupAddress, int annPort, byte[] payload,
            int size, NetPacketSender UDPacketSender) {
        if (UDPacketSender == null) {
            throw new NullPointerException("UDP Sender Server is null!");
        }
        if (groupAddress == null) {
            throw new NullPointerException("Announcement group address is null!");
        }
        if (annPort < 1 || annPort > 65535) {
            throw new IllegalArgumentException("Illegal port number!");
        }
        if (serverName == null || serverName.isEmpty())
            srvName = "Announcement server";
        else
            srvName = serverName;
        this.groupAddress = groupAddress;
        this.annPort = annPort;
        UDP_Sender = UDPacketSender;
        setAverageDeltaWait(averageWait, deltaWait);
        setNumRepeating(numRepeating);
        loadAnnouncementBytes(payload, size);
        srvThread = new Thread(this, srvName + " thread");
    }

    void start() {
        if (!running.get()) {
            if (UDP_Sender.getStatus() == NetPacketSender.SenderStatus.DOWN) {
                UDP_Sender.start();
            }
            srvThread.start();
        }
    }

    void terminate() {
        if (running.get()) {
            terminated.set(true);
            srvThread.interrupt();
        }
    }

    @Override
    public void run() {
        //Logger.getLogger(AnnouncementServer.class.getName()).log(Level.INFO, srvName + " is running");
        running.set(true);
        for (long i = 0L; (i < numRepeating.get() || infiniteLoop.get())
                && !terminated.get(); i++) {

            long waitTime = (long) ((2 * Math.random() - 1) * deltaWait.get()
                    + averageWait.get());
            long index = UDP_Sender.enQData(
                    new DataPacket(groupAddress, annPort, announcementBytes));
            numTrys.incrementAndGet();
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException ex) {
            }
        }
        running.set(false);
        //Logger.getLogger(AnnouncementServer.class.getName()).log(Level.INFO, srvName + " is done");
    }

    public final void loadAnnouncementBytes(final byte[] payload, int size) {
        if (payload == null) {
            throw new NullPointerException("Announcement payload is null!");
        }
        if (size < 0) {
            throw new IllegalArgumentException("Size of the payload is negative!");
        } else if (size == 0) {
            size = payload.length;
        } else {
            size = Math.min(size, payload.length);
        }

        synchronized (payloadLOCK) {
            annBytesSize = Math.min(MAX_ANNOUNCE_BYTES_LEN, size);
            System.arraycopy(payload, 0, announcementBytes, 0, annBytesSize);
        }
    }

    public final void loadAnnouncementBytes(final byte[] payload) {
        loadAnnouncementBytes(payload, 0);
    }

    public final void setAverageDeltaWait(long averageWait, long deltaWait) {
        if (averageWait > 0) {
            this.averageWait.set(averageWait);
        }
        if (deltaWait > 0 && deltaWait < this.averageWait.get()) {
            this.deltaWait.set(deltaWait);
        }
    }

    long getAverageWait() {
        return averageWait.get();
    }

    long getDeltaWait() {
        return deltaWait.get();
    }

    public final void setNumRepeating(long numRepeating) {
        this.numRepeating.set((numRepeating > 0) ? numRepeating : 0);
        infiniteLoop.set((numRepeating == 0));
    }

    long getNumRepeating() {
        return numRepeating.get();
    }

    public boolean isRunning() {
        return running.get();
    }

    public String getStatus() {
        return "isRunning: " + running.get();
    }

    @Override
    public String toString() {
        return srvName;
    }

}
