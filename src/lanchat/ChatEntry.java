/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lanchat;

import java.io.File;
import java.io.Serializable;

/**
 *
 * @author Mear
 */
public class ChatEntry implements Serializable {

    final static public int MAX_MESSAGE_LENGTH = 1024;
    final public String enteredString;
    final ChatPeer toPeer;
    final ChatPeer fromPeer;
    static private long nextIndex = 0L;
    final public long index;
    final public long timeEntered; //millisec from epoch (sender time)
    final public long timeRecieved;
    final public File file;

    ChatEntry(String enteredStr, File file, ChatPeer toPeer,
            ChatPeer fromPeer, long timeEntered, long timeRecieved) {
        long currentTime = System.currentTimeMillis();
        enteredString = (enteredStr != null) ? preProcess(enteredStr) : null;
        this.toPeer = toPeer;
        this.fromPeer = fromPeer;
        if (this.fromPeer == LANchat.thisChatPeer && this.toPeer != LANchat.thisChatPeer) {
            this.timeEntered = (timeEntered > currentTime || timeEntered == 0) ? currentTime : timeEntered;
            this.timeRecieved = -1L;
        } else if (this.toPeer == LANchat.thisChatPeer && this.fromPeer != LANchat.thisChatPeer) {
            this.timeEntered = (timeEntered > 0) ? timeEntered : -1L;
            this.timeRecieved = (timeRecieved > currentTime || timeRecieved == 0) ? currentTime : timeRecieved;
        } else {
            this.timeEntered = (timeEntered > 0) ? timeEntered
                    : (timeEntered == 0) ? currentTime : -1L;
            this.timeRecieved = (timeRecieved > 0) ? timeRecieved
                    : (timeRecieved == 0) ? currentTime : -1L;
        }
        index = nextIndex++;
        this.file = file;
    }

    ChatEntry(File file, ChatPeer toPeer) {
        this(null, file, toPeer, LANchat.thisChatPeer, 0, -1);
    }

    ChatEntry(String enteredStr, ChatPeer toPeer) {
        this(enteredStr, null, toPeer, LANchat.thisChatPeer, 0, -1);
    }

    public ChatEntry(String enteredStr) {
        this(enteredStr, null, LANchat.thisChatPeer, LANchat.thisChatPeer, 0, 0);
    }

    static public String preProcess(String enteredStr) {
        String processedStr;
        if (enteredStr.length() > MAX_MESSAGE_LENGTH) {
            processedStr = enteredStr.substring(0, MAX_MESSAGE_LENGTH);
        } else {
            processedStr = enteredStr;
        }
        return processedStr;
    }

    final public EntryType getType() {
        EntryType resultingType;
        if (enteredString == null && file == null ) {
            throw new NullPointerException("Input string and selected file are both null!");
        }

        if (file != null) {
            resultingType = EntryType.file;
        } else if (enteredString.charAt(0) == '/') {
            resultingType = EntryType.command;
        } else if (fromPeer != toPeer) {
            resultingType = EntryType.chat;
        } else if (fromPeer == toPeer) {
            resultingType = EntryType.announcement;
        } else {
            resultingType = EntryType.unknown;
        }
        return resultingType;
    }

    static public enum EntryType {
        unknown, chat, command, announcement, file
    }

}
