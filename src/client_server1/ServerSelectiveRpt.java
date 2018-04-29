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
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class ServerSelectiveRpt implements Runnable {

    public static int client_port;
    public static int server_port;
    public static InetAddress IPAddress;
    public static String Filename;
    public static int packets_needed;
    public static int Dpacket_length = 500;                //the length of data in every packet
    public static byte[] file_bytes;    // file of interest converted to bytes
    public static int detail_length = 14;
    public static double plp = 0;
    public static double plc = 0;
    public static int dropafter = (int) (1 / plp);
    public static int corruptionafter = (int) (1 / plc);
    public static int windowSize = 7;

    public static ArrayList<packet> allPackets = new ArrayList<>();

    public ServerSelectiveRpt(int client_port, int Server_port, String Filename, InetAddress IPAddress) {
        this.server_port = Server_port;
        this.client_port = client_port;
        this.IPAddress = IPAddress;
        this.Filename = Filename;
        //this.serverSocket=serverSocket;
    }

    public class packet {

        public int seq_no;
        public long time;
        public boolean isAck;
        public byte[] packet;

        public packet(int seq_no, long time, byte[] packet) {
            this.seq_no = seq_no;
            this.isAck = false;
            this.time = time;
            this.packet = packet;

        }

        public packet() {

        }

        public int getSeq_no() {
            return seq_no;
        }

        public void setSeq_no(int seq_no) {
            this.seq_no = seq_no;
        }

        public long getTime() {
            return time;
        }

        public void setTime(int time) {
            this.time = time;
        }

        public boolean isIsAck() {
            return isAck;
        }

        public void setIsAck(boolean isAck) {
            this.isAck = isAck;
        }

    }

    public void run() {

        try {
            System.out.println("Welcome to selective repeat server!");
            System.out.println("-------------------------------------");
            System.out.println("Client port here :" + client_port);
            loadFile(Filename);
            System.out.println("User " + client_port + "  packets needed to send: " + Integer.toString(packets_needed));
            byte initialize[] = new byte[8];
            ByteBuffer bx = ByteBuffer.allocate(4);
            bx.putInt(server_port);
            byte result1[] = new byte[4];
            result1 = bx.array();
            System.out.println("User " + client_port + " server port: " + server_port);
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
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            SendFile(serverSocket);
            System.out.println("User " + client_port + " SEND FILE FINISHED");
            serverSocket.close();

        } catch (SocketException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ServerSelectiveRpt.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void copyArray(byte src[], byte dest[], int offset, int length) {
        for (int i = 0; i < length; i++) {
            dest[i + offset] = src[i];

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
            System.err.println("User " + client_port + "\nError converting file into bytes.\n");
            System.exit(1);
        }
    }

    public void SendFile(DatagramSocket serverSocket) throws IOException {

        System.out.println("User " + client_port + " SEND FILE BEGIN");

        System.out.println("----------------------------------------------------------------");

        Checksum ch = new CRC32();
        byte[] packet_to_send = null;
        int count = 0;
        int PCKT_NO = 0;
        int last_ack = -1;
        int windowBase = -1;
        long checksum = 0;
        long rightChecksum;
        while (PCKT_NO < packets_needed) {

            if (PCKT_NO > windowBase && PCKT_NO <= windowBase + windowSize) {   // if pipeline is not full

                packet_to_send = get_packet(PCKT_NO);
                packet p = new packet(PCKT_NO, System.currentTimeMillis(), packet_to_send);
                allPackets.add(p);
                Timer1 pkt_timer = new Timer1(PCKT_NO);
                TimerTask1 pkt_action = new TimerTask1(PCKT_NO) {
                    public void run() {
                        int timerseq = this.getSequenceNumber();

                        if (!allPackets.get(timerseq).isAck) {

                            try {
                                System.out.println("Packet " + timerseq + " timeout!");
                                Send_Data(serverSocket, allPackets.get(timerseq).packet);
                                System.out.println("Resending packet with sequence number: " + timerseq);
                                // this.x=false;
                                this.cancel();
                            } catch (IOException ex) {
                                System.err.println("ERROR!");
                                Logger.getLogger(ServerSelectiveRpt.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }

                };
                pkt_timer.schedule(pkt_action, 200);

                if (corruptionafter == 0) {
                    rightChecksum
                            = checksum = 1;
                    corruptionafter = (int) (1 / plc);
                    ByteBuffer bx = ByteBuffer.allocate(8);
                    bx.putLong(checksum);
                    byte[] b;
                    b = bx.array();
                    copyArray(b, packet_to_send, 2, 8);

                }

                //serverSocket.setSoTimeout(50);
                if (dropafter != 0) {
                    Send_Data(serverSocket, packet_to_send);
                    System.out.println("Window base: " + (windowBase + 1) + "           Window High: " + (windowBase + windowSize));
                    System.out.println("Sent packet with sequence number : " + PCKT_NO);

                    System.out.println("----------------------------------------------------------------");
                } else {
                    dropafter = (int) (1 / plp);
                    System.out.println("Packet with sequence number: " + PCKT_NO + " is lost!");
                }
                dropafter--;
                if (corruptionafter == 0) {
                    System.out.println("righting the checkSum Of Packet:" + PCKT_NO);
                    packet_to_send = get_packet(PCKT_NO);
                    packet_to_send = allPackets.get(PCKT_NO).packet;

                }
                corruptionafter--;
                PCKT_NO++;
                count = count + Dpacket_length;
            } else {
                // if pipeline is full
                while (true) {
                    int ackReceived = 0;

                    int ackSeq = recieve_Ack(serverSocket);

                    // whenever ack is received, break to send next packet
                    // else, resend all
                    //System.out.println("ACKING nowwwww" + (ackSeq));
                    //last_ack = ackSeq;
                    allPackets.get(ackSeq).setIsAck(true);
                    System.out.println("Received Acknowledgment with sequence number: " + ackSeq);

                    // if ack sequence number is the first window base, shift window forward to the next not unacknowledged
                    if (ackSeq == windowBase + 1) {
                        windowBase = ackSeq;
                        int seq = ackSeq + 1;
                        System.out.println("Window base: " + (windowBase + 1) + "           Window High: " + (windowBase + windowSize));
                        System.out.println("----------------------------------------------------------------");
                        while (allPackets.get(seq).isAck) {
                            windowBase = seq;
                            seq++;
                            System.out.println("Window base: " + (windowBase + 1) + "           Window High: " + (windowBase + windowSize));
                            System.out.println("----------------------------------------------------------------");
                        }
                    }
                    break;
//                    } else {
//                        for (int j = windowBase + 1; j < PCKT_NO; j++) {
//
//                            packet_to_send = get_packet(j - 1);
//                            Send_Data(serverSocket, packet_to_send);
//                            //retransmissionCounter += 1;
//                            System.out.println(" Resending packet with sequence number: " + j);
//                        }

                }
            }

        }
        boolean isLastAckPacket = false;
        int resendCounter = 0;
        int y[] = new int[2];
        System.out.println("->Finished sending all packets first time");
        while (!isLastAckPacket) {

            boolean ackReceived = false;

            int ackSequenceNum = 0;

            ackSequenceNum = recieve_Ack(serverSocket);

            // whenever ack is received, break to receive other acknowledgments
            // else, resend all unacknowledged packets in the current window
            allPackets.get(ackSequenceNum).setIsAck(true);
            // last_ack = ackSequenceNum;
            System.out.println("Received acknowledgment with sequence number: " + ackSequenceNum);

            if (ackSequenceNum == windowBase + 1) {

                //System.out.println(" Helooo heree   " + ackSequenceNum);
                while (allPackets.get(ackSequenceNum).isAck) {
                    windowBase = windowBase + 1;
                    ackSequenceNum++;
                    System.out.println("Window base: " + (windowBase + 1) + "           Window High: " + (windowBase + windowSize));
                    System.out.println("----------------------------------------------------------------");
                    if (ackSequenceNum == packets_needed) {
                        break;
                    }
                }
            }

            // if ack sequence number == last packet's sequence number,
            // set isLastAckPacket to true so that we can break from the while loop and close the socket
            //System.out.println("WINDOOOOW BASEEE:" + windowBase);
            if (windowBase == (packets_needed - 1)) {
                isLastAckPacket = true;
                System.out.println("Window base: " + (windowBase + 1) + "           Window High: " + (windowBase + windowSize));
                System.out.println("Received final acknowledgment, now shutting down.");

                System.out.println("----------------------------------------------------------------");

            }

        }
    }

    public static int get_seq_no(byte[] bytes_rec) {
        byte[] xx = new byte[4];
        xx = Arrays.copyOfRange(bytes_rec, 1, 5);

        ByteBuffer by = ByteBuffer.wrap(xx);
        int seq_no = by.getInt();

        return seq_no;
    }

    public static int recieve_Ack(DatagramSocket serverSocket) throws SocketException, IOException {
        //WAIT FOR ACK
//        boolean flag = true;
        byte[] Ack = new byte[5];

        // serverSocket.setSoTimeout(10);
        DatagramPacket receivePacket = new DatagramPacket(Ack, Ack.length);
        serverSocket.receive(receivePacket);
        int ack_seq = get_seq_no(receivePacket.getData());

        // System.out.println("The user " + client_port + "**ACK recieved: " + seq11);
        //See If positive or TIMEOUT
        // System.out.println("The user " + client_port + "the type of packet: " + receivePacket.getData()[0]);
        return ack_seq;

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

    public static void Send_Data(DatagramSocket serverSocket, byte[] packet_to_send) throws IOException {
        DatagramPacket sendPacket1 = new DatagramPacket(packet_to_send, packet_to_send.length, IPAddress, client_port);
        serverSocket.send(sendPacket1);
    }

}
