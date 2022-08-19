package tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import clientHandler.ClientHandler;

public class TCPServer {

    public static void main (String args[]) {

        try{

            int port = 1234; 
            ServerSocket listenServerSocket = new ServerSocket(port);

            System.out.println("\n----------Server started----------\n\n");

            while(true) {
                Socket clientSocket = listenServerSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
            }
            
        } catch(IOException e) {
            System.out.println("\nAddress already in use!\n\n");

        } 
    }
    
}
