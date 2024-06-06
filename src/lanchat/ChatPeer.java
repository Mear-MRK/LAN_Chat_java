/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lanchat;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import lanchat.net.Utils;

/**
 *
 * @author Mear
 */
public class ChatPeer implements HasLife, Comparable<ChatPeer>, Serializable {

    //final transient private Object appLOCK, userLOCK, statLOCK;
    private ChatApp app;
    private ChatUser user;
    final private static AtomicLong aliveDelta
            = new AtomicLong(21 * (LANchat.AVE_DELAY_ANNOUNCE + LANchat.DELTA_DELAY_ANNOUNCE) / 10 + 30);
    final private AtomicLong lastTimeOnline = new AtomicLong(-1L);
    private Status status = Status.unknown;

    final public ChatApp getApp() {
        synchronized (this.app) {
            return app;
        }
    }

    final public void setApp(ChatApp app) {
        synchronized (this.app) {
            this.app = app;
        }
    }

    final public ChatUser getUser() {
        synchronized (this.user) {
            return user;
        }
    }

    final public void setUser(ChatUser user) {
        synchronized (this.user) {
            this.user = user;
        }
    }

    final public void setStatus(Status newStatus) {
        synchronized (this.status) {
            status = newStatus;
        }
    }

    final public Status getStatus() {
        synchronized (this.status) {
            return status;
        }
    }

    public ChatPeer(ChatApp app, ChatUser user, long lastTimeOnline, Status status) {
        this.app = app;
        this.user = user;
        setLastTimeOnline(lastTimeOnline);
        this.status = status;
    }

    public ChatPeer(ChatApp app, ChatUser user) {
        this(app, user, 0L, Status.available);
    }

    @Override
    public boolean guessIsAlive() {
        //System.out.println(lastTimeOnline.get() + " " + System.currentTimeMillis() + " " + aliveDelta);
        return (getStatus() != Status.offline)
                && ((System.currentTimeMillis() - lastTimeOnline.get()) < aliveDelta.get());
    }

    public static long getAliveDelta() {
        return aliveDelta.get();
    }

    public static void setAliveDelta(long aAliveDelta) {
        if (aAliveDelta > 0L) {
            aliveDelta.set(aAliveDelta);
        }
    }

    public final void setLastTimeOnline() {
        lastTimeOnline.set(System.currentTimeMillis());
    }

    public final void setLastTimeOnline(long time) {
        if (time > lastTimeOnline.get()) {
            lastTimeOnline.set(time);
        } else if (time == 0L) {
            lastTimeOnline.set(System.currentTimeMillis());
        }
    }

    public final long getLastTimeOnline() {
        return lastTimeOnline.get();
    }

    static public enum Status {
        unknown, offline, away, busy, available
    }

    @Override
    public String toString() {
        return this.user.toString() + " @ " + this.app.srvAddress.getHostName() + " - " + getStatus();
    }

    @Override
    public int compareTo(ChatPeer chPeer) {
        return (lastTimeOnline.get() > chPeer.getLastTimeOnline())
                ? //reverse of lastime
                -1 : ((lastTimeOnline.get() == chPeer.getLastTimeOnline()) ? 0 : 1);
    }

    public Properties getAnnoucement() {
        Properties announcement = new Properties();
        announcement.put("Signature", app.signature);
        announcement.put("Version", Integer.toString(app.version));
        announcement.put("AppUUID", app.uuid.toString());
        announcement.put("SrvPort", Integer.toString(app.srvPort));
        announcement.put("UserUUID", user.uuid.toString());
        announcement.put("UserName", user.name);
        announcement.put("PeerStatus", getStatus().toString());
        return announcement;
    }

    public byte[] genAnnounceBytes() throws IOException {
        return Utils.convertToBytes(getAnnoucement());
    }
    
    public static Properties getPeerPropsFromAnn(byte[] announcedBytes){        
        try {
            Object bytesObj = Utils.convertFromBytes(announcedBytes);            
            if (bytesObj instanceof Properties) {
                return (Properties) bytesObj;
            } else {
                return null;
            }
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(ChatPeer.class.getName()).log(Level.WARNING, null, ex);
            return null;
        }
    }
    static public ChatPeer getPeerFromAnnouncement(byte[] announcedBytes, InetAddress recievedAddress)
        {
        Properties announcement = getPeerPropsFromAnn(announcedBytes);
        if (announcement == null || !Authentication.isAuthentic(announcement)) {
            return null;
        }

        ChatApp annApp;
        ChatUser annUser;
        ChatPeer annPeer;


        annApp = new ChatApp(UUID.fromString(announcement.getProperty("AppUUID")),
                Integer.parseInt(announcement.getProperty("Version")),
                announcement.getProperty("Signature"),
                recievedAddress,
                Integer.parseInt(announcement.getProperty("SrvPort")));
        annUser = new ChatUser(UUID.fromString(announcement.getProperty("UserUUID")),
                announcement.getProperty("UserName"));

        annPeer = new ChatPeer(annApp, annUser, System.currentTimeMillis(),
                Status.valueOf(announcement.getProperty("PeerStatus")));

        return annPeer;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof ChatPeer) {
            ChatPeer chpObj = (ChatPeer) obj;
            return app.equals(chpObj.app) && user.equals(chpObj.user);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + this.app.hashCode();
        hash = 53 * hash + this.user.hashCode();
        return hash;
    }
}

/*class ChatPeerComparator implements Comparator<ChatPeer> {

    @Override
    public int compare(ChatPeer chPeer1, ChatPeer chPeer2) {
        return chPeer1.compareTo(chPeer2);
    }

}*/
