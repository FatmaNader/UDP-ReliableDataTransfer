package client_server1;

import java.net.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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

    public static String[] Read_server_file() throws FileNotFoundException, IOException {
        File file = new File("server.in.txt");

        BufferedReader br = new BufferedReader(new FileReader(file));

        String[] info = new String[4];
        //well known server window randomseed plp
        for (int i = 0; i < 4; i++) {
            info[i] = br.readLine();
            //System.out.println(info[i]);
        }
        return info;
    }

    public static int RandomTest(int RandomSeed, double plp) {
        // create random object
        Random randomno = new Random();
        int Low = 0;
        int High = (int) (1/plp);
        
        // setting seed
        randomno.setSeed(RandomSeed);
        int Result = randomno.nextInt(High - Low) + Low;

        // value after setting seed
        System.out.println("Object after seed: " + Result);
        return Result;
    }

    public static void run() throws SocketException, IOException {

//        clients = new HashMap<String, ClientSelectiveRpt>();
//        //ClientSelectiveRpt client1 = new ClientSelectiveRpt("Monica", 5);
//        clients.put("Monica", client1);
        //System.out.println("DROP AFTER"+ dropafter);
        String[] info = new String[4];
        info = Read_server_file();
        int serverPort = Integer.parseInt(info[0].trim());
        int windowSize = Integer.parseInt(info[1].trim());
        int RandomSeed = Integer.parseInt(info[2].trim());
        double plp = Double.parseDouble(info[3].trim());
        //System.out.println(plp);
        int Result=RandomTest(RandomSeed, plp);
        int server_inc = 9877;
        DatagramSocket serverSocket;
        //int serverPort = 9876;
        int colour = -1;
        serverSocket = new DatagramSocket(serverPort);
        System.out.println("Main server is listening to requests on port: " + serverPort);
        while (true) {
            if (colour == 5) {
                colour = -1;
            }
            colour++;

            byte[] receiveData = new byte[100];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            String Filename = new String(receiveData);
            Filename = Filename.trim();

            int clientPort = receivePacket.getPort();
            ClientServerUtils.PRINT("The server recieved request from client " + clientPort + " to get file: " + Filename, colour);
            InetAddress IP_Address = receivePacket.getAddress();

            //ServerGBN s_t=new ServerGBN(clientPort,server_inc,Filename,IP_Address,colour, windowSize, plp, Result) );
            ServerSelectiveRpt s_t = new ServerSelectiveRpt(clientPort, server_inc, Filename, IP_Address, colour, windowSize, plp, Result);
            //ServerSW s_t = new ServerSW(clientPort, server_inc, Filename, IP_Address,colour, windowSize, plp, Result);
            Thread thread = new Thread(s_t);
            //thread.start();

            if (thread.getState() != Thread.State.NEW) {
                return;
            }
            thread.setName((Integer.toString(server_inc)));
            thread.start();
            server_inc++;

        }
    }
}
