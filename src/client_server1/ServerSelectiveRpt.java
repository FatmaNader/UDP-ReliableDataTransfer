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

    public int client_port;
    public int server_port;
    public InetAddress IPAddress;
    public String Filename;
    public int packets_needed;
    public int Dpacket_length = 500;                //the length of data in every packet
    public byte[] file_bytes;    // file of interest converted to bytes
    public int detail_length = 14;
    public double plp;
    public double plc = 0;
    public int dropafter=0;
    public int corruptionafter = (int) (1 / plc);
    public int windowSize;
    public int colour;
    //public int RandomSeed;
    int Result;

    public ArrayList<packet> allPackets = new ArrayList<>();

    public ServerSelectiveRpt(int client_port, int Server_port, String Filename, InetAddress IPAddress, int colour, int windowSize, double plp, int Result) {
        this.server_port = Server_port;
        this.client_port = client_port;
        this.IPAddress = IPAddress;
        this.Filename = Filename;
         this.plp = plp;
        
        this.colour = colour;
        this.windowSize = windowSize;
        //this.RandomSeed = RandomSeed;
       
        this.Result = Result;
        //this.serverSocket=serverSocket;
    }

   
    public void run() {

        try {
            ClientServerUtils.PRINT("Welcome to selective repeat server!", colour);
            ClientServerUtils.PRINT("-------------------------------------", colour);
            ClientServerUtils.PRINT("Client port here :" + client_port, colour);
            file_bytes = ClientServerUtils.loadFile(Filename, file_bytes, Dpacket_length, client_port, colour);
            packets_needed = (file_bytes.length / Dpacket_length) + 1;
            ClientServerUtils.PRINT("User " + client_port + "  packets needed to send: " + Integer.toString(packets_needed), colour);
            byte initialize[] = new byte[8];
            ByteBuffer bx = ByteBuffer.allocate(4);
            bx.putInt(server_port);
            byte result1[] = new byte[4];
            result1 = bx.array();
            ClientServerUtils.PRINT("User " + client_port + " server port: " + server_port, colour);
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
                 long Start_time =System.currentTimeMillis();
                SendFile(serverSocket);
                long end_time = System.currentTimeMillis();
               long througput = (file_bytes.length / ((end_time-Start_time)/100));
                System.out.println("THE throuput is "+througput + " bits/sec and the time taken is "+(end_time-Start_time));
            ClientServerUtils.PRINT("User " + client_port + " SEND FILE FINISHED", colour);
            serverSocket.close();

        } catch (SocketException ex) {
            Logger.getLogger(ServerSW.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ServerSelectiveRpt.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void SendFile(final DatagramSocket serverSocket) throws IOException {

        ClientServerUtils.PRINT("User " + client_port + " SEND FILE BEGIN", colour);
        ClientServerUtils.PRINT("----------------------------------------------------------------", colour);
        ClientServerUtils.PRINT("----------------------------------------------------------------", colour);

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
                packet p = new packet(PCKT_NO,  packet_to_send);
                allPackets.add(p);
                Timer1 pkt_timer = new Timer1(PCKT_NO);
                TimerTask1 pkt_action = new TimerTask1(PCKT_NO) {
                    public void run() {
                        int timerseq = this.getSequenceNumber();

                        if (!allPackets.get(timerseq).isAck) {

                            ClientServerUtils.PRINT("User " + client_port + " Packet " + timerseq + " timeout!", colour);
                            ClientServerUtils.Send_Data(serverSocket, allPackets.get(timerseq).packet, IPAddress, client_port);
                            ClientServerUtils.PRINT("User " + client_port + " Resending packet with sequence number: " + timerseq, colour);
                           // run();
                        }
                        else 
                            this.cancel();
                    }

                };
                pkt_timer.schedule(pkt_action, 50);

                //corrupt the packet
                if (corruptionafter == 0) {
                    rightChecksum = checksum;
                    checksum = 1;
                    ByteBuffer bx = ByteBuffer.allocate(8);
                    bx.putLong(checksum);
                    byte[] b;
                    b = bx.array();
                    ClientServerUtils.copyArray(b, packet_to_send, 2, 8);
                    allPackets.get(PCKT_NO).packet = packet_to_send;
                }
                corruptionafter--;
               
               
                if (dropafter != Result) {
                    ClientServerUtils.Send_Data(serverSocket, packet_to_send, IPAddress, client_port);
                    ClientServerUtils.PRINT("User " + client_port + " Window base: " + (windowBase + 1) + "           Window High: " + (windowBase + windowSize), colour);
                    ClientServerUtils.PRINT("User " + client_port + " Sent packet with sequence number : " + PCKT_NO, colour);

                    ClientServerUtils.PRINT("----------------------------------------------------------------", colour);
                } else {
                    //dropafter = (int) (1 / plp);
                    ClientServerUtils.PRINT("User " + client_port + " Packet with sequence number: " + PCKT_NO + " is lost!", colour);
                }
                dropafter++;
                if (dropafter == (int) (1 / plp)) {
                    dropafter = 0;
                       Result=ClientServerUtils.RandomTest(0, plp);
                }
               
                //begin counting for the next corruption
                if (corruptionafter == -1) {
                   
                    packet_to_send = get_packet(PCKT_NO);
                    allPackets.get(PCKT_NO).packet = packet_to_send;
                    corruptionafter = (int) (1 / plc);

                }

                PCKT_NO++;
                count = count + Dpacket_length;
            } else {
                // if pipeline is full
                while (true) {
                    int ackReceived = 0;

                    int ackSeq = recieve_Ack(serverSocket);

                    // whenever ack is received, break to send next packet else, resend all 
                   
                    allPackets.get(ackSeq).setIsAck(true);
                    ClientServerUtils.PRINT("User " + client_port + " Received Acknowledgment with sequence number: " + ackSeq, colour);

                    // if ack sequence number is the first window base, shift window forward to the next not unacknowledged
                    if (ackSeq == windowBase + 1) {
                        windowBase = ackSeq;
                        int seq = ackSeq + 1;
                        ClientServerUtils.PRINT("User " + client_port + " Window base: " + (windowBase + 1) + "           Window High: " + (windowBase + windowSize), colour);
                        ClientServerUtils.PRINT("----------------------------------------------------------------", colour);
                        while (allPackets.get(seq).isAck) {
                            windowBase = seq;
                            seq++;
                            ClientServerUtils.PRINT("User " + client_port + " Window base: " + (windowBase + 1) + "           Window High: " + (windowBase + windowSize), colour);
                            ClientServerUtils.PRINT("----------------------------------------------------------------", colour);
                        }
                    }
                    break;

                }
            }

        }
        boolean isLastAckPacket = false;
        int resendCounter = 0;
        int y[] = new int[2];
        ClientServerUtils.PRINT("User " + client_port + " Finished sending all packets first time", colour);
        while (!isLastAckPacket) {

            boolean ackReceived = false;

            int ackSequenceNum = 0;

            ackSequenceNum = recieve_Ack(serverSocket);

            allPackets.get(ackSequenceNum).setIsAck(true);  
            ClientServerUtils.PRINT("User " + client_port + " Received acknowledgment with sequence number: " + ackSequenceNum, colour);
            if (ackSequenceNum == windowBase + 1) {

                //System.out.println(" Helooo heree   " + ackSequenceNum);
                while (allPackets.get(ackSequenceNum).isAck) {
                    windowBase = windowBase + 1;
                    ackSequenceNum++;
                    ClientServerUtils.PRINT("User " + client_port + " Window base: " + (windowBase + 1) + "           Window High: " + (windowBase + windowSize), colour);
                    ClientServerUtils.PRINT("----------------------------------------------------------------", colour);
                    if (ackSequenceNum == packets_needed) {
                        break;
                    }
                }
            }

            // if ack sequence number == last packet's sequence number, set isLastAckPacket to true so that we can break from the while loop and close the socket
            if (windowBase == (packets_needed - 1)) {
                isLastAckPacket = true;
                ClientServerUtils.PRINT("User " + client_port + " Window base: " + (windowBase + 1) + "           Window High: " + (windowBase + windowSize), colour);
                ClientServerUtils.PRINT("User " + client_port + " Received final acknowledgment, now shutting down.", colour);

                ClientServerUtils.PRINT("----------------------------------------------------------------", colour);

            }

        }
    }

    public static int recieve_Ack(DatagramSocket serverSocket) throws SocketException, IOException {
        //WAIT FOR ACK
        byte[] Ack = new byte[5];

        DatagramPacket receivePacket = new DatagramPacket(Ack, Ack.length);
        serverSocket.receive(receivePacket);
        int ack_seq = ClientServerUtils.server_get_seq_no(receivePacket.getData());

        return ack_seq;

    }

    public byte[] get_packet(int PCKT_NO) {
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
        ClientServerUtils.copyArray(s, packet_detail, 0, 2);

        
        //we must send the checksum in the packet
        // update the current checksum with the specified array of bytes
        ch.update(data_to_send, 0, data_to_send.length);
        long checksum;

        checksum = ch.getValue();

        byte[] b;

        ByteBuffer bx = ByteBuffer.allocate(8);

        bx.putLong(checksum);
        b = bx.array();
        ClientServerUtils.copyArray(b, packet_detail, 2, 8);
        ch.reset();

        //we must send the sequence no of the packet
        ByteBuffer bz = ByteBuffer.allocate(4);
        bz.putInt(PCKT_NO);
        byte seq_no[] = new byte[4];
        seq_no = bz.array();
        ClientServerUtils.copyArray(seq_no, packet_detail, 10, 4);
        packet_to_send = ArrayUtils.addAll(packet_detail, data_to_send);

        String d = new String(data_to_send);
        return packet_to_send;
    }

}
