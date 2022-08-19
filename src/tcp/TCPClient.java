package tcp;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

public class TCPClient {
    
    public TCPClient(String host, int port) {

        Socket socket = null;

        try{

            socket = new Socket(host, port); 

            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String clientMessage = "", serverMessage = "";
            boolean check = true;

            String errorClientAlreadyExists = "A terminal is already being used by the Client!";
            String availableMemory = "Available memory: ";
            String errorMemoryFull = "Server full memory, please try again later!";
            String errorValidValue = "Please enter a valid value!";
            String errorZeroValue = "Enter a value greater than 0!";
            String errorInsufficientMemory = "There is not enough space for memory allocation!";
            String success = "Success";            

            System.out.println("\n Client: /" + host + "\n");
            System.out.println("\n------------------------------------\n\n");

            while(check) {

                serverMessage = dataInputStream.readUTF();

                if(serverMessage.equals(errorClientAlreadyExists)) {

                    showMessage(serverMessage, 1);                   
    
                } else if(serverMessage.contains(availableMemory)) {

                    showMessage(serverMessage, 0);
                    System.out.println("------------------------------------\n\n");

                } else if(serverMessage.equals(errorMemoryFull)) {

                    showMessage(serverMessage, 1);                   

                } else if (serverMessage.equals(errorValidValue)) {

                    showMessage(serverMessage, 0);

                } else if (serverMessage.equals(errorZeroValue)) {

                    showMessage(serverMessage, 0);

                } else if (serverMessage.equals(errorInsufficientMemory)) {

                    showMessage(serverMessage, 1);

                } else if (serverMessage.equals(success)) {
                    check = false; 
                    break;

                } else {

                    System.out.print(serverMessage); 
                    clientMessage = br.readLine();
                    toTheServer(clientMessage, dataOutputStream);

                }                                                
            }

            showIntro();           

            while(!clientMessage.equals(".")){
                
                System.out.print("Enter a value: ");                
                clientMessage = br.readLine();     
                
                if (clientMessage.equals(".")) {

                    System.out.println("\n\n----------Connection closed----------\n\n");
                    break;

                }

                toTheServer(clientMessage, dataOutputStream);

                serverMessage = dataInputStream.readUTF();
                System.out.println(serverMessage + "\n");

            }            

        }catch (UnknownHostException e){
            System.out.println("Sock:" + e.getMessage());

        } catch (EOFException e) {

        } catch (IOException e) {    

            System.out.println("Server was shut down before performing the operation!");
            System.out.println("\n\n----------Connection closed----------\n\n");

        } finally {

            if(socket != null) 
                try {
                    socket.close();

                }catch (IOException e){
                    /*close falhou*/
                    System.out.println("Close: " + e.getMessage());
                }                
        }
    }

    public void showIntro(){

        System.out.println("\n\n------------------------------------\n");  

        System.out.println("\n----------Connection started----------\n\n");
        System.out.println("To leave send '.'\n\n");		
        System.out.println("------------------------------------\n\n");

    }    

    public void showMessage(String serverMessage, int status) {

        System.out.println(serverMessage + "\n\n");

        if(status == 1) {
            System.exit(0);
        }

    }

    public void toTheServer(String clientMessage, DataOutputStream out) {

        try {
            out.writeUTF(clientMessage);                
            out.flush();
        } catch (IOException e) {

            System.out.println("Connection: " + e.getMessage());

        }

    }

}
