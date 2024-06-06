/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lanchat;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import lanchat.net.DataPacket;
import lanchat.net.NetPacketListener;
import lanchat.net.NetPacketSender;
import lanchat.net.PacketObserver;
import lanchat.net.UDP_ListeningServer;
import lanchat.net.UDP_SendServer;
import lanchat.net.Utils;


/**
 *
 * @author Mear
 */
public class LANchat extends Application {

    final static int LANchat_VER = 5;
    final static UUID CHAT_APP_UUID = UUID.randomUUID();
    final static UUID CHAT_USER_UUID = UUID.randomUUID();
    final static UUID DEV_NULL_UUID = UUID.randomUUID();
    final static int UUID_STR_LENGTH = CHAT_APP_UUID.toString().length();
    final static int MAX_LENGTH_PER_MESSAGE = 1024;
    final static int MULTICAST_LISTENING_PORT = 43210;
    final static String MULTICAST_GROUP_STR = "237.232.169.9";
    final static String MSG_BOUND_ADDRESS = "0.0.0.0";
    final public static long AVE_DELAY_ANNOUNCE = 3000L; // 3sec
    final public static long DELTA_DELAY_ANNOUNCE = 2000L; // 2sec
    final static int MAX_NUM_CHAT_PEERS = 24;

    static NetPacketListener announcListeningServ;
    static NetPacketListener msgListeningServ;
    static NetPacketSender msgSenderServ;
    static byte[] announcementBytes;
    static AnnouncementServer announcementServer;

    //static String thisChatPeer.getUser().name = ""; //String.format("User_%04d", (int) (10000 * Math.random()));
    static ChatPeer thisChatPeer;
    static ChatPeer recipientChatPeer = null;
    
    final static Map<ChatPeer, Properties> CHAT_PEERS = new ConcurrentHashMap<>();
    final static ObservableList<ChatPeer> availableChatPeers = FXCollections.observableArrayList();

    static Label lblStatus = new Label();
    static TextArea chatDisplay = new TextArea();

    static PeriodicCheckItemsAlive<ChatPeer> checkClientsOnlineServ;
    static listObeserver<ChatPeer> peerListObserver;
    static ComboBox<ChatPeer> cbAvailableChUsers = new ComboBox<>(availableChatPeers);

    final ObservableList<ChatEntry> chatHistory = FXCollections.observableArrayList();

