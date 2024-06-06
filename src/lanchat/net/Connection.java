package lanchat.net;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import lanchat.LANchat;

/**
 *
 * @author Mear
 */
public class Connection {

    final public InetAddress address;
    final public int binPort;
    final public int txtPort;
    final public int connTimeout = (int) (LANchat.AVE_DELAY_ANNOUNCE + 2 * LANchat.DELTA_DELAY_ANNOUNCE);
    final private Socket binSocket;
    final private Socket txtSocket;
    final int BIN_SO_BUFSZ = 512*1024;
    final int MAX_LINE_LEN = 256;

    public boolean txtConnected() {
        return ! txtSocket.isClosed() && txtSocket.isConnected();
    }

    public boolean binConnected() {
        return !binSocket.isClosed() && binSocket.isConnected();
    }
    final private Reader txtReader;
    final private Writer txtWriter;
    final private InputStream binInpStrm;
    final private OutputStream binOutStrm;
    final public Object txtReadLock, txtWriteLock, binReadLock, binWriteLock;

    public Connection(InetAddress address, int textPort, int binaryPort) {
        if (address == null) {
            throw new NullPointerException("Address is null.");
        }
        if (1 > textPort && textPort > 65535) {
            throw new IllegalArgumentException("Illegal textPort: " + textPort);
        }
        if (0 > binaryPort && binaryPort> 65535) {
            throw new IllegalArgumentException("Illegal dataPort: " + binaryPort);
        }
        this.address = address;
        this.binPort = binaryPort;
        this.txtPort = textPort;

        boolean txtConRes = false;
        txtSocket = new Socket();
        Reader tmpReader = null;
        Writer tmpWriter = null;
        try {
            txtSocket.connect(new InetSocketAddress(address, textPort), connTimeout);
            txtSocket.setSoTimeout(0);
            txtSocket.setKeepAlive(true);
            tmpReader = new BufferedReader(
                    new InputStreamReader(txtSocket.getInputStream(), StandardCharsets.UTF_8)
            );
            tmpWriter = new BufferedWriter(
                    new OutputStreamWriter(txtSocket.getOutputStream(), StandardCharsets.UTF_8)
            );
            txtConRes = true;
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.WARNING, null, ex);
        }
        txtReader = tmpReader;
        txtWriter = tmpWriter;

        boolean datConRes = false;
        binSocket = new Socket();
        InputStream tmpInpStrm = null;
        OutputStream tmpOutStrm = null;
        if (binaryPort > 0) {
            try {
                binSocket.connect(new InetSocketAddress(address, binaryPort), connTimeout);
                binSocket.setSoTimeout(0);
                Logger.getLogger(Connection.class.getName()).log(Level.INFO,
                        "RCV BUF: {0}, SND BUF: {1}", 
                        new Object[]{binSocket.getReceiveBufferSize(), binSocket.getSendBufferSize()});
                binSocket.setReceiveBufferSize(BIN_SO_BUFSZ);
                binSocket.setSendBufferSize(BIN_SO_BUFSZ);                
                tmpInpStrm = new BufferedInputStream(
                        binSocket.getInputStream()
                );
                tmpOutStrm = new BufferedOutputStream(
                        binSocket.getOutputStream()
                );
                
                datConRes = true;
            } catch (IOException ex) {
                Logger.getLogger(Connection.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        binInpStrm = tmpInpStrm;
        binOutStrm = tmpOutStrm;

        txtReadLock = new Object();
        txtWriteLock = new Object();
        binReadLock = new Object();
        binWriteLock = new Object();
    }
    
    public Connection(Socket txtSock, Socket binSock) {
        
    }


    public Connection(InetAddress address, int port) {
        this(address, port, port);
    }
    
    
    /* public final void close() {
        if (binConnected()) {
            synchronized (dataLOCK) {
                try {
                    dataSocket.close();
                } catch (IOException ex) {
                    Logger.getLogger(Connection.class.getName()).log(Level.WARNING, null, ex);
                }
                txtConnected.set(false);
            }
        }
        if (txtConnected.get()) {
            synchronized (textLOCK) {
                try {
                    textSocket.close();
                } catch (IOException ex) {
                    Logger.getLogger(Connection.class.getName()).log(Level.WARNING, null, ex);
                }
                txtConnected.set(false);
            }
        }

    } */

    public final void terminate() {
        try {
            binSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.WARNING, null, ex);
        }

        try {
            txtSocket.close();
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.WARNING, null, ex);
        }

    }

