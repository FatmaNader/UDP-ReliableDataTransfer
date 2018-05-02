package client_server1;

import java.net.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class ServerRUN {

    public static final String ANSI_CYAN = "\u001B[36m";    //0
    public static final String ANSI_PURPLE = "\u001B[35m";  //1
    public static final String ANSI_BLUE = "\u001B[34m";    //2
    public static final String ANSI_GREEN = "\u001B[32m";   //3
    public static final String ANSI_RED = "\u001B[31m";     //4
    public static final String ANSI_YELLOW = "\u001B[33m";  //5
    public static final String ANSI_BLACK = "\u001B[30m";
    private static final Object syncObj = new Object();
    private static HashMap<Integer , String> clients=new HashMap<Integer, String>();
    public int mode;

    // public Thread thread;
    public static void main(String args[]) throws IOException {

      run();

    }
public static void init_clients()
{
      clients.put(12, "fatma");
      clients.put(2, "Rahul");
      clients.put(7, "Singh");
      clients.put(49, "Ajeet");
      clients.put(3, "Anuj");
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
init_clients();
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
        System.out.println(plp);
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

            byte[] receiveInfo = new byte[100];
            byte[] receiveData = new byte[100];
            
             DatagramPacket receivePacket1 = new DatagramPacket(receiveInfo, receiveInfo.length);
            serverSocket.receive(receivePacket1);
            InetAddress IP_Address = receivePacket1.getAddress();
            byte []uname = new byte [50];
            ClientServerUtils.copyArray(receiveInfo, uname, 0, 50);
            String name = new String(uname).trim();
            System.out.println("name:  "+name);
            int clientPort = receivePacket1.getPort();
             byte []pass = new byte [50];
            ClientServerUtils.copyArray1(receiveInfo, pass,50, 50);
            String passw = new String(pass).trim();
            int id = Integer.parseInt(passw);
            System.out.println("pass:  "+ id);
              //System.out.println("pppp:  "+clients.get(12));
            if (clients.get(id)==null)
            {
                System.out.println("ID NOT VALID");
                byte []b =new byte[1];
                b[0]=0;
                DatagramPacket sendack =new DatagramPacket(b, b.length,IP_Address,clientPort);
                serverSocket.send(sendack);
            }
            else if(clients.get(id).equals(name))
            {
                System.out.println("Correct");
                byte []b =new byte[1];
                b[0]=1;
                DatagramPacket sendack =new DatagramPacket(b, b.length,IP_Address,clientPort);
                serverSocket.send(sendack);
            }
            else 
                   {
                System.out.println("ID AND USERNAME DON'T MATCH");
                byte []b =new byte[1];
                b[0]=0;
                DatagramPacket sendack =new DatagramPacket(b, b.length,IP_Address,clientPort);
                serverSocket.send(sendack);
            }
            
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            String Filename = new String(receiveData);
            Filename = Filename.trim();

            
            ClientServerUtils.PRINT("The server recieved request from client " + clientPort + " to get file: " + Filename, colour);
            
                System.out.println("1) Stop and wait");
        System.out.println("2) Go back N");
        System.out.println("3) Selective repeat");
        System.out.println("Please choose the mode to use: ");
        Scanner scan = new Scanner(System.in);

        int i = scan.nextInt();

        switch (i) {
            case 1:
                //ServerRUN.mode=1;
                ServerSW s_t = new ServerSW(clientPort, server_inc, Filename, IP_Address,colour, windowSize, plp, Result);
                 Thread thread = new Thread(s_t);
            if (thread.getState() != Thread.State.NEW)  return;
            thread.setName((Integer.toString(server_inc)));
            thread.start();
                break;
           
            case 2:
                
            ServerGBN s_t1=new ServerGBN(clientPort,server_inc,Filename,IP_Address,colour, windowSize, plp, Result) ;
              Thread thread1= new Thread(s_t1);
            if (thread1.getState() != Thread.State.NEW)  return;
            thread1.setName((Integer.toString(server_inc)));
            thread1.start();
            break;
                
           case 3: 
            ServerSelectiveRpt s_t2 = new ServerSelectiveRpt(clientPort, server_inc, Filename, IP_Address, colour, windowSize, plp, Result);
             Thread thread2= new Thread(s_t2);
            if (thread2.getState() != Thread.State.NEW)  return;
            thread2.setName((Integer.toString(server_inc)));
            thread2.start();//
           break;      
        }
         server_inc++;
    }
}
}