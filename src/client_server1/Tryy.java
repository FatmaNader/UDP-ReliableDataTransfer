/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client_server1;

import java.util.Random;

/**
 *
 * @author HP
 */
public class Tryy {
    public static void main(String[] args) {
       
        for(int i=0; i<50;i++){
               // int x=5;
           int x= RandomTest(5,0.1);
           
           System.out.println(x);
        }
        
    }
        public static int RandomTest(int RandomSeed, double plp) {
        // create random object
        Random randomno = new Random();
        int Low = 0;
        int High = (int) (1 / plp);

        // setting seed
        //randomno.setSeed();
        int Result = randomno.nextInt(High - Low) + Low;

        // value after setting seed
       // System.out.println("***********************Object after seed: " + Result);
        return Result;
    }
}
