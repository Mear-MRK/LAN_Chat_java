/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lanchat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.media.AudioClip;
import static lanchat.LANchat.chatDisplay;
import static lanchat.LANchat.thisChatPeer;
import static lanchat.LANchat.msgSenderServ;
import static lanchat.LANchat.commandInterpreter;
import static lanchat.LANchat.prmStg;
import lanchat.net.DataPacket;
import lanchat.net.Utils;

/**
 *
 * @author Mear
 */
class ChatEntryObserver implements ListChangeListener<ChatEntry> {

    AudioClip beep = new AudioClip(getClass().getResource("/lanchat/R2D2-do.wav").toExternalForm());

    @Override
    public void onChanged(Change<? extends ChatEntry> change) {

        //System.out.println(Thread.currentThread());
        while (change.next()) {
            List<? extends ChatEntry> addedChatEntries = change.getAddedSubList();
            if (addedChatEntries != null || !addedChatEntries.isEmpty()) {
                for (ChatEntry ChEntry : addedChatEntries) {
                    String nowDateTimeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    String fromUserStr = (ChEntry.fromPeer.getApp().uuid.equals(thisChatPeer.getApp().uuid))
                            ? "YOU" : ChEntry.fromPeer.getUser().toString();
                    ChatEntry.EntryType type = ChEntry.getType();
                    String entryDisplayStr = "";
                    switch (type) {
                        case chat:
                            entryDisplayStr
                                    += fromUserStr + ", "
                                    + nowDateTimeStr
                                    + "\n"
                                    + ChEntry.enteredString
                                    + "\n";
                            if (ChEntry.toPeer != null
                                    && !ChEntry.toPeer.getApp().uuid.equals(thisChatPeer.getApp().uuid)) {
                                try {
                                    DataPacket msgDP = new DataPacket(
                                            ChEntry.toPeer.getApp().srvAddress,
                                            ChEntry.toPeer.getApp().srvPort,
                                            Utils.convertToBytes(ChEntry)
                                    );
                                    msgSenderServ.enQData(msgDP);
                                } catch (IOException ex) {
                                    Logger.getLogger(LANchat.class.getName()).log(Level.SEVERE, null, ex);
                                    entryDisplayStr += "== Failed to send the entry: " + ex + "\n";
                                }

                            }
                            break;
                        case file:
                            if (ChEntry.toPeer != null
                                    && !ChEntry.toPeer.getApp().uuid.equals(
                                            thisChatPeer.getApp().uuid)) {
                                entryDisplayStr
                                        += fromUserStr + ", "
                                        + nowDateTimeStr
                                        + "\n"
                                        + "== Request for sending : "
                                        + ChEntry.file.toString()
                                        + "\n";

                                /*
                                fileSender.enQ(file, recepientPeer, statObserver);
                                */
                            } else {
                                entryDisplayStr
                                        += fromUserStr + ", "
                                        + nowDateTimeStr
                                        + "\n"
                                        + "== ERROR: sending the file to WHOM?!\n"
                                        + ChEntry.file.toString()
                                        + "\n";
                            }
                            break;
                        case command:
                            entryDisplayStr
                                    += fromUserStr + ", "
                                    + nowDateTimeStr
                                    + " - command:\n"
                                    + ChEntry.enteredString
                                    + "\n";
                            if (ChEntry.fromPeer.getApp().uuid.equals(thisChatPeer.getApp().uuid)) {
                                entryDisplayStr
                                        += "== Output: "
                                        + commandInterpreter(ChEntry.enteredString)
                                        + "\n";
                                entryDisplayStr
                                        += "== Did NOTHING"
                                        + "\n";
                            }
                            break;
                        case announcement:
                            entryDisplayStr
                                    += "== "
                                    + nowDateTimeStr + " - "
                                    + ChEntry.enteredString
                                    + "\n";
                            break;
                        default:
                            break;
                    }
                    final String displayStr = entryDisplayStr;
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            chatDisplay.appendText(displayStr);
                            if (!prmStg.isFocused()) {
                                beep.play();
                            }
                        }
                    });
                }
            }
        }
    }

}
