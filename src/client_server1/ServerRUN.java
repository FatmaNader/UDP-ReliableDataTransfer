package client_server1;

import java.net.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ServerRUN {

    public static final String ANSI_CYAN = "\u001B[36m";    //0
    public static final String ANSI_PURPLE = "\u001B[35m";  //1
    public static final String ANSI_BLUE = "\u001B[34m";    //2
    public static final String ANSI_GREEN = "\u001B[32m";   //3
    public static final String ANSI_RED = "\u001B[31m";     //4
    public static final String ANSI_YELLOW = "\u001B[33m";  //5
    public static final String ANSI_BLACK = "\u001B[30m";
    private static final Object syncObj = new Object();
    private static Map<String, ClientSelectiveRpt> clients;
    public int mode;

    // public Thread thread;
    public static void main(String args[]) throws IOException {
        run();
    }

    public static void run() throws SocketException, IOException {
        
        
//        clients = new HashMap<String, ClientSelectiveRpt>();
//        //ClientSelectiveRpt client1 = new ClientSelectiveRpt("Monica", 5);
//        clients.put("Monica", client1);

        //System.out.println("DROP AFTER"+ dropafter);
        int server_inc = 9877;
        DatagramSocket serverSocket;
        int serverPort = 9876;
        serverSocket = new DatagramSocket(serverPort);
        System.out.println("Main server is listening to requests on port: "+ serverPort);
        while (true) {

            byte[] receiveData = new byte[100];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            String Filename = new String(receiveData);
            Filename = Filename.trim();

            int clientPort = receivePacket.getPort();
            PRINT("The server recieved request from client " + clientPort + " to get file: " + Filename, 2);
            InetAddress IP_Address = receivePacket.getAddress();
            
       
            
            // ServerGBN s_t=new ServerGBN(clientPort,server_inc,Filename,IP_Address);
            ServerSelectiveRpt s_t = new ServerSelectiveRpt(clientPort, server_inc, Filename, IP_Address);
            //ServerThread s_t = new ServerSW(clientPort, server_inc, Filename, IP_Address);
            Thread thread = new Thread(s_t);
            //thread.start();

            if (thread.getState() != Thread.State.NEW) {
                return;
            }
            thread.setName((Integer.toString(server_inc)));
            thread.start();
            server_inc++;

//            byte[] receiveData1 = new byte[100];
//            DatagramPacket receivePacket1 = new DatagramPacket(receiveData1, receiveData1.length);
//            serverSocket.receive(receivePacket1);
//
//            String Filename1 = new String(receiveData1);
//            Filename1 = Filename1.trim();
//
//            int clientPort1 = receivePacket1.getPort();
//            PRINT("The server recieved request from client " + clientPort1 + " to get file:" + Filename1, 1);
//            InetAddress IP_Address1 = receivePacket1.getAddress();
//
//             //ServerGBN s_t=new ServerGBN(clientPort,server_inc,Filename,IP_Address);
//            // ServerSW s_t1 = new ServerSW(clientPort1, server_inc, Filename1, IP_Address1);
//            ServerSelectiveRpt s_t1 = new ServerSelectiveRpt(clientPort1, server_inc, Filename1, IP_Address1);
//            Thread thread1 = new Thread(s_t1);
//            //thread.start();
//
//            if (thread1.getState() != Thread.State.NEW) {
//                return;
//            }
//            thread1.setName((Integer.toString(server_inc)));
//            thread1.start();
//            server_inc++;
//
//        }
        }
    }

    public synchronized static void PRINT(String message, int color) {
        synchronized (syncObj) {
            switch (color) {
                case 0:
                    System.out.println(ANSI_CYAN + message + ANSI_CYAN);
                    break;
                case 1:
                    System.out.println(ANSI_PURPLE + message + ANSI_PURPLE);
                    break;
                case 2:
                    System.out.println(ANSI_BLUE + message + ANSI_BLUE);
                    break;
                case 3:
                    System.out.println(ANSI_GREEN + message + ANSI_GREEN);
                    break;
                case 4:
                    System.out.println(ANSI_RED + message + ANSI_RED);
                    break;
                case 5:
                    System.out.println(ANSI_YELLOW + message + ANSI_YELLOW);
                    break;
            }
        }
    }
}
