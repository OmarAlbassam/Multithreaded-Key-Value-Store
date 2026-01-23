import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private DataStore dataStore;

    public Server() throws IOException {

        // Initialize the data store
        dataStore = new DataStore();

        // Create a server socket listening on port 2020
        ServerSocket serverSocket = new ServerSocket(2020);
        System.out.println("Port 2020 is open");

        // Loop to continuously listen for new client connections
        while (true) {
            Socket socket = serverSocket.accept(); // Accept a new client connection
            
            // Create a new thread to handle the connected client
            ServerThread ServerThread = new ServerThread(socket, this, dataStore);
            Thread.ofVirtual().start(ServerThread);
        }
    }

    private int clientNumber = 1;

    // Increment and return the unique client number
    public int getClientNumber() {
        return clientNumber++;
    }
}