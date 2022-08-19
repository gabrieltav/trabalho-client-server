package clientHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler extends Thread {

    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private Socket clientSocket;
    private String clientMessage, serverMessage;
    private int spaceRequest;
    private InetAddress ipAddress;
    private boolean clientExist = false;
    private static ArrayList<String> memory = new ArrayList<String>();
    private final int MAINMEMORY = 10;

    public ClientHandler (Socket socket) {
        try {
            clientSocket = socket;
            dataInputStream = new DataInputStream(clientSocket.getInputStream());
            dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
            clientMessage = "";
            serverMessage = "";
            spaceRequest = 0;
            ipAddress = clientSocket.getLocalAddress();
            boolean check = true;

            clientExist = clientAlreadyExists(memory, ipAddress, dataOutputStream);
            
            showAllocateMemory(memory);
            availableMemoryServer(memory);

            if(memory.size() < MAINMEMORY) {

                availableMemoryClient(memory, dataOutputStream);

                while(check) {

                    toTheClient("How many memory spaces do you want to request? ", dataOutputStream);

                    clientMessage = dataInputStream.readUTF();
                    spaceRequest = requestMemory(clientMessage, ipAddress, dataOutputStream);
                    
                    if (spaceRequest > 0 && spaceRequest != -1){
                        check = false; 
                        break;
                    }

                }

                if(memory.size() + spaceRequest <= MAINMEMORY ) {

                    allocateInMemory(memory, spaceRequest, ipAddress);

                    toTheClient("Success", dataOutputStream);

                    System.out.println("Client " + ipAddress + ": connected!");                    
                    availableMemoryServer(memory);

                    this.start();

                } else {

                    if(clientExist) {
                        errorLogs("A terminal is already being used by the client!", ipAddress);
                    } else {
                        errorLogs("There is not enough space for memory allocation!", ipAddress);
                        toTheClient("There is not enough space for memory allocation!", dataOutputStream);
                    }
                    
                }                
                
            } else {

                if(clientExist) {
                    errorLogs("A terminal is already being used by the client!", ipAddress);
                } else {
                    errorLogs("Memory full!", ipAddress);
                    toTheClient("Server full memory, please try again later!", dataOutputStream);
                }
            }

        } catch(IOException e) {
            System.out.println("Client " + ipAddress + ": did not report space for memory allocation and exited!");
        }
    }    

    public void run() {

        try {             
            
            while(!clientMessage.equals(".")) {
                
                clientMessage = dataInputStream.readUTF();

                if (clientMessage.equals(".")) {

                    break;

                }

                toTheClient(serverMessage, dataOutputStream);

            }   
            
            deallocateFromMemory(memory, ipAddress);            

        } catch(EOFException e) {

            deallocateFromMemory(memory, ipAddress);            

        } catch(IOException e) {   

            deallocateFromMemory(memory, ipAddress);

        } finally {

            deallocateFromMemory(memory, ipAddress);
            System.out.println("Client " + ipAddress + ": it went out!"); 
            
            try {
                clientSocket.close();

            }catch (IOException e){
            }
        }
    }

    public boolean clientAlreadyExists(ArrayList<String> memory, InetAddress ipAddress, DataOutputStream dataOutputStream) {

        int p;
        p = memory.indexOf(String.valueOf(ipAddress));
        boolean clientExist = false;

        if(p != -1){
            clientExist = true;
            toTheClient("A terminal is already being used by the client!", dataOutputStream);
        }

        return clientExist;
    }

    public int requestMemory(String clientMessage, InetAddress ipAddress, DataOutputStream dataOutputStream) {

        int spaceRequest;

        try {
            spaceRequest = toInt(clientMessage);           
            
            if(spaceRequest == 0) {
                toTheClient("Enter a value greater than 0!", dataOutputStream);
            } else if(spaceRequest > 0) {
                System.out.println("Client " + ipAddress + ": requested " + spaceRequest + " memory spaces!");
            }

        } catch (Exception e) {
            spaceRequest = -1;
            System.out.println("Client " + ipAddress + ": reported an invalid value!");
            toTheClient("Please enter a valid value!", dataOutputStream);                  
        }

        return spaceRequest;

    }

    public void allocateInMemory(ArrayList<String> memory, int spaceRequest, InetAddress ipAddress) {

        for (int i = 0; i < spaceRequest; i++) {

            memory.add(String.valueOf(ipAddress));

        }

    }

    public void deallocateFromMemory(ArrayList<String> memory, InetAddress ipAddress) {

        for (int i = memory.size(); i >= 0; i--) {

            memory.remove(String.valueOf(ipAddress));

        }

    }

    public void showAllocateMemory(ArrayList<String> memory) {

        for (int i = 0; i < memory.size(); i++) {

            System.out.println(memory.get(i));

        }

    }

    public void availableMemoryServer(ArrayList<String> memory) {

        int amountOfMemory = MAINMEMORY - memory.size();

        if(amountOfMemory > 0) {
            System.out.println("Available memory: " + amountOfMemory);
        } else {
            System.out.println("No memory available!");
        }   

    }

    public void availableMemoryClient(ArrayList<String> memory, DataOutputStream dataOutputStream) {

        int qtd = MAINMEMORY - memory.size();
        toTheClient("Available memory: " + qtd, dataOutputStream);

    }

    public void toTheClient(String serverMessage, DataOutputStream out){

        try {
            out.writeUTF(serverMessage);                
            out.flush();
        } catch (IOException e) {

        }

    }
    
    public int toInt(String clientMessage2){

        return Integer.parseInt(clientMessage); 

    }

    public void errorLogs(String error, InetAddress ipAddress){

        System.out.println("Client " + ipAddress + ": denied!"); 
        System.out.println(error);

    }

}