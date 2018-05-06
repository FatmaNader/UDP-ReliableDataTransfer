package client_server1;

import java.net.*;
import java.io.*;
import org.apache.commons.lang3.ArrayUtils;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class ServerSW implements Runnable {

    private int client_port;
    private int server_port;
    private InetAddress IPAddress;
    private String Filename;
    private int packets_needed;
    private int Dpacket_length = 500;                //the length of data in every packet
    private byte[] file_bytes;    // file of interest converted to bytes
    private int detail_length = 14;
    private double plp;
    private double plc = 0;
    private int dropafter = 0;
    private int corruptionafter = (int) (1 / plc);
    //  public static DatagramSocket serverSocket;
    int colour;
    public int Result;

    public ServerSW(int client_port, int Server_port, String Filename, InetAddress IPAddress, int colour, int windowSize, double plp, int Result) {
        this.server_port = Server_port;
        this.client_port = client_port;
        this.IPAddress = IPAddress;
        this.Filename = Filename;
        this.colour = colour;
        this.plp = plp;
        this.Result = Result;

        //this.serverSocket=serverSocket;
        
    }

    public void run() {

        try {
            ClientServerUtils.PRINT("Welcome to stop and wait server!",colour);
            ClientServerUtils.PRINT("-------------------------------------",colour);
           ClientServerUtils.PRINT("Client port here :" + client_port,colour);
            file_bytes = ClientServerUtils.loadFile(Filename, file_bytes, Dpacket_length, client_port, colour);
            packets_needed = (file_bytes.length / Dpacket_length) + 1;
            ClientServerUtils.PRINT("User " + client_port + "  packets needed to send: " + Integer.toString(packets_needed),colour);

            byte initialize[] = new byte[8];
            ByteBuffer bx = ByteBuffer.allocate(4);
            bx.putInt(server_port);
            byte result1[] = new byte[4];
            result1 = bx.array();
            System.out.println("User " + client_port + " server port: " + server_port);
            
            ClientServerUtils.copyArray(result1, initialize, 0, 4);
            ByteBuffer b = ByteBuffer.allocate(4);
            b.putInt(packets_needed);
            byte result[] = new byte[4];
            result = b.array();
            ClientServerUtils.copyArray(result, initialize, 4, 4);
            DatagramSocket serverSocket = new DatagramSocket(server_port);
            DatagramPacket sendPacket = new DatagramPacket(initialize, initialize.length, IPAddress, client_port);
            try {
                serverSocket.send(sendPacket);
            } catch (IOException ex) {
                Logger.getLogger(ServerSW.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                  long Start_time =System.currentTimeMillis();
                SendFile(serverSocket);
                long end_time = System.currentTimeMillis();
               long througput = (file_bytes.length / ((end_time-Start_time)/100));
                System.out.println("THE throuput is "+througput + " bits/sec and the time taken is "+(end_time-Start_time));
            } catch (IOException ex) {
                Logger.getLogger(ServerSW.class.getName()).log(Level.SEVERE, null, ex);
            }
            ClientServerUtils.PRINT("User " + client_port + " SEND FILE FINISHED",colour);
            serverSocket.close();
            

        } catch (SocketException ex) {
            Logger.getLogger(ServerSW.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void SendFile(DatagramSocket serverSocket) throws IOException {

        ClientServerUtils.PRINT("User " + client_port + " SEND FILE BEGIN",colour);

        ClientServerUtils.PRINT("----------------------------------------------------------------",colour);

        Checksum ch = new CRC32();
        byte[] data_to_send, packet_to_send;
        int count = 0;

        byte[] packet_detail = new byte[detail_length];

        for (int i = 0; i < packets_needed; i++) {

            if ((count + Dpacket_length) > file_bytes.length - 1) {
                data_to_send = Arrays.copyOfRange(file_bytes, count, file_bytes.length); //this is the data supposed to be sent
            } else {
                data_to_send = Arrays.copyOfRange(file_bytes, count, (count + Dpacket_length));

            }

            //WE MUST SEND THE LENGTH OF THE DATA TO SEND IN THE PACKET
            ByteBuffer b1 = ByteBuffer.allocate(2);
            b1.putShort((short) data_to_send.length);
            byte[] s = new byte[2];
            s = b1.array();
            ClientServerUtils.copyArray(s, packet_detail, 0, 2);

            
            //we must send the checksum in the packet  And update the current checksum with the specified array of bytes
            ch.update(data_to_send, 0, data_to_send.length);
            long checksum;
            if (corruptionafter != 0) {
                checksum = ch.getValue();
            } else {
                checksum = 1;
                corruptionafter = (int) (1 / plc);
            }
            long rightChecksum = ch.getValue();
            corruptionafter--;
            byte[] b;

            ByteBuffer bx = ByteBuffer.allocate(8);

            bx.putLong(checksum);
         
            b = bx.array();
            ClientServerUtils.copyArray(b, packet_detail, 2, 8);
            ch.reset();

            //we must send the sequence no of the packet
            ByteBuffer bz = ByteBuffer.allocate(4);
            bz.putInt(i);
            byte seq_no[] = new byte[4];
            seq_no = bz.array();
            ClientServerUtils.copyArray(seq_no, packet_detail, 10, 4);

            packet_to_send = ArrayUtils.addAll(packet_detail, data_to_send);

            String d = new String(data_to_send);
            
            //SEND DATA
            if (dropafter != Result) {
               // if(i!=5)
                ClientServerUtils.Send_Data(serverSocket, packet_to_send, IPAddress, client_port);
                ClientServerUtils.PRINT("Sent packet with sequence number: " + (i % 2),colour);
            } else {
              
                ClientServerUtils.PRINT("Packet with sequence number: " + i + " is lost!",colour);
            }
            dropafter++;
            if (dropafter == (int) (1 / plp)) {
                dropafter = 0;
                   Result=ClientServerUtils.RandomTest(0, plp);
            }
            count = count + Dpacket_length;
 
            //WAIT FOR ACK
            {
                while ((recieve_Ack(serverSocket, i)) == false) {

                    ClientServerUtils.PRINT("Packet " + i + " timeout!",colour);

                    ClientServerUtils.PRINT("Resending packet with sequence number: " + i,colour);
                    
                    //Right the check sum if the packet was corrupted
                    if (rightChecksum != checksum) {
                        ByteBuffer bxx = ByteBuffer.allocate(8);
                        byte[] by;
                        bxx.putLong(rightChecksum);
                        by = bxx.array();
                        ClientServerUtils.copyArray(by, packet_to_send, 2, 8);
                        ClientServerUtils.get_checkSum(packet_to_send);
                    }
                    ClientServerUtils.Send_Data(serverSocket, packet_to_send, IPAddress, client_port);
                }
            }
        }
    }

    public boolean recieve_Ack(DatagramSocket serverSocket, int seq) throws SocketException, IOException {
        //WAIT FOR ACK
        boolean flag = true;
        byte[] Ack = new byte[5];

        serverSocket.setSoTimeout(50);

        DatagramPacket receivePacket = new DatagramPacket(Ack, Ack.length);
        try {
            serverSocket.receive(receivePacket);
        } catch (SocketTimeoutException ex) {
            flag = false;
           // ClientServerUtils.PRINT("The user " + client_port + "time out",colour);
        }

        int ack_seq = ClientServerUtils.server_get_seq_no(receivePacket.getData());
        int seq11 = (ack_seq) % 2;
        ClientServerUtils.PRINT("Received Acknowledgment with sequence number: " + seq11,colour);
        ClientServerUtils.PRINT("----------------------------------------------------------------",colour);

        //See If positive or TIMEOUT
        if (receivePacket.getData()[0] == -1 || ack_seq != seq || flag == false) {
            //System.out.println("the type of packet: "+receivePacket.getData()[0]);
            return false;
        } else {
            //System.out.println("The user "+client_port+"the type of packet: " + receivePacket.getData()[0]);
            return true;
        }
    }
}
