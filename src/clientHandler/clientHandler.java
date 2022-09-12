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
    private InetAddress ip;
    private final int TAM = 10;
    private int space, amount, total_value_main_memory, total_swap_memory_value;
    boolean loop;
    private static ArrayList<String> memoria = new ArrayList<String>();
    private static ArrayList<String> swap = new ArrayList<String>();
    private static ArrayList<String> aux = new ArrayList<String>();

    public ClientHandler(Socket socket) {

        try {

            clientSocket = socket;
            dataInputStream = new DataInputStream(clientSocket.getInputStream());
            dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
            clientMessage = "";
            serverMessage = "";
            space = 0;
            amount = 0;
            total_value_main_memory = 0;
            total_swap_memory_value = 0;
            ip = clientSocket.getLocalAddress();
            loop = true;

            // duplicação de terminal/ip
            clientAlreadyExists(memoria, ip, dataOutputStream);

            this.start();

        } catch (IOException e) {
        }
    }

    public void run() {

        try {

            // testes(memoria, swap, out);

            while (loop) {
                // Quantidade de espaços de memória que o Cliente deseja alocar
                toClient("How many memory spaces do you want to use? ", dataOutputStream);
                clientMessage = dataInputStream.readUTF();
                // Verifica se o valor é válido para alocação de memória
                space = requestMemory(clientMessage, ip, dataOutputStream);

                if (space > 0 && space <= TAM)
                    loop = false;
            }

            // Calcula a quantidade de memória total
            amount = memoria.size() + space;
            // Calcula a quantidade de memória que vai para a memória swap
            total_swap_memory_value = amount - TAM;
            // Calcula a quantidade de memória que vai para a memória principal
            total_value_main_memory = space - total_swap_memory_value;

            // Verifica se há espaço suficiente para a alocação na memória principal
            if (amount <= TAM) {
                // Aloca os espaços de memória que o Cliente informou
                allocateInMemory(memoria, space, ip);
                // Informa o Servidor
                logs("Initiated", "\n", ip);
                // Informa o Cliente
                toClient("Sucesso", dataOutputStream);

            } else if (memoria.size() < TAM) {// Servidor com memória principal está com pouco espaço de armazenamento
                // Aloca uma parte do espaço de memoria que o Cliente informou na memória
                // principal
                allocateInMemory(memoria, total_value_main_memory, ip);
                // Aloca os espaços de memória que o Cliente informou na memória SWAP
                allocateInMemory(swap, total_swap_memory_value, ip);
                // Informa ao Servidor sobre a alocação de espaços de memória SWAP
                logs("Started - Running Partially",
                        "\n     - Cause: Not enough space in main memory, Client tasks moved to SWAP memory\n",
                        ip);
                toClient("Sucesso", dataOutputStream);

            } else {// Servidor com memória principal cheia
                    // Aloca os espaços de memória que o Cliente informou na memória SWAP
                allocateInMemory(swap, space, ip);
                // Informa ao Servidor sobre a alocação de espaços de memória SWAP
                logs("Waiting",
                        "\n     - Cause: Not enough space in main memory, Client moved to SWAP memory\n",
                        ip);
                // Informa o Cliente sobre alocação de memória na SWAP
                toClient("Server full! waiting in line ...", dataOutputStream);
            }

            testes(memoria, swap, dataOutputStream);

            // loop para verificar se já pode sair dá fila, caso Cliente esteja
            loop = false;
            while (!loop) {
                // Ip/Cliente está na memória princial
                if (memoria.contains(String.valueOf(ip))) {

                    toClient("show", dataOutputStream);

                    while (!clientMessage.equals(".")) {
                        // Recebe valor do Cliente
                        clientMessage = dataInputStream.readUTF();
                        // Se for . encerra o programa
                        if (clientMessage.equals(".")) {
                            break;
                        }
                    }

                    loop = true;
                    // Após o Cliente terminar suas tarefas, o Servidor desaloca a memória principal
                    // e swap
                    deallocateFromMemory(memoria, ip);
                    deallocateFromMemory(swap, ip);
                    // Transfere o Cliente da memória SWAP para a memória principal
                    moveClientInSwapToMain(memoria, swap, aux, amount);
                    testes(memoria, swap, dataOutputStream);

                    // Ip/Cliente está na SWAP
                } else {
                    toClient("hide", dataOutputStream);
                    // Pausa por 2 segundos a verificação para nao ocorrer travamentos da memoria ou
                    // processamento
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                    }
                }

            }

        } catch (EOFException e) {
            // System.out.println("EOF: " + e.getMessage());
            // Cliente informa o .
            // Libera a memória por garantia
            // Após o Cliente terminar suas tarefas, o Servidor desaloca a memória principal
            // e swap
            deallocateFromMemory(memoria, ip);
            deallocateFromMemory(swap, ip);
            // Transfere o Cliente da memória SWAP para a memória principal
            moveClientInSwapToMain(memoria, swap, aux, amount);
            testes(memoria, swap, dataOutputStream);

        } catch (IOException e) {
            // System.out.println("IO: " + e.getMessage());
            // Cliente fechou o terminal!
            // Libera a memória por garantia
            // Após o Cliente terminar suas tarefas, o Servidor desaloca a memória principal
            // e swap
            deallocateFromMemory(memoria, ip);
            deallocateFromMemory(swap, ip);
            // Transfere o Cliente da memória SWAP para a memória principal
            moveClientInSwapToMain(memoria, swap, aux, amount);
            testes(memoria, swap, dataOutputStream);

        } finally {
            // Libera a memória por garantia
            // Após o Cliente terminar suas tarefas, o Servidor desaloca a memória principal
            // e swap
            deallocateFromMemory(memoria, ip);
            deallocateFromMemory(swap, ip);
            // Transfere o Cliente da memória SWAP para a memória principal
            moveClientInSwapToMain(memoria, swap, aux, amount);
            // Informa ao Servidor que o Cliente saiu
            logs("It went out", "", ip);
            testes(memoria, swap, dataOutputStream);

            try {
                // Fecha a Conexão do Cliente
                clientSocket.close();

            } catch (IOException e) {
                /* close falhou */
            }
        }
    }

    private void testes(ArrayList<String> memoria, ArrayList<String> swap, DataOutputStream out) {

        // Mostra os espaços alocados na memória princial e na swap
        showAllocateMemory(memoria, "Main");
        showAllocateMemory(swap, "SWAP");

        // Verifica memória principal disponivel
        availableMemory(memoria, out);

    }

    private void moveClientInSwapToMain(ArrayList<String> memoria, ArrayList<String> swap, ArrayList<String> aux,
            int amount) {

        // Calculo de quanto sobrou na memoria principal
        amount = TAM - memoria.size();

        if (amount > swap.size())
            amount = swap.size();

        // Armazena os ips em uma lista auxiliar
        for (int i = 0; i < amount; i++)
            aux.add(String.valueOf(swap.get(i)));

        // Aloca memória da SWAP na memória principal
        for (int i = 0; i < amount; i++)
            memoria.add(String.valueOf(aux.get(i)));

        // Desaloca memória da SWAP
        for (int i = 0; i < amount; i++)
            swap.remove(String.valueOf(aux.get(i)));

        // Limpa memória do auxiliar
        aux.clear();
    }

    public void clientAlreadyExists(ArrayList<String> memoria, InetAddress ip, DataOutputStream dataOutputStream) {

        if (memoria.indexOf(String.valueOf(ip)) != -1)
            toClient("A terminal is already being used by the Client!", dataOutputStream);

    }

    public void availableMemory(ArrayList<String> memoria, DataOutputStream dataOutputStream) {

        int qtd = TAM - memoria.size();

        if (qtd > 0) {
            System.out.println(" *** Available Main Memory: " + qtd);
            toClient("Available memory: " + qtd, dataOutputStream);
        } else {
            System.out.println(" *** No Main Memory Available!");
            toClient("Server full, will need to wait in line!", dataOutputStream);
        }
    }

    public int requestMemory(String clientMessage, InetAddress ip, DataOutputStream out) {

        int space;

        try {
            space = toInt(clientMessage);

            if (space > 0 && space <= TAM)
                System.out.println(" ** " + "Client " + ip + ": requested " + space
                        + " memory space(s) for allocating processes!");
            else if (space < 0)
                toClient("Please enter a valid value!", out);
            else if (space == 0)
                toClient("Enter a value greater than 0!", out);
            else if (space > TAM)
                toClient("Value exceeded limit! Enter only 1 to 10 memory space(s).", out);

        } catch (Exception e) {
            space = -1;
            toClient("Please enter a valid value!", out);
        }

        return space;
    }

    public void allocateInMemory(ArrayList<String> memoria, int space, InetAddress ip) {

        for (int i = 0; i < space; i++)
            memoria.add(String.valueOf(ip));

    }

    public void deallocateFromMemory(ArrayList<String> memoria, InetAddress ip) {

        for (int i = memoria.size(); i >= 0; i--)
            memoria.remove(String.valueOf(ip));

    }

    public void toClient(String serverMessage, DataOutputStream out) {

        try {
            out.writeUTF(serverMessage);
            out.flush();
        } catch (IOException e) {
        }
    }

    public int toInt(String clientMessage) {
        return Integer.parseInt(clientMessage);
    }

    public void logs(String s, String m, InetAddress ip) {
        System.out.println(" ** " + "Client " + ip + ": " + s + "!" + m);
    }

    public void showAllocateMemory(ArrayList<String> memoria, String tipo) {

        System.out.println(" *** Memory " + tipo + "\n");

        for (int i = 0; i < memoria.size(); i++)
            System.out.println(memoria.get(i));

        System.out.println();
    }

}