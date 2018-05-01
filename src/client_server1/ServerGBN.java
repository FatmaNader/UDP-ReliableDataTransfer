/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client_server1;

import com.sun.corba.se.impl.naming.cosnaming.NamingUtils;
import java.net.*;
import java.io.*;
import org.apache.commons.lang3.ArrayUtils;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class ServerGBN implements Runnable {

    public static int client_port;
    public static int server_port;
    public static InetAddress IPAddress;
    public static String Filename;
    public static int packets_needed;
    public static int Dpacket_length = 500;
    public static byte[] file_bytes;    // file of interest converted to bytes
    public static int detail_length = 14;
    public static double plp ;
    public static double plc = 0;   //the length of data in every packet
    public static int windowSize;
    public static int dropafter = 0;
    public static int retransmissionCounter = 0;
    int colour;
    public static int Result;

    public ServerGBN(int client_port, int Server_port, String Filename, InetAddress IPAddress, int colour, int windowSize, double plp, int Result) {
        this.server_port = Server_port;
        this.client_port = client_port;
        this.IPAddress = IPAddress;
        this.Filename = Filename;
        this.colour=colour;
        this.Result=Result;
        this.plp=plp;
        this.windowSize=windowSize;
      
        //this.serverSocket=serverSocket;
    }

    public void run() {

        try {
            System.out.println("Welcome to go back n server!");
            System.out.println("-------------------------------------");
            System.out.println("Client port here:" + client_port);
            file_bytes = ClientServerUtils.loadFile(Filename, file_bytes, Dpacket_length, client_port, colour);
            packets_needed = (file_bytes.length / Dpacket_length) + 1;
            System.out.println("User " + client_port + "  packets needed to send: " + Integer.toString(packets_needed));
            byte initialize[] = new byte[8];
            ByteBuffer bx = ByteBuffer.allocate(4);
            bx.putInt(server_port);
            byte result1[] = new byte[4];
            result1 = bx.array();
            System.out.println("User " + client_port + " server port:" + server_port);
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
                SendFile(serverSocket);
            } catch (IOException ex) {
                Logger.getLogger(ServerSW.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("User " + client_port + " SEND FILE FINISHED");
            serverSocket.close();

        } catch (SocketException ex) {
            Logger.getLogger(ServerSW.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void SendFile(DatagramSocket serverSocket) throws IOException {

        System.out.println("User " + client_port + " SEND FILE BEGIN");
        System.out.println("----------------------------------------------------------------");

        Checksum ch = new CRC32();
        byte[] packet_to_send = null;
        int count = 0;
        int PCKT_NO = 0;
        int last_ack = -1;
        int windowBase = -1;
        long checksum = 0;
        int corruptionafter = (int) (1 / plc);
        while (PCKT_NO < packets_needed) {

            if (PCKT_NO > windowBase && PCKT_NO <= windowBase + windowSize) {   // if pipeline is not full

                packet_to_send = ClientServerUtils.get_packet(PCKT_NO, Dpacket_length, detail_length, file_bytes);

                if (corruptionafter == 0) {
                    checksum = 1;
                    corruptionafter = (int) (1 / plc);
                    ByteBuffer bx = ByteBuffer.allocate(8);
                    bx.putLong(checksum);
                    byte[] b;
                    b = bx.array();
                    ClientServerUtils.copyArray(b, packet_to_send, 2, 8);
                }

                corruptionafter--;

                serverSocket.setSoTimeout(200);

                if (dropafter != Result) {
                    ClientServerUtils.Send_Data(serverSocket, packet_to_send, IPAddress, client_port);
                } else {
                   // dropafter = (int) (1 / plp);
                    System.out.println("Packet with sequence number: " + PCKT_NO + " is lost!");
                }
                dropafter++;
                if(dropafter== (int) 1/plp)
                    dropafter=0;
                
                System.out.println("Window base: " + (windowBase + 1) + "           Window High: " + (windowBase + windowSize));
                System.out.println("Sent packet with sequence number : " + PCKT_NO);

                System.out.println("----------------------------------------------------------------");
                PCKT_NO++;
                count = count + Dpacket_length;
            } else {  // if pipeline is full
                while (true) {
                    int ackReceived = 0;
                    int[] x = new int[2];
                    x[0] = 0;
                    x = recieve_Ack(serverSocket, last_ack);
                    ackReceived = x[0];
                    int ackSeq = x[1];

                    // whenever ack is received, break to send next packet
                    // else, resend all unacknowledged packets in the current window
                    if (ackReceived == 1) {
                        last_ack = ackSeq;
                        System.out.println("Received Acknowledgment with sequence number: " + ackSeq);

                        // if ack sequence number > window base, shift window forward
                        if (ackSeq > windowBase) {
                            windowBase = ackSeq;
                            System.out.println("Window base: " + (windowBase + 1) + "           Window High: " + (windowBase + windowSize));
                            System.out.println("----------------------------------------------------------------");
                        }
                        break;
                    } else {
                        for (int j = windowBase + 1; j < PCKT_NO; j++) {

                            packet_to_send = ClientServerUtils.get_packet(j - 1, Dpacket_length, detail_length, file_bytes);
                            ClientServerUtils.Send_Data(serverSocket, packet_to_send, IPAddress, client_port);
                            retransmissionCounter += 1;
                            System.out.println("Packet " + j + " timeout!");
                            System.out.println("Resending packet with sequence number: " + j);
                        }
                    }

                }
            }
        }

        // continue to receive acknowledgements until last acknowledgement is received
        boolean isLastAckPacket = false;
        int resendCounter = 0;
        int y[] = new int[2];
        System.out.println("->Finished sending all packets first time");
        while (!isLastAckPacket) {

            boolean ackReceived = false;

            int ackSequenceNum = 0;
            y[0] = 0;
            y = recieve_Ack(serverSocket, last_ack);
            ackSequenceNum = y[1];

            // whenever ack is received, break to receive other acknowledgments
            // else, resend all unacknowledged packets in the current window
            if (y[0] == 1) {
                last_ack = ackSequenceNum;
                System.out.println("Received acknowledgment with sequence number: " + ackSequenceNum);

                // if ack sequence number > window base, shift window forward
                if (ackSequenceNum > windowBase) {
                    windowBase = ackSequenceNum;
                    System.out.println("Window base: " + (windowBase + 1) + "           Window High: " + (windowBase + windowSize));
                    System.out.println("----------------------------------------------------------------");
                }

                // if ack sequence number == last packet's sequence number,
                // set isLastAckPacket to true so that we can break from the while loop and close the socket
                if (ackSequenceNum == packets_needed - 1) {
                    isLastAckPacket = true;
                    System.out.println("Window base: " + (windowBase + 1) + "           Window High: " + (windowBase + windowSize));
                    System.out.println("Received final acknowledgment, now shutting down.");

                    System.out.println("----------------------------------------------------------------");
                }

                // reset resend counter every time an acknowledgment is received
                resendCounter = 0;
            } else {
                resendCounter++;

                for (int j = windowBase + 1; j < PCKT_NO; j++) {

                    packet_to_send = ClientServerUtils.get_packet(j, Dpacket_length, detail_length, file_bytes);
                    ClientServerUtils.Send_Data(serverSocket, packet_to_send, IPAddress, client_port);
                    // System.out.println("Sent packet with sequence number : " + (j-1));
                    retransmissionCounter += 1;
                    System.out.println("Packet " + j + " timeout!");
                    System.out.println("Resending packet with sequence number: " + (j));
                }

            }
        }
        serverSocket.close();
    }

    public static int[] recieve_Ack(DatagramSocket serverSocket, int last_ack) throws SocketException, IOException {
        //WAIT FOR ACK
        boolean flag = true;
        byte[] Ack = new byte[5];
        int[] x = new int[2];
        x[0] = 0;
        DatagramPacket receivePacket = new DatagramPacket(Ack, Ack.length);
        try {
            serverSocket.receive(receivePacket);
        } catch (SocketTimeoutException ex) {
            flag = false;
            System.out.println("User " + client_port + "time out");
        }

        int ack_seq = ClientServerUtils.server_get_seq_no(receivePacket.getData());

        //See If positive or TIMEOUT
        if (receivePacket.getData()[0] == -1 || flag == false || ack_seq != (last_ack + 1)) {
            x[0] = 0;
            x[1] = ack_seq;
            return x;
        } else {
            x[0] = 1;
            x[1] = ack_seq;
            return x;
        }

    }

}