    public String readLine() throws IOException {
        if (! txtConnected())
            return null;
        StringBuilder inpStrBl = new StringBuilder(256);
        int counter = 0;
        int ich;
        while ((ich = txtReader.read()) != -1 && counter < MAX_LINE_LEN) {
            char ch = (char) ich;
            if (ch == '\r') {
                ich = txtReader.read();
                if (ich == -1) {
                    inpStrBl.append(ch);
                    counter++;
                    break;
                } else if (ich == (int) '\n') {
                    break;
                } else {
                    inpStrBl.append(ch);
                    counter++;
                    if (counter < MAX_LINE_LEN) {
                        inpStrBl.append((char) ich);
                        counter++;
                    }
                }
            } else {
                inpStrBl.append(ch);
                counter++;
            }
        }
        return inpStrBl.toString();
    }

    public boolean writeText(String text) {
        if (! txtConnected())
            return false;
        try {
            txtWriter.write(text);
            txtWriter.flush();
            return true;
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.WARNING, null, ex);
        }
        return false;
    }
    
    public boolean writeLine(String text) {
        if (! txtConnected())
            return false;
        int indxNewLine = text.indexOf("\r\n");
        indxNewLine = (indxNewLine == -1) ? 
                text.length() : indxNewLine;
        indxNewLine = Math.min(indxNewLine, MAX_LINE_LEN);
        try {
            txtWriter.write(text, 0, indxNewLine);
            txtWriter.write("\r\n");
            return true;
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.WARNING, null, ex);
            return false;
        } 
        
    }
    
    public int readData(byte[] outBytes) {
        if (! binConnected())
            return -1;
        int numBytesRead = 0;
        try {
            numBytesRead = binInpStrm.read(outBytes);
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.WARNING, null, ex);
            return -1;
        }
        return numBytesRead;
    }
    
    public int readDate(byte[] outBytes, int offset, int length) {
        if (! binConnected())
            return -1;
        int numBytesRead = 0;
        try {
            numBytesRead = binInpStrm.read(outBytes, offset, length);
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.WARNING, null, ex);
            return -1;
        }
        return numBytesRead;
    }
    
    public int readInt() throws IOException {
        if (! binConnected()) 
            throw new IOException("Binary socket is not connected.");
        DataInputStream dis = new DataInputStream(binInpStrm);
        return dis.readInt();
    }
    
    public long readLong() throws IOException {
        if (! binConnected()) 
            throw new IOException("Binary socket is not connected.");
        DataInputStream dis = new DataInputStream(binInpStrm);
        return dis.readLong();
    }
    
    public boolean writeData(byte[] inpBytes) {
        if (! binConnected())
            return false;
        try {
            binOutStrm.write(inpBytes);
            binOutStrm.flush();
            return true;
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.WARNING, null, ex);
            return false;
        }
    }
    
    public boolean writeData(byte[] inpBytes, int offset, int length) {
        if (! binConnected())
            return false;
        try {
            binOutStrm.write(inpBytes, offset, length);
            binOutStrm.flush();
            return true;
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.WARNING, null, ex);
            return false;
        }
    }
    
    public boolean writeLong(long outLongNum) {
        if (! binConnected())
            return false;
        DataOutputStream dos = new DataOutputStream(binOutStrm);
        try {
            dos.writeLong(outLongNum);
            return true;
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.WARNING, null, ex);
            return false;
        }
    }
    
       
    public boolean writeInt(int outIntNum) {
        if (! binConnected())
            return false;
        DataOutputStream dos = new DataOutputStream(binOutStrm);
        try {
            dos.writeInt(outIntNum);
            return true;
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.WARNING, null, ex);
            return false;
        }
    } 
    
    public int getTxtPort() {
        return txtPort;
    }

    public int getBinPort() {
        return binPort;
    }

}
