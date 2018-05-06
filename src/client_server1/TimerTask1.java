/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client_server1;
import java.util.TimerTask;

public class TimerTask1 extends TimerTask{
    boolean x=true ;
    private final int sequenceNumber;

 


    public TimerTask1(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }
    
    public boolean getX(){
        return x;
    }
    @Override
    public void run() 
    {
        
    }


    
}
