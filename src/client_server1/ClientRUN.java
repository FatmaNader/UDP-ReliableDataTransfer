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
import java.util.Scanner;

/**
 *
 * @author HP
 */
public class ClientRUN {

    
    public static String[] Read_client_file(String fileInfo) throws FileNotFoundException, IOException {
        File file = new File(fileInfo);

        BufferedReader br = new BufferedReader(new FileReader(file));

        String[] info = new String[5];
        //IP wellknownserver clientport filename window
        for (int i = 0; i < 5; i++) {
            info[i] = br.readLine();
               // System.out.println(info[i]);
        }
        return info;
    }
    public static void main(String[] args) throws IOException {
        System.out.println("->Please enter filename containing your info: ");
           Scanner s = new Scanner(System.in);
           String fileInfo= s.nextLine();
        
        System.out.println("1) Stop and wait");
        System.out.println("2) Go back N");
        System.out.println("3) Selective repeat");
        System.out.println("Please choose the mode to use: ");
        Scanner scan = new Scanner(System.in);
        
        
         String[] info = new String[5];
        info = Read_client_file(fileInfo);
        String IP = (info[0].trim());
        int MainServer= Integer.parseInt(info[1].trim());
        int clientPort= Integer.parseInt(info[2].trim());
        String filename=info[3].trim();
        int windowSize = Integer.parseInt(info[4].trim());
        
        int i = scan.nextInt();
        
        switch (i) {
                case 1:
                    //ServerRUN.mode=1;
                   ClientSW client1= new ClientSW(IP, MainServer,clientPort, filename, windowSize );
                   client1.run();
                   
                    break;
                case 2:
                    //ServerRUN.mode=2;
                    ClientGBN client2= new ClientGBN(IP, MainServer,clientPort, filename, windowSize);
                    client2.run();
                    break;
                case 3:
                    //ServerRUN.mode=3;
                    ClientSelectiveRpt client3= new ClientSelectiveRpt(IP, MainServer,clientPort, filename, windowSize);
                    client3.run();
                
        }
        

    }

}
