package client_server1;

import java.net.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

public class ServerRUN {

    private static HashMap<Integer, String> clients = new HashMap<>();
    public int mode;

    // public Thread thread;
    public static void main(String args[]) throws IOException {
        run();

    }

    public static void init_clients() {
        clients.put(12, "fatma");
        clients.put(2, "Monica");
        clients.put(7, "Mohamed");
        clients.put(49, "ahmed");
        clients.put(34, "maha");
    }

    public static String[] Read_server_file() throws FileNotFoundException, IOException {
        
        File file = new File("server.in.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        String[] info = new String[4];
        //well known server window randomseed plp
        for (int i = 0; i < 4; i++) {
            info[i] = br.readLine();
        }
        
        br.close();
        return info;
    }



    public static void run() throws SocketException, IOException {
        init_clients();

        String[] info = new String[4];
        
        //Get info from serever file
        info = Read_server_file();
        int serverPort = Integer.parseInt(info[0].trim());
        int windowSize = Integer.parseInt(info[1].trim());
        int RandomSeed = Integer.parseInt(info[2].trim());
        double plp = Double.parseDouble(info[3].trim());
        
        //Calculate the  Random packet lost 
          int Result=ClientServerUtils.RandomTest(0, plp);
        int server_inc = 9877;
        DatagramSocket serverSocket;
        int colour = -1;
        serverSocket = new DatagramSocket(serverPort);
        
        System.out.println("Main server is listening to requests on port: " + serverPort);
        while (true) {
            if (colour == 5) {
                colour = -1;
            }
            colour++;
            int clientPort;
            InetAddress IP_Address;
            byte[] receiveData = new byte[100];
            byte[] receiveInfo = new byte[100];
            
            //recieve packet containing username and password
            DatagramPacket receivePacket1 = new DatagramPacket(receiveInfo, receiveInfo.length);
            serverSocket.receive(receivePacket1);
            
            IP_Address = receivePacket1.getAddress();
            byte[] uname = new byte[50];
            ClientServerUtils.copyArray(receiveInfo, uname, 0, 50);
            String name = new String(uname).trim();

            ClientServerUtils.PRINT("Username: " + name, colour);
            clientPort = receivePacket1.getPort();
            //System.out.println(clientPort);
          

            byte[] pass = new byte[50];
            ClientServerUtils.copyArray1(receiveInfo, pass, 50, 50);
            String passw = new String(pass).trim();
            int id = Integer.parseInt(passw);
            ClientServerUtils.PRINT("Password: " + ClientRUN.maskNumber(passw, "***"), colour);
            
            if (clients.get(id) == null) {
                ClientServerUtils.PRINT("Password NOT VALID", colour);

                byte[] b = new byte[1];
                b[0] = 0;
                DatagramPacket sendack = new DatagramPacket(b, b.length, IP_Address, clientPort);
                serverSocket.send(sendack);
            } else if (!clients.get(id).equals(name)) {

                ClientServerUtils.PRINT("USERNAME AND PASSWORD DON'T MATCH", colour);

                byte[] b = new byte[1];
                b[0] = 0;
                DatagramPacket sendack = new DatagramPacket(b, b.length, IP_Address, clientPort);
                serverSocket.send(sendack);
            } else {

                ClientServerUtils.PRINT("Correct username and password", 2);

                byte[] b = new byte[1];
                b[0] = 1;
                //Send ack
                DatagramPacket sendack = new DatagramPacket(b, b.length, IP_Address, clientPort);
                serverSocket.send(sendack);
                 // recieve File name and metod type
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                int i = (int) receiveData[0];
               
                byte[] Fn = new byte[120];
                ClientServerUtils.copyArray1(receiveData, Fn, 1, receiveData.length - 1);

                String Filename = new String(receiveData);
                Filename = Filename.trim();

                ClientServerUtils.PRINT("The server recieved request from client " + clientPort + " to get file: " + Filename, colour);
                long start = System.currentTimeMillis();
                switch (i) {
                    case 1:
            
                        ServerSW s_t = new ServerSW(clientPort, server_inc, Filename, IP_Address, colour, windowSize, plp, Result);
                        Thread thread = new Thread(s_t);
                        if (thread.getState() != Thread.State.NEW) {
                            return;
                        }
                        thread.setName((Integer.toString(server_inc)));
                        thread.start();
                        
                        break;

                    case 2:

                        ServerGBN s_t1 = new ServerGBN(clientPort, server_inc, Filename, IP_Address, colour, windowSize, plp, Result);
                        Thread thread1 = new Thread(s_t1);
                        if (thread1.getState() != Thread.State.NEW) {
                            return;
                        }
                        thread1.setName((Integer.toString(server_inc)));
                        thread1.start();
                        break;

                    case 3:
                        ServerSelectiveRpt s_t2 = new ServerSelectiveRpt(clientPort, server_inc, Filename, IP_Address, colour, windowSize, plp, Result);
                        Thread thread2 = new Thread(s_t2);
                        if (thread2.getState() != Thread.State.NEW) {
                            return;
                        }
                        thread2.setName((Integer.toString(server_inc)));
                        thread2.start();//
                        break;
                }
                server_inc++;
            }
        }
    }
}
