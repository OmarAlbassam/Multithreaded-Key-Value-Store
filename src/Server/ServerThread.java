package Server;

import java.io.*;
import java.net.Socket;
import Logic.DataStore;

public class ServerThread implements Runnable {
    private Socket socket; // Client socket
    private Server Server; // Reference to the server
    private DataStore dataStore; // Reference to the data store

    public ServerThread(Socket socket, Server Server, DataStore dataStore) {
        this.socket = socket;
        this.Server = Server;
        this.dataStore = dataStore;
    }

    @Override
    public void run() {
        try {
            int clientNumber = Server.getClientNumber();
            System.out.println("Client " + clientNumber + " at " + socket.getInetAddress() + " has connected.");
            
            // Set up input and output streams for client communication
            BufferedReader in_socket = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out_socket = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            
            // Initial message to the client
            out_socket.println("Welcome to Omar Key Value Server.Server!");
            
            // Read and process multiple messages
            String message;
            while ((message = in_socket.readLine()) != null) {
                System.out.println("Client " + clientNumber + " says: " + message);
                
                // Check for exit command
                if (message.equalsIgnoreCase("EXIT")) {
                    out_socket.println("Goodbye!");
                    break;
                }
                
                // Process the message through Logic.DataStore and send result
                String result = dataStore.handler(message);
                out_socket.println(result);
            }
            
            // Close the connection
            socket.close();
            System.out.println("Client " + clientNumber + " " + socket.getInetAddress() + " has disconnected.");
       
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}