    static Stage prmStg;
    @Override
    public void start(Stage primaryStage) {
        prmStg = primaryStage;
        int prfCol = 24;
        int prfHi = 24;

        TextField txtfChatName = new TextField();
        TextArea txtInput = new TextArea();
        final FileChooser fileChooser = new FileChooser();
        final Button fileSendButton = new Button("Open a file to send");

        lblStatus.setText("Recipient Host/Address:Port N/A");

        txtfChatName.setPromptText("Enter your name here");
        txtfChatName.setPrefColumnCount(prfCol);
        txtfChatName.setText(thisChatPeer.getUser().name);

        cbAvailableChUsers.setPromptText("Select an available user to chat with");
        cbAvailableChUsers.setVisibleRowCount(5);

        chatDisplay.setPrefColumnCount(prfCol);
        chatDisplay.setPrefRowCount(prfHi);
        chatDisplay.setEditable(false);
        chatDisplay.setWrapText(true);

        txtInput.setPromptText("Enter your message here...");
        txtInput.setPrefColumnCount(prfCol);
        txtInput.setPrefRowCount(2);
        txtInput.setWrapText(true);

        txtfChatName.setOnAction((ActionEvent event) -> {
            String inName = txtfChatName.getText();
            thisChatPeer.setUser(thisChatPeer.getUser().newName(inName));
            primaryStage.setTitle("LAN chat - " + thisChatPeer.getUser().name);

            byte[] announcBytes;
            try {
                announcBytes = thisChatPeer.genAnnounceBytes();
                announcementServer.loadAnnouncementBytes(announcBytes);
                txtInput.requestFocus();
            } catch (IOException ex) {
                Logger.getLogger(LANchat.class.getName()).log(Level.SEVERE, null, ex);
                chatDisplay.appendText(ex.getMessage());
            }

            event.consume();
        }
        );

        cbAvailableChUsers.setOnAction((ActionEvent event) -> {
            ChatPeer selectedPeer = cbAvailableChUsers.getValue();
            if (selectedPeer != null && !selectedPeer.guessIsAlive()) {
                chatDisplay.appendText("== " + selectedPeer.getUser().name + " (apparently) is offline. ==");
                cbAvailableChUsers.setValue(recipientChatPeer);
            } else {
                recipientChatPeer = selectedPeer;
            }
            if (recipientChatPeer != null) {
                lblStatus.setText(recipientChatPeer.getApp().srvAddress + ":" + recipientChatPeer.getApp().srvPort);
                //connectWith(recipientChatPeer);
                
                txtInput.setDisable(false);
                txtInput.requestFocus();
            } else {
                lblStatus.setText("Recipient Host/Address:Port N/A");
                //txtInput.setDisable(true);
            }
            event.consume();
        }
        );

        PacketObserver msgObserver = new PacketObserver() {
            @Override
            public int processPacket(final DataPacket receivedDP) {
                byte[] payload = receivedDP.getPayload();
                //Logger.getLogger(LANchat.class.getName()).log(Level.INFO, "msg received from " + receivedDP.address
                //        + "\n Content: " + new String(payload) + "\n");
                try {
                    ChatEntry receivedEntry = (ChatEntry) Utils.convertFromBytes(payload);
                    /*System.out.println(
                            Thread.currentThread().getName() + "; LANchat observer: \n"
                            + "fromPeer.app: " + receivedEntry.fromPeer.getApp() + "\n"
                            + "toPeer.app: " + receivedEntry.toPeer.getApp() + "\n"
                            + "this app: " + thisChatPeer.getApp() + "\n"
                    );*/

                    if (receivedEntry != null
                            && receivedEntry.toPeer.getApp().signature.equals(thisChatPeer.getApp().signature)
                            && receivedEntry.toPeer.getApp().uuid.equals(thisChatPeer.getApp().uuid)
                            && !receivedEntry.fromPeer.getApp().uuid.equals(thisChatPeer.getApp().uuid)) {
                        Platform.runLater(new Runnable() {

                            @Override
                            public void run() {
                                chatHistory.add(receivedEntry);
                            }
                        });
                    }
                } catch (IOException | ClassNotFoundException ex) {
                    Logger.getLogger(LANchat.class.getName()).log(Level.SEVERE, null, ex);
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            chatHistory.add(new ChatEntry("Failed to get chat-entry: " + ex));
                        }
                    });

                }
                return receivedDP.length;
            }
        };

        msgListeningServ.addPacketObserver(msgObserver);

        PacketObserver announceObs = (final DataPacket receivedDP) -> {
            ChatPeer receivedPeer;
            try {
                receivedPeer = ChatPeer.getPeerFromAnnouncement(receivedDP.getPayload(), receivedDP.address);
            } catch (IOException | ClassNotFoundException ex) {
                Logger.getLogger(LANchat.class.getName()).log(Level.SEVERE, null, ex);
                receivedPeer = null;
            }
            if (receivedPeer != null && !receivedPeer.getApp().uuid.equals(thisChatPeer.getApp().uuid)
                    && Authentication.isAuthentic(receivedPeer.getApp().signature)) {
                final ChatPeer rcvdPeer = receivedPeer;
                Platform.runLater(() -> {
                    boolean inTheList = false;
                    boolean guiShouldNotice = false;
                    boolean nameChanged = false;
                    String prvName = "";
                    for (ChatPeer cc : availableChatPeers) {
                        if (cc.getUser().uuid.equals(rcvdPeer.getUser().uuid)
                                && cc.getApp().uuid.equals(rcvdPeer.getApp().uuid)) {
                            inTheList = true;
                            if (!rcvdPeer.getApp().srvAddress.equals(cc.getApp().srvAddress)) {
                                //cc.getApp().setSrvBoundAddress(cc.getApp().srvAddress);
                                guiShouldNotice = true;
                                //System.out.println(rcvdPeer.getApp().getMsgAddress());
                            }
                            if (rcvdPeer.getApp().srvPort != -1 && cc.getApp().srvPort != rcvdPeer.getApp().srvPort) {
                                //cc.getApp().setSrvPort(rcvdPeer.getApp().srvPort);
                                guiShouldNotice = true;
                                //System.out.println(rcvdPeer.getApp().getMsgPort());
                            }
                            if (rcvdPeer.getUser().name.length() != 0 && !rcvdPeer.getUser().name.equals(cc.getUser().name)) {
                                prvName = cc.getUser().name;
                                //cc.getUser().setName(rcvdPeer.getUser().name);
                                guiShouldNotice = true;
                                nameChanged = true;
                                //System.out.println(rcvdPeer.getUser().name);
                            }
                            cc.setApp(rcvdPeer.getApp());
                            cc.setUser(rcvdPeer.getUser());
                            cc.setLastTimeOnline();
                            break;
                        }
                    }
                    synchronized (availableChatPeers) {
                        if (!inTheList && availableChatPeers.size() < MAX_NUM_CHAT_PEERS) {
                            
                            availableChatPeers.add(rcvdPeer);
                            
                            //System.out.println("Added to list: " + Arrays.toString(availableChatPeers.toArray()));
                            guiShouldNotice = true;
                        }
                    }
                    if (guiShouldNotice) {
                        synchronized (availableChatPeers) {
                            availableChatPeers.sort(null);
                        }
                        //String announcement = rcvdPeer.getApp().uuid + "_" + rcvdPeer.getApp().signature + "_" + rcvdPeer.getApp().getMsgPort()
                        //        + "_" + rcvdPeer.getUser().uuid + "_" + rcvdPeer.getUser().name + "_" + rcvdPeer.getStatus();
                        String announcement = "";
                        if (!inTheList) {
                            announcement += rcvdPeer.getUser().name + " is online. ";
                        }
                        if (nameChanged) {
                            announcement += prvName + " -> " + rcvdPeer.getUser().name + ". ";
                        }
                        
                        chatHistory.add(new ChatEntry(announcement));
                        
                    }
                });
            }
            return receivedDP.length;
        };
        announcListeningServ.addPacketObserver(announceObs);

        final ArrayList<String> enteredChats = new ArrayList<>();
        final Int chIndx = new Int();
        final StringBuilder editEntr = new StringBuilder();
        txtInput.setOnKeyPressed((KeyEvent keyE) -> {
            switch (keyE.getCode()) {
                case ENTER:
                    if (keyE.isShiftDown()) {
                        txtInput.insertText(txtInput.getCaretPosition(), "\n");
                    } else {
                        enteredChats.add(ChatEntry.preProcess(txtInput.getText()));
                        chIndx.i = enteredChats.size();
                        synchronized (chatHistory) {
                            chatHistory.add(new ChatEntry(txtInput.getText(), recipientChatPeer));
                        }
                        txtInput.clear();
                    }
                    keyE.consume();
                    break;
                case P:
                    if (keyE.isControlDown()) {
                        if (chIndx.i == enteredChats.size()) {
                            editEntr.delete(0, editEntr.length());
                            editEntr.append(ChatEntry.preProcess(txtInput.getText()));
                        }
                        if (chIndx.i > 0) {
                            chIndx.i--;
                            txtInput.setText(enteredChats.get(chIndx.i));
                            txtInput.positionCaret(txtInput.getLength());
                            
                        }
                        keyE.consume();
                    }
                    break;
                case N:
                    if (keyE.isControlDown()) {
                        if (chIndx.i < enteredChats.size() - 1) {
                            chIndx.i++;
                            txtInput.setText(enteredChats.get(chIndx.i));
                            txtInput.positionCaret(txtInput.getLength());
                        } else if (chIndx.i == enteredChats.size() - 1) {
                            chIndx.i++;
                            txtInput.setText(editEntr.toString());
                            txtInput.positionCaret(txtInput.getLength());
                        }
                        keyE.consume();
                    }
                    break;
                default:
                    chIndx.i = enteredChats.size();
                    
            }
        });

        ChatEntryObserver entryObserver = new ChatEntryObserver();

        chatHistory.addListener(entryObserver);

        fileSendButton.setOnAction((ActionEvent event) -> {
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null && file.isFile() && file.canRead()) {
                synchronized (chatHistory) {
                    chatHistory.add(new ChatEntry(file, recipientChatPeer));
                }
            } else {
                synchronized (chatHistory) {
                    chatHistory.add(new ChatEntry("File problem!", thisChatPeer));
                }
            }
            event.consume();
        });

        VBox rootPane = new VBox();

        rootPane.getChildren()
                .addAll(txtfChatName, cbAvailableChUsers, new Separator(), lblStatus, new Separator(), chatDisplay, txtInput, fileSendButton);
        rootPane.setAlignment(Pos.CENTER);

        Scene scene = new Scene(rootPane);

        //txtInput.setDisable(true);
        cbAvailableChUsers.autosize();

        cbAvailableChUsers.requestFocus();

        primaryStage.setTitle("LAN chat - " + thisChatPeer.getUser().name);
        primaryStage.setScene(scene);

        primaryStage.setResizable(false);

        primaryStage.show();

        //System.out.println("In start(): " + Thread.currentThread());

        /*primaryStage.iconifiedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
             System.out.println("minimized: " + newValue);
             beep.play();

            }
        });*/
 /*
        synchronized (availableChatPeers) {
            ChatPeer nullPeer = new ChatPeer(
                    thisChatPeer.getApp(),
                    new ChatUser(DEV_NULL_UUID, "/dev/null"),
                    System.currentTimeMillis(),
                    Status.available
            );
            availableChatPeers.add(nullPeer);
        }
         */
    }

    @Override
    public void stop() {
        checkClientsOnlineServ.terminate();
        announcementServer.terminate();
        msgListeningServ.terminate();
        announcListeningServ.terminate();
        msgSenderServ.terminate();
    }

    /**
     * @param args the command line arguments
     * @throws java.net.UnknownHostException
     */
    public static void main(String[] args) throws UnknownHostException, IOException {

        InetAddress multicastGroup = InetAddress.getByName(MULTICAST_GROUP_STR);
        InetAddress msgListeningBoundAddress = InetAddress.getByName(MSG_BOUND_ADDRESS);

        announcListeningServ = new UDP_ListeningServer("Announcement Listening Server",
                MULTICAST_LISTENING_PORT,
                multicastGroup);
        announcListeningServ.start();

        msgListeningServ = new UDP_ListeningServer("Message Listening Server",
                0, msgListeningBoundAddress);
        msgListeningServ.start();

        msgSenderServ = new UDP_SendServer("Message Sending Server");
        msgSenderServ.start();

        thisChatPeer = new ChatPeer(
                new ChatApp(CHAT_APP_UUID, LANchat_VER, Authentication.signatureStr(),
                        msgListeningBoundAddress, msgListeningServ.getListeningPort()),
                new ChatUser(CHAT_USER_UUID));

        byte[] announcByts = thisChatPeer.genAnnounceBytes();
        announcementServer = new AnnouncementServer("Announcing Server",
                AVE_DELAY_ANNOUNCE, DELTA_DELAY_ANNOUNCE,
                0, multicastGroup, MULTICAST_LISTENING_PORT,
                announcByts, announcByts.length, msgSenderServ);
        announcementServer.start();

        peerListObserver = (final List<ChatPeer> fromList, final List<ChatPeer> removeList) -> {
            if (fromList == null || removeList == null) {
                throw new NullPointerException("FromList: " + fromList + " RemoveList: " + removeList);
            }
            Platform.runLater(() -> {
                for (ChatPeer rmChCli : removeList) {
                    if (rmChCli != null && cbAvailableChUsers.getValue() == rmChCli) {
                        cbAvailableChUsers.setValue(null);
                        recipientChatPeer = null;
                        lblStatus.setText("Recipient Host/Address:Port N/A");
                        chatDisplay.appendText("== " + rmChCli.getUser().name + " is (apparently) offline.\n");
                    }
                    if (rmChCli != null && fromList.contains(rmChCli)) {
                        //System.out.println("listObs: Removed peer: " + rmChCli.toString());
                        fromList.remove(rmChCli);
                    }
                }
            });
        };

        checkClientsOnlineServ = new PeriodicCheckItemsAlive<>(availableChatPeers, peerListObserver,
                AVE_DELAY_ANNOUNCE);
        Thread onlineCheckingServThrd = new Thread(checkClientsOnlineServ, "Check Clients Online Server Thread");
        onlineCheckingServThrd.start();

        launch(args);
    }

    static String commandInterpreter(String commandString) {
        String commandOutput = "";
        switch (commandString) {
            case "/stat msgserv":
                commandOutput = msgListeningServ
                        + "\n Status: " + msgListeningServ.getStatus();
                break;
            case "/stat multserv":
                commandOutput = announcListeningServ
                        + "\n Status: " + announcListeningServ.getStatus();
                break;
            case "/stat annserv":
                commandOutput = announcementServer
                        + "\n Status: " + announcementServer.getStatus();
                break;
            case "/stat chkcliserv":
                commandOutput = checkClientsOnlineServ
                        + "\n Status: " + checkClientsOnlineServ.getStatus();
                break;
            case "/stat msgsndserv":
                commandOutput = msgSenderServ
                        + "\n Status: " + msgSenderServ.getStatus();
                break;

            case "/term msgserv":
                msgListeningServ.terminate();
                commandOutput = msgListeningServ + ": " + msgListeningServ.getStatus();
                break;
            case "/term annserv":
                announcementServer.terminate();
                commandOutput = announcementServer + ": " + announcementServer.getStatus();
                break;
            case "/term all":
                checkClientsOnlineServ.terminate();
                announcementServer.terminate();
                msgListeningServ.terminate();
                announcListeningServ.terminate();
                msgSenderServ.terminate();
                break;

            case "/recip":
                commandOutput
                        = (recipientChatPeer != null)
                                ? recipientChatPeer.getUser().name + " @ "
                                + recipientChatPeer.getApp().srvAddress.getHostName() + ":"
                                + recipientChatPeer.getApp().srvPort + " UUID: "
                                + recipientChatPeer.getUser().uuid
                                : "N/A, null recipient!";
                break;

            case "/show netints":
                try {
                    commandOutput = Utils.showUpInterfacesStr();
                } catch (SocketException ex) {
                    commandOutput += ex.toString();
                }

                break;

            default:
                commandOutput = "Unknown command!";
                break;
        }

        return commandOutput;
    }

}

class Int {

    int i;

    Int(int i) {
        this.i = i;
    }

    Int() {
        i = 0;
    }
}
