/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client_server1;

import java.util.Scanner;

/**
 *
 * @author HP
 */
public class ClientRUN {

    public static void main(String[] args) {

        System.out.println("1) Stop and wait");
        System.out.println("2) Go back N");
        System.out.println("3) Selective repeat");
        System.out.println("Please choose the mode to use: ");
        Scanner scan = new Scanner(System.in);
        int i = scan.nextInt();
        
        switch (i) {
                case 1:
                    //ServerRUN.mode=1;
                   ClientSW client1= new ClientSW();
                   client1.run();
                   
                    break;
                case 2:
                    //ServerRUN.mode=2;
                    ClientGBN client2= new ClientGBN();
                    client2.run();
                    break;
                case 3:
                    //ServerRUN.mode=3;
                    ClientSelectiveRpt client3= new ClientSelectiveRpt();
                    client3.run();
                
        }
        

    }

}
