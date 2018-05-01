/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client_server1;

import org.apache.commons.lang3.ArrayUtils;
import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class ClientSW {

    public static byte[] line = null;
    public static int packets_needed;
    public static int ThreadServer;
    public static int MainServer;//main server
    public static int Dpacket_length = 500;                //the length of data in every packet
    public static int detail_length = 14;
    public static String IP;
    public static int clientPort;
    public static String filename;
    public static int windowSize;
    int color;

    public ClientSW(String IP, int MainServer, int clientPort, String filename, int windowSize ) {
        this.IP=IP;
        this.MainServer=MainServer;
        this.clientPort=clientPort;
        this.filename=filename;
        this.windowSize=windowSize;
  
    }

    public static void run() {
        try {
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            
            DatagramSocket clientSocket = new DatagramSocket(clientPort);
            
            //define Ip an Socket
            InetAddress IPAddress = InetAddress.getByName(IP);
            
            byte[] receiveData = new byte[512];
            byte[] init = new byte[8];
            
            System.out.println("Welcome to stop and wait client!");
            System.out.println("-------------------------------------");
           
            //get file name from user
            //String FileName = inFromUser.readLine();
            
            DatagramPacket sendPacket = new DatagramPacket(filename.getBytes(), filename.getBytes().length, IPAddress, MainServer);
            clientSocket.send(sendPacket);
            
            //the file name is sent we are supposed to wait for the file
            //get packets_needed to recieve file
            //GET PACKETS NEEDED TO RECIEVE FILE
            DatagramPacket receivePacket = new DatagramPacket(init, init.length);
            clientSocket.receive(receivePacket);
            
            if (receivePacket != null) {
//            String pckts = new String(receivePacket.getData());
                byte[] xxy = new byte[4];
                xxy = Arrays.copyOfRange(init, 0, 4);

                ByteBuffer byx = ByteBuffer.wrap(xxy);
                ThreadServer = byx.getInt();

                byte[] xx = new byte[4];
                xx = Arrays.copyOfRange(init, 4, 8);

                ByteBuffer by = ByteBuffer.wrap(xx);
                packets_needed = by.getInt();
                
                System.out.println("The new server socket is: " + ThreadServer);
                System.out.println("The packets needed to get file is:" + packets_needed);

            }
            if (ThreadServer > 10000) {
                System.out.println("HELOOOOOO MONICAAA");
                
                DatagramPacket receivePacket1 = new DatagramPacket(init, init.length);
                clientSocket.receive(receivePacket1);
                if (receivePacket1 != null) {
//            String pckts = new String(receivePacket.getData());
                    byte[] xxy = new byte[4];
                    xxy = Arrays.copyOfRange(init, 0, 4);
                    
                    ByteBuffer byx = ByteBuffer.wrap(xxy);
                    ThreadServer = byx.getInt();
                    
                    byte[] xx = new byte[4];
                    xx = Arrays.copyOfRange(init, 4, 8);
                    
                    ByteBuffer by = ByteBuffer.wrap(xx);
                    packets_needed = by.getInt();
                    
                    System.out.println("The new client socket is: " + ThreadServer);
                    System.out.println("The packets needed to get file is:" + packets_needed);
                    
                }
            }
            
            //clientSocket.receive(receivePacket);
            get_file(clientSocket);
            System.out.println("The file is received successfully");
            System.out.print("->Please enter file name to save: ");
            String NFileName = inFromUser.readLine();
            
            ClientServerUtils.SaveFile(NFileName,line);
            System.out.println("----------YOUR FILE IS SAVED NOW!----------");
        } catch (SocketException ex) {
            Logger.getLogger(ClientSW.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnknownHostException ex) {
            Logger.getLogger(ClientSW.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ClientSW.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static long get_line(byte[] bytes_rec, int expected_seq, long expected_check) {
        Checksum ch = new CRC32();
        byte data_rec[];
        byte[] packet_detail = new byte[detail_length];
        packet_detail = Arrays.copyOfRange(bytes_rec, 0, detail_length);

        //get data LENGTH OF DATA from the packet
        short len = ClientServerUtils.get_dataLength(bytes_rec);

        //get data needed from the packet
        data_rec = Arrays.copyOfRange(bytes_rec, detail_length, len + detail_length);
        //String debug = new String(data_rec);
        //System.out.println("packet:\n" + debug);

        //get checkSum from the packet
        int seq_no = ClientServerUtils.get_seq_no(bytes_rec,detail_length);
        int seq = (seq_no);
       // System.out.println("-Seq_no: " + seq);
        //calculate the current check sum
        ch.update(data_rec, 0, data_rec.length);
        long current_checksum = ch.getValue();
        System.out.println("Received packet with sequence number " + (seq_no%2) + " length " + len + " expected checksum " + expected_check+
            " and current checksum: "+ current_checksum);
         System.out.println("-----------------------------------------------------------------------------------------------------------------");

        //System.out.println("the checksum is" + current_checksum);
        ch.reset();
        //System.out.println("---HELLOOOOO-----");
        if (seq_no == expected_seq && current_checksum == expected_check) {
            line = ArrayUtils.addAll(line, data_rec);
            // System.out.println("---ARRAAAY SIZE NOWW IS:" + line.length);
        }
        return current_checksum;
    }

    //recieve file from sender
    public static void get_file(DatagramSocket clientSocket) throws IOException {

       // System.out.println("-------PACKETS NEEDED------" + packets_needed);
        int w;
        for (w = 0; w < packets_needed; w++) {
            //System.out.println("this is packet no" + w);

            long[] x = recieve_packet(clientSocket, w);

            long seq_no = x[0];
            long expected_checkSum = x[1];
            long curr_sheckSum = x[2];
            //IF THERE IS ERROR 
            while (curr_sheckSum != expected_checkSum || seq_no != w) {
                if (curr_sheckSum != expected_checkSum) {
                     System.out.println("Packet is CORRUPTED !");
                }
                if (seq_no != w) {
                    System.out.println("Packet is with wrong sequence number!");
                }
                System.out.println("The number of Packets Lost is: " + (seq_no - w));
                //System.out.println("+++++++++SEND NEGATIVE ACKNOWLEDGE ++++++++");

                //DON'T Send Negative Acknowledge
//                byte type = -1;
//                sendAck(clientSocket, type, w);
                //recieve retransmited data
                x = recieve_packet(clientSocket, w);
                seq_no = x[0];
                expected_checkSum = x[1];
                curr_sheckSum = x[2];
            } //IF THERE IS NO ERROR SEND POSITIVE ACKNOWLEDGEMENT

            //Send POSTITVE Acknowledge
            byte type = 1;
            ClientServerUtils.sendAck(clientSocket, w,IP,ThreadServer);

        }
      
    }

    public static long[] recieve_packet(DatagramSocket clientSocket, int pckt_no) throws IOException {
        //System.out.println("this is packet no" + pckt_no);
        byte[] receiveData = new byte[Dpacket_length + detail_length];

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);

        byte bytes_rec[];
        bytes_rec = receivePacket.getData();

        long expected_checkSum = ClientServerUtils.get_checkSum(bytes_rec);
        //System.out.println("EXPECTED CHECK SUM IS:" + expected_checkSum);
        int seq_no = ClientServerUtils.get_seq_no(bytes_rec,detail_length);

        // System.out.println("YHTHGRTTDTGI--------------------------");
        long curr_sheckSum = get_line(bytes_rec, pckt_no, expected_checkSum);

        InetAddress IPAddress = InetAddress.getByName(IP);
        long[] x = new long[3];
        x[0] = seq_no;
        x[1] = expected_checkSum;
        x[2] = curr_sheckSum;
        return x;
    }

    
    
}
