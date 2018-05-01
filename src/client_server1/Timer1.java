package client_server1;
 import java.util.Timer;
/**
 *
 * @author galaxy1
 */
public class Timer1 extends Timer{
   


    private final int sequenceNumber;


    public Timer1(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    
    public int getSequenceNumber() {
        return sequenceNumber;
    }


    
}
