package client_server1;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.CRC32;
import org.apache.commons.lang3.ArrayUtils;
import java.util.zip.Checksum;

public class ClientGBN {

    public static byte[] line = null;
    public static int packets_needed;
    public static int NClientSocket;
    public static int ClientSocket = 9876;
    public static int Dpacket_length = 500;                //the length of data in every packet
    public static int detail_length = 14;
    public static String IP = "localhost";

    public static void main(String[] args) throws SocketException, UnknownHostException, IOException {

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        DatagramSocket clientSocket = new DatagramSocket();

        //define Ip an Socket
        InetAddress IPAddress = InetAddress.getByName(IP);

        byte[] receiveData = new byte[512];
        byte[] init = new byte[8];
        System.out.println("Welcome to selective repeat client!");
        System.out.println("-------------------------------------");
        System.out.print("->Please enter filename to request: ");
        //get file name from user
        String FileName = inFromUser.readLine();

        DatagramPacket sendPacket = new DatagramPacket(FileName.getBytes(), FileName.getBytes().length, IPAddress, ClientSocket);
        clientSocket.send(sendPacket);
        DatagramPacket receivePacket = new DatagramPacket(init, init.length);
        clientSocket.receive(receivePacket);

        if (receivePacket != null) {
//            String pckts = new String(receivePacket.getData());
            byte[] xxy = new byte[4];
            xxy = Arrays.copyOfRange(init, 0, 4);

            ByteBuffer byx = ByteBuffer.wrap(xxy);
            NClientSocket = byx.getInt();

            byte[] xx = new byte[4];
            xx = Arrays.copyOfRange(init, 4, 8);

            ByteBuffer by = ByteBuffer.wrap(xx);
            packets_needed = by.getInt();

            System.out.println("The new client socket is: " + NClientSocket);
            System.out.println("The packets needed to get file is:" + packets_needed);

        }
        if (NClientSocket > 10000) {
            System.out.println("HELOOOOOO MONICAAA");

            DatagramPacket receivePacket1 = new DatagramPacket(init, init.length);
            clientSocket.receive(receivePacket1);
            if (receivePacket1 != null) {
//            String pckts = new String(receivePacket.getData());
                byte[] xxy = new byte[4];
                xxy = Arrays.copyOfRange(init, 0, 4);

                ByteBuffer byx = ByteBuffer.wrap(xxy);
                NClientSocket = byx.getInt();

                byte[] xx = new byte[4];
                xx = Arrays.copyOfRange(init, 4, 8);

                ByteBuffer by = ByteBuffer.wrap(xx);
                packets_needed = by.getInt();

                System.out.println("The new client socket is: " + NClientSocket);
                System.out.println("The packets needed to get file is:" + packets_needed);

            }
        }
        //clientSocket.receive(receivePacket);
        get_file(clientSocket);

        System.out.println("The file is received successfully");
        System.out.print("->Please enter file name to save: ");
        String NFileName = inFromUser.readLine();

        SaveFile(NFileName);
        System.out.println("----------YOUR FILE IS SAVED NOW!----------");
    }

    public static void SaveFile(String NfileName) throws FileNotFoundException, IOException {
        FileOutputStream file = new FileOutputStream(NfileName);
        file.write(line);

        file.close();
    }

    public static void get_file(DatagramSocket clientSocket) throws IOException {

        //System.out.println("-------PACKETS NEEDED------" + packets_needed);
        int SequenceNum = 0;
      //  int sequenceNum = 0;
        // int previousSequenceNum = -1;
        while (SequenceNum < packets_needed) {
            //System.out.println("this is packet no" + SequenceNum);

            long[] x = recieve_packet(clientSocket, SequenceNum);

            long seq_no = x[0];
            long expected_checkSum = x[1];
            long curr_sheckSum = x[2];
            //IF THERE IS ERROR 
            while (curr_sheckSum != expected_checkSum || seq_no != SequenceNum) {
                if (curr_sheckSum != expected_checkSum) {
                    System.out.println("Packet is CORRUPTED !");
                }
                if (seq_no != SequenceNum) {
                    System.out.println("Packet is with wrong sequence number!");
                }
                System.out.println(" Expected sequence number: " + SequenceNum + " but recieved seq no is" + seq_no);
                //System.out.println("+++++++++SEND NEGATIVE ACKNOWLEDGE ++++++++");

                //DON'T Send Negative Acknowledge
//                byte type = -1;
//                sendAck(clientSocket, type, w);
                //recieve retransmited data
                sendAck(clientSocket, SequenceNum - 1);
                x = recieve_packet(clientSocket, SequenceNum);
                seq_no = x[0];
                expected_checkSum = x[1];
                curr_sheckSum = x[2];
            } //IF THERE IS NO ERROR SEND POSITIVE ACKNOWLEDGEMENT

            //Send POSTITVE Acknowledge
            byte type = 1;
            sendAck(clientSocket, SequenceNum);
            SequenceNum++;
        }
        // System.out.println(" GET FILE COMPLETED");
    }

