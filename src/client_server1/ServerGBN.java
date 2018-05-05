/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client_server1;

import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class ServerGBN implements Runnable {

    public int client_port;
    public int server_port;
    public InetAddress IPAddress;
    public String Filename;
    public int packets_needed;
    public int Dpacket_length = 500;
    public byte[] file_bytes;    // file of interest converted to bytes
    public int detail_length = 14;
    public double plp;
    public double plc = 0;   //the length of data in every packet
    public int windowSize;
    public int dropafter = 0;
    public int retransmissionCounter = 0;
    int colour;
    public int Result;

    public ServerGBN(int client_port, int Server_port, String Filename, InetAddress IPAddress, int colour, int windowSize, double plp, int Result) throws SocketException {
        this.server_port = Server_port;
        this.client_port = client_port;
        this.IPAddress = IPAddress;
        this.Filename = Filename;
        this.colour = colour;
        this.Result = Result;
        this.plp = plp;
        this.windowSize = windowSize;

        //this.serverSocket=serverSocket;
    }

    public void run() {

        try {
            ClientServerUtils.PRINT("Welcome to go back n server!", colour);
            System.out.println(Result);
            ClientServerUtils.PRINT("-------------------------------------", colour);
            ClientServerUtils.PRINT("Client port here:" + client_port, colour);
            file_bytes = ClientServerUtils.loadFile(Filename, file_bytes, Dpacket_length, client_port, colour);
            packets_needed = (file_bytes.length / Dpacket_length) + 1;
            ClientServerUtils.PRINT("User " + client_port + "  packets needed to send: " + Integer.toString(packets_needed), colour);
            byte initialize[] = new byte[8];
            ByteBuffer bx = ByteBuffer.allocate(4);
            bx.putInt(server_port);
            byte result1[] = new byte[4];
            result1 = bx.array();
            ClientServerUtils.PRINT("User " + client_port + " server port:" + server_port, colour);
            ClientServerUtils.copyArray(result1, initialize, 0, 4);
            ByteBuffer b = ByteBuffer.allocate(4);
            b.putInt(packets_needed);
            byte result[] = new byte[4];
            result = b.array();
            ClientServerUtils.copyArray(result, initialize, 4, 4);
            DatagramSocket serverSocket = new DatagramSocket(server_port);
            serverSocket.setSoTimeout(50);

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
            ClientServerUtils.PRINT("User " + client_port + " SEND FILE FINISHED", colour);
            serverSocket.close();

        } catch (SocketException ex) {
            Logger.getLogger(ServerSW.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void SendFile(DatagramSocket serverSocket) throws IOException {

        ClientServerUtils.PRINT("User " + client_port + " SEND FILE BEGIN", colour);
        ClientServerUtils.PRINT("----------------------------------------------------------------", colour);

        Checksum ch = new CRC32();
        byte[] packet_to_send = null;
        int count = 0;
        int sequenceNum = 0;
        int last_ack = -1;
        int windowBase = -1;
        long checksum = 0;
        int corruptionafter = (int) (1 / plc);

        while (sequenceNum < packets_needed) {
            packet_to_send = ClientServerUtils.get_packet(sequenceNum, Dpacket_length, detail_length, file_bytes);

            if (sequenceNum > windowBase && sequenceNum <= windowBase + windowSize) {   // if pipeline is not full

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
                System.out.println(dropafter);
                if (dropafter != Result) {
                    //if(sequenceNum!=5)
                    ClientServerUtils.Send_Data(serverSocket, packet_to_send, IPAddress, client_port);
                    System.out.println(" Sent packet with sequence number: " + sequenceNum);

                } else {

                    ClientServerUtils.PRINT("Packet with sequence number: " + sequenceNum + " is lost!", colour + 1);

                }
                dropafter++;
                if (dropafter == (int) (1 / plp)) {
                    dropafter = 0;
                }

                ClientServerUtils.PRINT("Window base: " + (windowBase + 1) + "           Window High: " + (windowBase + windowSize), colour);
                // ClientServerUtils.PRINT("Sent packet with sequence number : " + sequenceNum, colour);

                System.out.println("----------------------------------------------------------------");
                sequenceNum++;
                count = count + Dpacket_length;
            } else {  // if pipeline is full
                while (true) {
                    boolean ackReceived = false;
                    byte[] Ack = new byte[5];
                    int[] x = new int[2];
                    x[0] = 0;
                    DatagramPacket receivePacket = new DatagramPacket(Ack, Ack.length);
                    int ackSequenceNum = 0;
                    try {
                        serverSocket.receive(receivePacket);
                        ackSequenceNum = ClientServerUtils.server_get_seq_no(receivePacket.getData());
                        ackReceived = true;
                    } catch (SocketTimeoutException ex) {
                        ackReceived = false;
                        ClientServerUtils.PRINT("User " + client_port + " timed out while waiting for acknowledgment", colour);
                    }

                    // x = recieve_Ack(serverSocket, last_ack);
//                    ackReceived = x[0];
//                    int ackSeq = x[1];
                    // whenever ack is received, break to send next packet
                    // else, resend all unacknowledged packets in the current window
                    if (ackReceived == true) {
                        //last_ack = ackSequenceNum;
                        ClientServerUtils.PRINT("Received Acknowledgment with sequence number: " + ackSequenceNum, colour);

                        // if ack sequence number > window base, shift window forward
                        if (ackSequenceNum > windowBase) {
                            serverSocket.setSoTimeout(50);
                            windowBase = ackSequenceNum;
                            ClientServerUtils.PRINT("Window base: " + (windowBase + 1) + "           Window High: " + (windowBase + windowSize), colour);
                            ClientServerUtils.PRINT("----------------------------------------------------------------", colour);
                        }
                        break;
                    } else {
                        for (int j = windowBase + 1; j < sequenceNum; j++) {
                            packet_to_send = ClientServerUtils.get_packet(j, Dpacket_length, detail_length, file_bytes);
                            ClientServerUtils.Send_Data(serverSocket, packet_to_send, IPAddress, client_port);
                            //ClientServerUtils.PRINT("Packet " + j + " timeout!", colour);
                            ClientServerUtils.PRINT("Resending packet with sequence number: " + j, colour);
                        }
                    }

                }
            }
        }

        // continue to receive acknowledgements until last acknowledgement is received
        boolean isLastAckPacket = false;
        int resendCounter = 0;
        int y[] = new int[2];
        ClientServerUtils.PRINT("->Finished sending all packets first time", colour);

        // loop untill all packets are recieved
        while (!isLastAckPacket) {
            boolean ackReceived = false;
            byte[] Ack = new byte[5];
            int[] x = new int[2];
            x[0] = 0;
            int ack_seq = 0;
            DatagramPacket receivePacket = new DatagramPacket(Ack, Ack.length);
            try {
                serverSocket.receive(receivePacket);
                ackReceived = true;
                ack_seq = ClientServerUtils.server_get_seq_no(receivePacket.getData());
            } catch (SocketTimeoutException ex) {
                ackReceived = false;
                ClientServerUtils.PRINT("User " + client_port + " timed out while waiting for acknowledgment", colour);
            }

            if (ackReceived == true) {

                ClientServerUtils.PRINT("Received acknowledgment with sequence number: " + ack_seq, colour);

                // if ack sequence number > window base, shift window forward
                if (ack_seq > windowBase) {
                    serverSocket.setSoTimeout(50);
                    windowBase = ack_seq;

                    ClientServerUtils.PRINT("Window base: " + (windowBase + 1) + "           Window High: " + (windowBase + windowSize), colour);
                    ClientServerUtils.PRINT("----------------------------------------------------------------", colour);
                }

                // if ack sequence number == last packet's sequence number,
                // set isLastAckPacket to true so that we can break from the while loop and close the socket
                if (ack_seq == packets_needed - 1) {
                    isLastAckPacket = true;
                    ClientServerUtils.PRINT("Window base: " + (windowBase + 1) + "           Window High: " + (windowBase + windowSize), colour);
                    ClientServerUtils.PRINT("Received final acknowledgment, now shutting down.", colour);

                    System.out.println("----------------------------------------------------------------");
                }

                // reset resend counter every time an acknowledgment is received
                resendCounter = 0;
            } else {
                resendCounter++;

                for (int j = windowBase + 1; j < sequenceNum; j++) {

                    packet_to_send = ClientServerUtils.get_packet(j, Dpacket_length, detail_length, file_bytes);
                    ClientServerUtils.Send_Data(serverSocket, packet_to_send, IPAddress, client_port);
                    // System.out.println("Sent packet with sequence number : " + (j-1));
                    retransmissionCounter += 1;
                    //ClientServerUtils.PRINT("Packet " + j + " timeout!", colour);
                    ClientServerUtils.PRINT("Resending packet with sequence number: " + (j), colour);
                }

            }
        }
        serverSocket.close();
    }

//    public int[] recieve_Ack(DatagramSocket serverSocket, int last_ack) throws SocketException, IOException {
//        //WAIT FOR ACK
//        boolean flag = true;
//        byte[] Ack = new byte[5];
//        int[] x = new int[2];
//        x[0] = 0;
//        DatagramPacket receivePacket = new DatagramPacket(Ack, Ack.length);
//        try {
//            serverSocket.receive(receivePacket);
//            flag=true;
//        } catch (SocketTimeoutException ex) {
//            flag = false;
//            ClientServerUtils.PRINT("User " + client_port + "time out",colour);
//        }
//
//        int ackSequenceNum = ClientServerUtils.server_get_seq_no(receivePacket.getData());
//
//        //See If positive or TIMEOUT
//        if (flag == false ) {
//            x[0] = 0;
//            x[1] = ackSequenceNum;
//            return x;
//        } 
//        else if ( ackSequenceNum != (last_ack + 1))
//        {
//            x[0]=2;
//            x[1]=ackSequenceNum;
//            return x;
//        }
//        else
//        {
//            x[0] = 1;
//            x[1] = ackSequenceNum;
//            return x;
//        }
//    }
}
