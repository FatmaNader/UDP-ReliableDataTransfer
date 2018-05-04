/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client_server1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Scanner;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * @author HP
 */
public class ClientRUN {

    //private static Object clientSocket;

    public static String[] Read_client_file(String fileInfo) throws FileNotFoundException, IOException {
        File file = new File(fileInfo);

        BufferedReader br = new BufferedReader(new FileReader(file));

        String[] info = new String[5];
        //IP wellknownserver clientport filename window
        for (int i = 0; i < 5; i++) {
            info[i] = br.readLine();
            // System.out.println(info[i]);
        }
        br.close();
        return info;
    }

    public static void main(String[] args) throws IOException {
        Scanner s = new Scanner(System.in);
        byte[] pass = new byte[50];
        byte[] uname = new byte[50];
        byte[] client_info = new byte[100];
       byte p = ' ';
      int f=0;
      DatagramSocket socket = null;
       while (true)
       {
        System.out.println("->Please enter your username");
        String username = s.nextLine();
        byte []y = username.trim().getBytes();
        ClientServerUtils.copyArray(y, uname, 0, y.length);
        for (int i = username.length(); i < 50; i++) {
            uname[i] =(byte)p ;
        }

        System.out.println("->Please enter your password");
        String password = s.nextLine();
        byte []x = password.trim().getBytes();
        ClientServerUtils.copyArray(x, pass, 0, x.length);
        for (int i = username.length(); i < 50; i++) {
            pass[i] = (byte) p;
        }

        byte[] b = new byte [1] ;
       
        
        client_info = ArrayUtils.addAll(uname, pass);
        System.out.println("->Please enter filename containing your info: ");
        String fileInfo = s.nextLine();
        String[] info = new String[5];
        info = Read_client_file(fileInfo);
        String IP = (info[0].trim());
        InetAddress IPAddress = InetAddress.getByName(IP);

        int MainServer = Integer.parseInt(info[1].trim());
        int clientPort = Integer.parseInt(info[2].trim());
        System.out.println(clientPort);
        String filename = info[3].trim();
        int windowSize = Integer.parseInt(info[4].trim());
        if(f==0)
        {
         socket=new DatagramSocket(clientPort);
        }
        f++;
        
        DatagramPacket sendPacket = new DatagramPacket(client_info, client_info.length, IPAddress, MainServer);
       socket.send(sendPacket);
       
        DatagramPacket packet=new DatagramPacket(b, b.length);
        socket.receive(packet);
        //socket.close();
        if(b[0]==1)
        {
        System.out.println("1) Stop and wait");
        System.out.println("2) Go back N");
        System.out.println("3) Selective repeat");
        System.out.println("Please choose the mode to use: ");
        Scanner scan = new Scanner(System.in);

        int i = scan.nextInt();

        switch (i) {
            case 1:
                //ServerRUN.mode=1;
                ClientSW client1 = new ClientSW(IP, MainServer, socket, filename, windowSize);
                client1.run();

                break;
            case 2:
                //ServerRUN.mode=2;
                ClientGBN client2 = new ClientGBN(IP, MainServer, clientPort, filename, windowSize,socket);
                client2.run();
                break;
            case 3:
                //ServerRUN.mode=3;
                ClientSelectiveRpt client3 = new ClientSelectiveRpt(IP, MainServer, clientPort, filename, windowSize,socket);
                client3.run();

        }
        break;

    }
           else {
            ClientServerUtils.PRINT("Wrong username or password!",4);
}
 
    }
    }
    

}
