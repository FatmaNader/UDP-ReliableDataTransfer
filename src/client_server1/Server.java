package client_server1;

import java.net.*;
import java.io.*;

public class Server {

    public static int Dpacket_length = 500;                //the length of data in every packet
    public static byte[] file_bytes;    // file of interest converted to bytes
    public static int client_port;//port no of the client connected now
    public static int packets_needed;
    private static InetAddress IPAddress;
    public static int detail_length = 14;
    public static double plp = 0;
    public static double plc = 0;
    public static int dropafter = (int) (1 / plp);
    public static int corruptionafter = (int) (1 / plc);
    // public Thread thread;

    public static void main(String args[]) throws IOException {

        run();
    }

    public static void run() throws SocketException, IOException {

        //System.out.println("DROP AFTER"+ dropafter);
        int server_inc = 9877;
        DatagramSocket serverSocket;
        int serverPort = 9876;
        serverSocket = new DatagramSocket(serverPort);
        while (true) {

            byte[] receiveData = new byte[100];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            String Filename = new String(receiveData);
            Filename = Filename.trim();

            int clientPort = receivePacket.getPort();
            System.out.println("Server " + clientPort + " received request to file: " + Filename);
            InetAddress IP_Address = receivePacket.getAddress();

             //ServerGBN s_t=new ServerGBN(clientPort,server_inc,Filename,IP_Address);
            //ServerSelectiveRpt s_t= new ServerSelectiveRpt(clientPort,server_inc,Filename,IP_Address);
            ServerThread s_t = new ServerThread(clientPort, server_inc, Filename, IP_Address);
            Thread thread = new Thread(s_t);
               //thread.start();

            if (thread.getState() != Thread.State.NEW) {
                return;
            }
            thread.setName((Integer.toString(server_inc)));
            thread.start();
            server_inc++;

            byte[] receiveData1 = new byte[100];
            DatagramPacket receivePacket1 = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket1);

            String Filename1 = new String(receiveData);
            Filename1 = Filename1.trim();

            int clientPort1 = receivePacket1.getPort();
            System.out.println("Server " + clientPort1 + " recieved request to file:" + Filename1);
            InetAddress IP_Address1 = receivePacket1.getAddress();

            //    ServerGBN s_t=new ServerGBN(clientPort,server_inc,Filename,IP_Address);
            ServerThread s_t1 = new ServerThread(clientPort1, server_inc, Filename1, IP_Address1);

            Thread thread1 = new Thread(s_t1);
               //thread.start();

            if (thread1.getState() != Thread.State.NEW) {
                return;
            }
            thread1.setName((Integer.toString(server_inc)));
            thread1.start();
            server_inc++;

        }

    }
}
