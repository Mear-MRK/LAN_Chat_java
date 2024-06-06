/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lanchat.net;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author Mear
 */
public final class DataPacket {

    final public static int MAX_PACK_SIZE = 60 * 1024;
    final private static AtomicLong nextSerial = new AtomicLong();
    final public InetAddress address;
    final public int port;
    final private byte[] payload;
    final public int length;
    final public long serial;

    public DataPacket(InetAddress address, int port, final byte[] inpData, int size) {
        if (1 > port && port > 65535) {
            throw new IllegalArgumentException("Illegal port: " + port);
        }        
        if (size < 0) {
            throw new IllegalArgumentException("Negative size: " + size);
        }
        if (address == null)
            throw new NullPointerException("Address is null.");
        
        this.serial = nextSerial.getAndIncrement(); 
        this.address = address;
        this.port = port;
        
        if (inpData == null) {
            size = 0;
        } else if (size == 0) {
            size = inpData.length;
        } else 
            size = Math.min(inpData.length, size);
        
        length = Math.min(size, MAX_PACK_SIZE);
        
        if (inpData != null) {
            payload = new byte[length];
            System.arraycopy(inpData, 0, payload, 0, length);
        } else                
            payload = null;
            
    }

    public DataPacket(InetAddress address, int port, final byte[] inpData) {
        this(address, port, inpData, 0);
    }
    
    
    public long getSerial() {
        return serial;
    }
    
    public byte getPayloadByteAt(int index) {
        return payload[index];
    }
    
    public byte[] getPayload() {
        byte[] tmpArr = new byte[length];
        System.arraycopy(payload, 0, tmpArr, 0, length);
        return tmpArr;
    }
    
    public static long nextSerial(){
        return nextSerial.get();
    }
    
    @Override
    public String toString() {
        return "DataPacket #" + serial + ", address: " + address.toString()
                + ", port: " + port + ", length: " + length;
    }
}
