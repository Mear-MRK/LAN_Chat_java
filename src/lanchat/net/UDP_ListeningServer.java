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
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mear
 */
public class UDP_ListeningServer implements NetPacketListener, Runnable {

    final private String srvName;
    final private Object statusLOCK = new Object();
    final private Thread serverThread;
    static private AtomicLong nextSerial = new AtomicLong();
    final private long serial;
    private ReceiverStatus status = ReceiverStatus.UNKNOWN;
    final private AtomicBoolean isRunning = new AtomicBoolean();
    private DatagramSocket dgSocket = null;
    private MulticastSocket mcSocket = null;
    final private int buffSize = 4096;
    final private byte[] buffer = new byte[buffSize];
    final private AtomicBoolean terminated = new AtomicBoolean();
    private int boundPort = -1;
    final private List<PacketObserver> packetObservers;
    final private ReadWriteLock pkObsLock = new ReentrantReadWriteLock();
    final private AtomicLong index = new AtomicLong();
    final private InetAddress boundAddress;
    final private boolean multicastListener;
    final private ExecutorService packetProcessService = Executors.newSingleThreadExecutor();

    public UDP_ListeningServer(String serverName, int boundPort, final InetAddress boundAddress,
            final PacketObserver packObs) throws UnknownHostException {
        this(serverName, boundPort, boundAddress);
        if (packObs == null) {
            throw new NullPointerException("Invalid packet observer");
        }
        packetObservers.add(packObs);
    }

    public UDP_ListeningServer(String serverName, int boundPort, InetAddress boundAddress)
            throws UnknownHostException {

        serial = nextSerial.getAndIncrement();
        if (serverName == null || serverName.isEmpty())
            srvName = "UDP Listening Server #" + serial;
        else 
            srvName = serverName;

        if (boundAddress != null) {
            this.boundAddress = boundAddress;
        } else {
            this.boundAddress = InetAddress.getByName("0.0.0.0");
        }
        setBoundPort(boundPort);
        multicastListener = this.boundAddress.isMulticastAddress();
        setupUDPsocket(this.boundPort, this.boundAddress);
        packetObservers = new ArrayList<>();

        serverThread = new Thread(this, srvName);
        if (getStatus()!=ReceiverStatus.SOCKET_ISSUE)
            setStatus(ReceiverStatus.INITIALIZED);
    }
    
    public UDP_ListeningServer(int boundPort, InetAddress boundAddress) throws UnknownHostException {
        this(null, boundPort, boundAddress);
    }

    @Override
    public void addPacketObserver(PacketObserver pOb) {
        pkObsLock.writeLock().lock();
        try {
            packetObservers.add(pOb);
        } finally {
            pkObsLock.writeLock().unlock();
        }
    }

    private void setStatus(ReceiverStatus servStatus) {
        synchronized (statusLOCK) {
            status = servStatus;
        }
    }

