package lanchat.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;

public class Utils {

    public static String showUpInterfacesStr() throws SocketException {
        String outputStr = "";
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(nets)) {
            if (!netint.isUp() || netint.isLoopback()) {
                continue;
            }
            outputStr += interfaceInformationStr(netint);
        }
        return outputStr;
    }

    public static String showInterfacesStr() throws SocketException {
        String outputStr = "";
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(nets)) {
            outputStr += interfaceInformationStr(netint);
        }
        return outputStr;
    }

    public static String interfaceInformationStr(NetworkInterface netint) throws SocketException {
        String outputStr = "";
        outputStr += "Display name: " + netint.getDisplayName() + "\n"
                + "Name: " + netint.getName() + "\n";
        Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
            outputStr += "InetAddress: " + inetAddress + "\n";
        }
        return outputStr;
    }

    /**
     * Returns a free localPort number on localhost.
     *
     *
     * @return a free localPort number on localhost
     * @throws IllegalStateException if unable to find a free localPort
     *
     */
    private static int findFreeTCPport() {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            socket.setReuseAddress(true);
            int port = socket.getLocalPort();
            try {
                socket.close();
            } catch (IOException e) {
            }
            return port;
        } catch (IOException e) {
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
        throw new IllegalStateException("Could not find a free TCP/IP port");
    }

    private static boolean isUDPportAvailable(int port) {

        try (DatagramSocket dgSocket = new DatagramSocket(port)) {
            dgSocket.setReuseAddress(true);
            return true;
        } catch (IOException ignored) {
        }
        return false;
    }

    static public byte[] convertToBytes(Object object) throws IOException {
        if (object == null) {
            return null;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bos);
        out.writeObject(object);
        out.flush();
        return bos.toByteArray();
    }

    static public Object convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        if (bytes == null) {
            return null;
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput in = new ObjectInputStream(bis);
        return in.readObject();

    }

}