    public static void sendAck(DatagramSocket clientSocket, int Ackseq) throws UnknownHostException, IOException {

        InetAddress IPAddress = InetAddress.getByName(IP);
        byte[] Ack = new byte[5];
        Ack[0] = 1;

        //we must send the sequence no of the packet
        ByteBuffer bz = ByteBuffer.allocate(4);
        bz.putInt(Ackseq);

        byte seq_noBytes[] = new byte[4];
        seq_noBytes = bz.array();
        copyArray(seq_noBytes, Ack, 1, 4);

        DatagramPacket sendPacket = new DatagramPacket(Ack, Ack.length, IPAddress, NClientSocket);
        clientSocket.send(sendPacket);
    }

    public static void copyArray(byte src[], byte dest[], int offset, int length) {
        for (int i = 0; i < length; i++) {
            dest[i + offset] = src[i];

        }
    }

    public static long[] recieve_packet(DatagramSocket clientSocket, int pckt_no) throws IOException {
       // System.out.println("this is packet no" + pckt_no);
        byte[] receiveData = new byte[Dpacket_length + detail_length];

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);

        byte bytes_rec[];
        bytes_rec = receivePacket.getData();

        long expected_checkSum = get_checkSum(bytes_rec);
        //System.out.println("EXPECTED CHECK SUM IS:" + expected_checkSum);
        int seq_no = get_seq_no(bytes_rec);

        // System.out.println("YHTHGRTTDTGI--------------------------");
        long curr_sheckSum = get_line(bytes_rec, pckt_no, expected_checkSum);

        InetAddress IPAddress = InetAddress.getByName(IP);
        long[] x = new long[3];
        x[0] = seq_no;
        x[1] = expected_checkSum;
        x[2] = curr_sheckSum;
        return x;
    }

    public static long get_line(byte[] bytes_rec, int expected_seq, long expected_check) {
        Checksum ch = new CRC32();
        byte data_rec[];
        byte[] packet_detail = new byte[detail_length];
        packet_detail = Arrays.copyOfRange(bytes_rec, 0, detail_length);

        //get data LENGTH OF DATA from the packet
        short len = get_dataLength(bytes_rec);

        //get data needed from the packet
        data_rec = Arrays.copyOfRange(bytes_rec, detail_length, len + detail_length);
        //String debug = new String(data_rec);
        //System.out.println("packet:\n" + debug);

        //get checkSum from the packet
        int seq_no = get_seq_no(bytes_rec);
        int seq = (seq_no);
        //System.out.println("-Seq_no: " + seq);
        //calculate the current check sum
        ch.update(data_rec, 0, data_rec.length);
        long current_checksum = ch.getValue();

       // System.out.println("the checksum is" + current_checksum);
        System.out.println("Received packet with sequence number " + seq_no + " length " + len + " expected checksum " + expected_check+
            " and current checksum: "+ current_checksum);
         System.out.println("-----------------------------------------------------------------------------------------------------------------");
        ch.reset();
        //System.out.println("---HELLOOOOO-----");
        if (seq_no == expected_seq && current_checksum == expected_check) {
            line = ArrayUtils.addAll(line, data_rec);
            // System.out.println("---ARRAAAY SIZE NOWW IS:" + line.length);
        }
        return current_checksum;
    }

    public static int get_seq_no(byte[] bytes_rec) {
        byte[] xx = new byte[4];
        xx = Arrays.copyOfRange(bytes_rec, 10, detail_length);

        ByteBuffer by = ByteBuffer.wrap(xx);
        int seq_no = by.getInt();
        //System.out.println("the seq_no for this paCKET IS " + seq_no % 2);
        return seq_no;
    }

    public static short get_dataLength(byte[] bytes_rec) {
        byte[] result = new byte[2];
        result = Arrays.copyOfRange(bytes_rec, 0, 2);
        ByteBuffer bb = ByteBuffer.wrap(result);
        short len = bb.getShort();
        //System.out.println("the length of data in this packet is:" + len);
        return len;
    }

    public static long get_checkSum(byte[] bytes_rec) {
        byte[] x = new byte[8];
        x = Arrays.copyOfRange(bytes_rec, 2, 10);

        ByteBuffer bx = ByteBuffer.wrap(x);
        long value = bx.getLong();
       // System.out.println("EXPECTED CHECK SUM IS " + value);
        return value;
    }

}