    @Override
    public void run() {
        if (!Thread.currentThread().equals(serverThread)) {
            throw new IllegalAccessError("run() should not be called directly."
                    + " Instead call start().");
        }
        isRunning.set(true);
        //Logger.getLogger(UDP_ListeningServer.class.getName()).log(Level.INFO, 
          //      srvName + " is running");
        if (getStatus() != ReceiverStatus.SOCKET_ISSUE) {
            DatagramSocket socket = (multicastListener) ? mcSocket : dgSocket;
            DatagramPacket dgPacket = new DatagramPacket(buffer, buffer.length);
            
            setStatus(ReceiverStatus.LISTENING);
            //Logger.getLogger(UDP_ListeningServer.class.getName()).log(Level.INFO, 
               //     srvName + " boundaddr: " + this.boundAddress + " listening port: " + this.boundPort);
            while (!terminated.get() && !socket.isClosed() && socket.isBound()) {
                try {

                    socket.receive(dgPacket);

                    DataPacket receivedDataPacket
                            = new DataPacket(dgPacket.getAddress(), dgPacket.getPort(),
                                    buffer, dgPacket.getLength());
                    /*System.out.println(
                            srvName + "; Status: " + getStatus() +
                                    " indx #" + index + " rcvdpack: " 
                                    + receivedDataPacket
                    );*/
                    
                    if (!packetProcessService.isShutdown()) {
                        packetProcessService.submit(new Runnable() {

                            @Override
                            public void run() {
                                pkObsLock.readLock().lock();
                                try {
                                    for (PacketObserver pOb : packetObservers) {
                                        /* System.out.println(srvName + "; dpack serial #" 
                                                + receivedDataPacket.serial + " given for process");*/
                                        pOb.processPacket(receivedDataPacket);
                                    }
                                } finally {
                                    pkObsLock.readLock().unlock();
                                }

                            }
                        });
                    }

                } catch (IOException ex) {
                    //ex.printStackTrace();
                    if (terminated.get()) {
                        setStatus(ReceiverStatus.TERMINATED);
                    } else {
                        setStatus(ReceiverStatus.SOCKET_ISSUE);
                        Logger.getLogger(UDP_ListeningServer.class.getName()).log(Level.SEVERE,
                                "Failed to receive packet indx#" + index.get(), ex);
                    }
                }
                index.incrementAndGet();
                //System.out.println("Receiver_" + this.serial + " end of loop");
            }
            if (multicastListener && mcSocket != null && !mcSocket.isClosed()) {
                try {
                    mcSocket.leaveGroup(boundAddress);
                } catch (IOException ex) {
                    //ex.printStackTrace();
                    Logger.getLogger(UDP_ListeningServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        packetProcessService.shutdown();
        setStatus(ReceiverStatus.DOWN);
        isRunning.set(false);
        }
        //Logger.getLogger(UDP_ListeningServer.class.getName()).log(Level.INFO, srvName + " is done");
    }

    private int setupUDPsocket(int port, InetAddress boundAddress) {
        if (!multicastListener) {
            try {
                dgSocket = new DatagramSocket(port, boundAddress);
                this.boundPort = dgSocket.getLocalPort();
            } catch (SocketException se) {
                setStatus(ReceiverStatus.SOCKET_ISSUE);
                Logger.getLogger(UDP_ListeningServer.class.getName()).log(Level.SEVERE, 
                        srvName + ": Socket listening failed on #"
                        + port + " bindig to " + boundAddress, se);
                dgSocket = null;
                this.boundPort = -1;
            }
        } else {
            try {
                mcSocket = new MulticastSocket(port);
                mcSocket.joinGroup(boundAddress);
                mcSocket.setReuseAddress(true);
                this.boundPort = mcSocket.getLocalPort();
            } catch (IOException ex) {
                //ex.printStackTrace();
                Logger.getLogger(UDP_ListeningServer.class.getName()).log(Level.SEVERE, 
                        srvName + ": Multicast-listening failed on #"
                        + port + " group of " + boundAddress, ex);
                setStatus(ReceiverStatus.SOCKET_ISSUE);
                mcSocket = null;
                this.boundPort = -1;
            }
        }
        return this.boundPort;
    }

    private int setupUDPsocket(int port) {
        int resultingPort;
        try {
            resultingPort = setupUDPsocket(port, InetAddress.getByName("0.0.0.0"));
        } catch (UnknownHostException he) {
            //he.printStackTrace();
            Logger.getLogger(UDP_ListeningServer.class.getName()).log(Level.SEVERE, null,
                    "Listening on global failed! exc: " + he);
            setStatus(ReceiverStatus.SOCKET_ISSUE);
            dgSocket = null;
            mcSocket = null;
            resultingPort = this.boundPort = -1;
        }
        return resultingPort;
    }

    private int setupUDPsocket() {
        return setupUDPsocket(0);
    }

    @Override
    final public void terminate() {
        terminated.set(true);
        setStatus(ReceiverStatus.TERMINATED);
        if (multicastListener && mcSocket != null && !mcSocket.isClosed()) {
            try {
                mcSocket.leaveGroup(boundAddress);
            } catch (IOException ex) {
                //ex.printStackTrace();
                Logger.getLogger(UDP_ListeningServer.class.getName()).log(Level.WARNING, null, ex);
            }
            mcSocket.close();

        }
        if (dgSocket != null && !dgSocket.isClosed()) {
            dgSocket.close();
        }
        try {
            packetProcessService.shutdown();
            packetProcessService.awaitTermination(300, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Logger.getLogger(UDP_ListeningServer.class.getName()).log(Level.WARNING, null, ex);
        } finally {
            packetProcessService.shutdownNow();
        }
        setStatus(ReceiverStatus.DOWN);
    }

    final public long getSerial() {
        return serial;
    }

    @Override
    final public InetAddress getBoundAddress() {
            return boundAddress;
    }

    private int setBoundPort(int port) {
        this.boundPort = (0 < port && port < 65536) ? port : 0;
        return this.boundPort;
    }

    @Override
    final public int getListeningPort() {
        return boundPort;
    }

    @Override
    public String toString() {
        return srvName + "; status: " + getStatus();
    }

    @Override
    final public boolean isMulticastListener() {
        return multicastListener;
    }

    public long getLastReceivedIndex() {
        return index.get();
    }

    @Override
    public void removePacketObserver(PacketObserver pObs) {
        pkObsLock.writeLock().lock();
        try {
            packetObservers.remove(pObs);
        } finally {
            pkObsLock.writeLock().unlock();
        }
    }

    @Override
    public void start() {
        if (! isRunning.get())
        serverThread.start();
    }

    @Override
    public ReceiverStatus getStatus() {
        synchronized (statusLOCK) {
            return status;
        }
    }
}
