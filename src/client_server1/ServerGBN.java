/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

public class ServerGBN implements Runnable {

    public static int client_port;
    public static int server_port;
    public static InetAddress IPAddress;
    public static String Filename;
    public static int packets_needed;
    public static int Dpacket_length = 500;
    public static byte[] file_bytes;    // file of interest converted to bytes
    public static int detail_length = 14;
    public static double plp = 0;
    public static double plc = 0;   //the length of data in every packet
    public static int windowSize =6;
    public static int dropafter = (int) (1 / plp);
    public static int retransmissionCounter = 0;
    //  public static DatagramSocket serverSocket;

    public ServerGBN(int client_port, int Server_port, String Filename, InetAddress IPAddress) {
        this.server_port = Server_port;
        this.client_port = client_port;
        this.IPAddress = IPAddress;
        this.Filename = Filename;
        //this.serverSocket=serverSocket;
    }

    public void run() {

        try {
            System.out.println("Welcome to go back n server!");
            System.out.println("-------------------------------------");
            System.out.println("Client port here:" + client_port);
            loadFile(Filename);
            System.out.println("User " + client_port + "  packets needed to send: " + Integer.toString(packets_needed));
            byte initialize[] = new byte[8];
            ByteBuffer bx = ByteBuffer.allocate(4);
            bx.putInt(server_port);
            byte result1[] = new byte[4];
            result1 = bx.array();
            System.out.println("User " + client_port + " server port:" + server_port);
            copyArray(result1, initialize, 0, 4);
            ByteBuffer b = ByteBuffer.allocate(4);
            b.putInt(packets_needed);
            byte result[] = new byte[4];
            result = b.array();
            copyArray(result, initialize, 4, 4);
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

    public static void loadFile(String filename) {
        File file = new File(filename);
        file_bytes = new byte[(int) file.length()];
        packets_needed = (file_bytes.length / Dpacket_length) + 1;
        System.out.println("User " + client_port + " file length is: " + file.length() + " bytes");

        try {
            FileInputStream stream = new FileInputStream(filename);
            stream.read(file_bytes);
            stream.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("The user " + client_port + "\nError converting file into bytes.\n");
            System.exit(1);
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
        // System.out.println("file LENGTH:"+(file_bytes.length-1)); 
        int corruptionafter = (int) (1 / plc);
        while (PCKT_NO < packets_needed) {

            if (PCKT_NO > windowBase && PCKT_NO <= windowBase + windowSize) {   // if pipeline is not full

                packet_to_send = get_packet(PCKT_NO);

                if (corruptionafter == 0) {
                    checksum = 1;
                    corruptionafter = (int) (1 / plc);
                    ByteBuffer bx = ByteBuffer.allocate(8);
                    bx.putLong(checksum);
                    byte[] b;
                    b = bx.array();
                    copyArray(b, packet_to_send, 2, 8);
                }

                corruptionafter--;

                serverSocket.setSoTimeout(10);

                if (dropafter != 0) {
                    Send_Data(serverSocket, packet_to_send);
                } else {
                    dropafter = (int) (1 / plp);
                    System.out.println("Packet with sequence number: " + PCKT_NO + " is lost!");
                }
                dropafter--;
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

                            packet_to_send = get_packet(j - 1);
                            Send_Data(serverSocket, packet_to_send);
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

//                if (resendCounter < 5) {
                for (int j = windowBase + 1; j < PCKT_NO; j++) {

                    packet_to_send = get_packet(j);
                    Send_Data(serverSocket, packet_to_send);
                    // System.out.println("Sent packet with sequence number : " + (j-1));
                    retransmissionCounter += 1;
                    System.out.println("Packet " + j + " timeout!");
                    System.out.println("Resending packet with sequence number: " + (j));
                }
//                } else {
//                    isLastAckPacket = true;
                //System.out.println(" Let assume we received final acknowledgment, now shutting down.");
//                    break;
//                }
            }
        }
        serverSocket.close();
    }

    public static byte[] get_packet(int PCKT_NO) {
        byte[] data_to_send = new byte[Dpacket_length];
        Checksum ch = new CRC32();
        byte[] packet_to_send;
        byte[] packet_detail = new byte[detail_length];
        int count = PCKT_NO * Dpacket_length;
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
        copyArray(s, packet_detail, 0, 2);

        // System.out.println("The user " + client_port + "DAATA LENGTH IS :" + data_to_send.length);
        //we must send the checksum in the packet
        // update the current checksum with the specified array of bytes
        ch.update(data_to_send, 0, data_to_send.length);
        long checksum;

        checksum = ch.getValue();

        byte[] b;

        ByteBuffer bx = ByteBuffer.allocate(8);

        bx.putLong(checksum);
        //-------------------
        //buffer to array
        b = bx.array();
        copyArray(b, packet_detail, 2, 8);
        //  System.out.println("The user " + client_port + "THE CHECKSum  sent is:" + checksum);
        ch.reset();

        //we must send the sequence no of the packet
        ByteBuffer bz = ByteBuffer.allocate(4);
        bz.putInt(PCKT_NO);
        //-------------------
        //buffer to array
        byte seq_no[] = new byte[4];
        seq_no = bz.array();
        copyArray(seq_no, packet_detail, 10, 4);
        packet_to_send = ArrayUtils.addAll(packet_detail, data_to_send);

        String d = new String(data_to_send);
        //System.out.println("data to send:\n" + d);

        return packet_to_send;
    }

    public static long get_checkSum(byte[] bytes_rec) {
        byte[] x = new byte[8];
        x = Arrays.copyOfRange(bytes_rec, 2, 10);

        ByteBuffer bx = ByteBuffer.wrap(x);
        long value = bx.getLong();
        // System.out.println("The user " + client_port + "HEREEE THIS IS NEW CHECK SUM SENT IS " + value);
        return value;
    }

    public static void copyArray(byte src[], byte dest[], int offset, int length) {
        for (int i = 0; i < length; i++) {
            dest[i + offset] = src[i];

        }
    }

    public static void Send_Data(DatagramSocket serverSocket, byte[] packet_to_send) throws IOException {
        DatagramPacket sendPacket1 = new DatagramPacket(packet_to_send, packet_to_send.length, IPAddress, client_port);
        serverSocket.send(sendPacket1);
    }

    public static int[] recieve_Ack(DatagramSocket serverSocket, int last_ack) throws SocketException, IOException {
        //WAIT FOR ACK
        boolean flag = true;
        byte[] Ack = new byte[5];
        int[] x = new int[2];
        x[0] = 0;
        // serverSocket.setSoTimeout(10);
        DatagramPacket receivePacket = new DatagramPacket(Ack, Ack.length);
        try {
            serverSocket.receive(receivePacket);
        } catch (SocketTimeoutException ex) {
            flag = false;
            System.out.println("User " + client_port + "time out");
        }

        int ack_seq = get_seq_no(receivePacket.getData());

        // System.out.println("The user " + client_port + "**ACK recieved: " + seq11);
        //See If positive or TIMEOUT
        if (receivePacket.getData()[0] == -1 || flag == false || ack_seq != (last_ack + 1)) {
            //System.out.println("the type of packet: "+receivePacket.getData()[0]);
            x[0] = 0;
            x[1] = ack_seq;
            return x;
        } else {
            // System.out.println("The user " + client_port + "the type of packet: " + receivePacket.getData()[0]);
            x[0] = 1;
            x[1] = ack_seq;
            return x;
        }

    }

    public static int get_seq_no(byte[] bytes_rec) {
        byte[] xx = new byte[4];
        xx = Arrays.copyOfRange(bytes_rec, 1, 5);

        ByteBuffer by = ByteBuffer.wrap(xx);
        int seq_no = by.getInt();

        //System.out.println("The user " + client_port + "-Seq no: " + seq);
        //System.out.println("the seq_no for this paCKET IS " + seq_no);
        return seq_no;
    }
}
