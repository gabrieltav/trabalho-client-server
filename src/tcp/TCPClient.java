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

        try {

            socket = new Socket(host, port);
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            String clientMessage = "", serverMessage = "";
            boolean loop = true;

            String errorClientAlreadyExists = "A terminal is already being used by the Customer!";
            String availableMemory = "Available memory: ";
            String noAvailableMemory = "Server full, will need to wait in line!";
            String waitingInQueue = "Server full! Waiting in line...";
            String success = "Success";

            System.out.println("\n -- " + "Client: /" + host + "\n");
            System.out.println("\n------------------------------------\n\n");

            while (loop) {

                serverMessage = dataInputStream.readUTF();

                if (serverMessage.equals(errorClientAlreadyExists)) {
                    showMessage(serverMessage, 1);

                } else if (serverMessage.contains(availableMemory) || serverMessage.contains(noAvailableMemory)) {
                    showMessage(serverMessage, 0);
                    System.out.println("------------------------------------\n\n");

                } else if (serverMessage.equals(waitingInQueue)) {
                    showMessage(serverMessage, 0);
                    loop = false;

                } else if (serverMessage.equals(success)) {
                    loop = false;

                } else {
                    System.out.print(serverMessage);
                    clientMessage = bufferedReader.readLine();
                    toServer(clientMessage, dataOutputStream);
                }
            }

            loop = false;
            while (!loop) {

                serverMessage = dataInputStream.readUTF();
                if (serverMessage.equals("show")) {

                    showIntro();

                    while (!clientMessage.equals(".")) {
                        System.out.print("Enter dot to exit: ");
                        clientMessage = bufferedReader.readLine();
                        if (clientMessage.equals(".")) {
                            System.out.println("\n\n**********Connection closed**********\n\n");
                            break;
                        }
                        toServer(clientMessage, dataOutputStream);

                        serverMessage = dataInputStream.readUTF();
                        System.out.println(serverMessage + "\n");
                    }

                    loop = true;
                } else if (serverMessage.equals("hide")) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                    }
                }
            }

        } catch (UnknownHostException e) {

        } catch (EOFException e) {
        } catch (IOException e) {
            System.out.println("\n ** Server was shut down before performing the operation!");
            System.out.println("\n\n**********Connection closed**********\n\n");

        } finally {

            if (socket != null)
                try {
                    socket.close();
                } catch (IOException e) {
                }
        }
    }

    public void showIntro() {

        System.out.println("\n\n------------------------------------\n");
        System.out.println("\n**********Connection started**********\n\n");
        System.out.println("- To leave send '.'\n\n");
        System.out.println("------------------------------------\n\n");

    }

    public void showMessage(String serverMessage, int status) {

        System.out.println(serverMessage + "\n\n");
        if (status == 1)
            System.exit(0);

    }

    public void toServer(String clientMessage, DataOutputStream out) {

        try {
            out.writeUTF(clientMessage);
            out.flush();
        } catch (IOException e) {
        }
    }
}
