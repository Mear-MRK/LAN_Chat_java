# LANchat

LANchat is a Java-based GUI application designed for communication over a local area network (LAN). It leverages both TCP and UDP protocols for establishing connections and transmitting messages, files, and announcements between users on the same network.

#### Key Features

1.  **Peer Discovery:** Utilizes UDP multicast for announcing the presence of users on the network and discovering other active users.
2.  **Text Chat:** Enables real-time text communication between users.
3.  **File Transfer:** Supports the exchange of files between users.
4.  **Command Interface:** Includes a command-line interface for executing specific actions and retrieving status information.
5.  **Status Updates:** Allows users to set and broadcast their availability status (e.g., available, away, busy).

#### Project Structure

The project is organized into several packages and classes, each serving a specific purpose:

*   **lanchat:** Contains the main application logic, including the GUI and core functionality.
*   **lanchat.net:** Houses the networking components responsible for handling TCP and UDP connections, packet transmission, and server operations.

**Key Classes:**

*   **Connection:** Represents a connection between two peers, managing both text and binary data streams.
*   **DataPacket:** Encapsulates data for transmission over the network, including payload and addressing information.
*   **NetPacketListener:** Interface for classes that listen for incoming network packets.
*   **NetPacketSender:** Interface for classes that send network packets.
*   **UDP\_ListeningServer:** Implements `NetPacketListener` for receiving UDP packets.
*   **UDP\_SendServer:** Implements `NetPacketSender` for sending UDP packets.
*   **TCP\_ConnectionServer:** Manages TCP connections between peers.
*   **TCP\_Server:** Handles incoming TCP connections.
*   **AnnouncementServer:** Broadcasts announcements over UDP multicast to facilitate peer discovery.
*   **ChatApp:** Represents the chat application itself, storing information like UUID, version, and server address.
*   **ChatPeer:** Represents a user in the chat, including their user information and connection details.
*   **ChatUser:** Stores information about a user, such as their name and UUID.

#### Usage

1.  **Launch:** Run the `LANchat` class to start the application.
2.  **Enter Name:** Provide your desired username in the text field.
3.  **Discover Peers:** The application will automatically discover other active users on the LAN.
4.  **Select Recipient:** Choose a recipient from the available users list.
5.  **Chat:** Type your messages in the text input area and press Enter to send.
6.  **Send Files:** Click the "Open a file to send" button to select and send a file.
7.  **Commands:** Use the command interface (starting with '/') to perform actions like checking server status or terminating connections.

#### Dependencies

*   **JavaFX:** Used for creating the graphical user interface.

#### Future Enhancements

*   **Encryption:** Implement end-to-end encryption for secure communication.
*   **Group Chat:** Add support for group conversations.
*   **Audio/Video Chat:** Extend functionality to include audio and video communication.
*   **Improved GUI:** Enhance the user interface for a more intuitive experience.
