/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lanchat;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 *
 * @author Mear
 */
public class ChatUser implements Serializable {

    final static public int MAX_USERNAME_LEN = 16;
    final public UUID uuid;
    final public String name;
    //private byte[] privateKey
    //private byte[] publicKey
    //private String passphrase
    //private byte[] passwordHash
    //private byte[] uuidSign

    public ChatUser(final UUID uuid, final String name) {
        if (uuid == null) {
            throw new NullPointerException("Null uuid!");
        }
        this.uuid = uuid;
        this.name = setName(name);
    }
    
    public final ChatUser newName(String name) {
        return new ChatUser(uuid, name);
    }

    public ChatUser(final UUID uuid) {
        this(uuid, "");
    }

    private String setName(String name) {
        String outName;
        if (name != null && name.length() != 0) {
            outName = trimName(name);
            } else {
                outName = trimName(System.getProperty("user.name") + "_"
                        + ((long) uuid.hashCode() - Integer.MIN_VALUE));
            }
        return outName;
    }


    @Override
    public String toString() {
        return name;
    }
    
    public boolean deeplyEquals(ChatUser chU) {
        if (chU == null)
            return false;
        return uuid.equals(chU.uuid) && name.equals(chU.name);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        else if (obj instanceof ChatUser)
            return uuid.equals(((ChatUser) obj).uuid);
        else
            return false;
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }


    /*public final UUID getUUID() {
        return uuid;
    }*/

    public static String trimName(String inName) {
        if (inName != null) {
            inName = inName.replace(':', ' ');
            inName = inName.trim();
            inName = (inName.length() <= MAX_USERNAME_LEN)
                    ? inName : inName.substring(0, MAX_USERNAME_LEN);
            inName = inName.trim();
        } else {
            inName = "";
        }
        return inName;
    }

}
