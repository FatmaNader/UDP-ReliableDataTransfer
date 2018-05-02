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
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class ClientServerUtils {

    public static final String ANSI_RED = "\u001B[31m";    //0
    public static final String ANSI_PURPLE = "\u001B[35m";  //1
    public static final String ANSI_BLUE = "\u001B[34m";    //2
    public static final String ANSI_GREEN = "\u001B[32m";   //3
    public static final String ANSI_YELLOW = "\u001B[33m";  //4
    public static final String ANSI_CYAN = "\u001B[36m";    //5
    public static final String ANSI_BLACK = "\u001B[30m";

    private static final Object syncObj = new Object();
public static String[] convertToStrings(byte[][] byteStrings) {
    String[] data = new String[byteStrings.length];
    for (int i = 0; i < byteStrings.length; i++) {
        data[i] = new String(byteStrings[i], Charset.defaultCharset());

    }
    return data;
}



    public static byte[] loadFile(String filename, byte[] file_bytes, int Dpacket_length, int client_port, int colour) {

        File file = new File(filename);
        file_bytes = new byte[(int) file.length()];
        //packets_needed = (file_bytes.length / Dpacket_length) + 1;
        PRINT("User " + client_port + " file length is: " + file.length() + " bytes", colour);

        try {
            FileInputStream stream = new FileInputStream(filename);
            stream.read(file_bytes);
            stream.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("User " + client_port + "\nError converting file into bytes.\n");
            System.exit(1);
        }
        return file_bytes;
    }

    public static byte[] get_packet(int PCKT_NO, int Dpacket_length, int detail_length, byte[] file_bytes) {
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

    public static void Send_Data(DatagramSocket serverSocket, byte[] packet_to_send, InetAddress IPAddress, int client_port) throws IOException {
        DatagramPacket sendPacket1 = new DatagramPacket(packet_to_send, packet_to_send.length, IPAddress, client_port);
        serverSocket.send(sendPacket1);
    }

    public static long get_checkSum(byte[] bytes_rec) {
        byte[] x = new byte[8];
        x = Arrays.copyOfRange(bytes_rec, 2, 10);

        ByteBuffer bx = ByteBuffer.wrap(x);
        long value = bx.getLong();
        return value;
    }

    public static int server_get_seq_no(byte[] bytes_rec) {
        byte[] xx = new byte[4];
        xx = Arrays.copyOfRange(bytes_rec, 1, 5);

        ByteBuffer by = ByteBuffer.wrap(xx);
        int seq_no = by.getInt();
        return seq_no;
    }

    public static void SaveFile(String NfileName, byte[] line) throws FileNotFoundException, IOException {
        FileOutputStream file = new FileOutputStream(NfileName);
        file.write(line);

        file.close();
    }

    public synchronized static void PRINT(String message, int color) {
        synchronized (syncObj) {

            switch (color) {
                case 0:
                    System.out.println(ANSI_RED + message + ANSI_RED);

                    break;
                case 1:
                    System.out.println(ANSI_PURPLE + message + ANSI_PURPLE);
                    break;
                case 2:
                    System.out.println(ANSI_BLUE + message + ANSI_BLUE);
                    break;
                case 3:
                    System.out.println(ANSI_GREEN + message + ANSI_GREEN);
                    break;
                case 4:
                    System.out.println(ANSI_YELLOW + message + ANSI_YELLOW);
                    break;
                case 5:
                    System.out.println(ANSI_CYAN + message + ANSI_CYAN);
                    break;
            }
        }
    }

    public static int get_seq_no(byte[] bytes_rec, int detail_length) {
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

    public static void copyArray(byte src[], byte dest[], int offset, int length) {
        for (int i = 0; i < length; i++) {
            dest[i + offset] = src[i];

        }
    }
    
  public static void copyArray1(byte src[], byte dest[], int offset, int length) {
        for (int i = 0; i < length; i++) {
            dest[i] = src[i+offset];

        }
    }
    public static void sendAck(DatagramSocket clientSocket, int expected_seqNo, String IP, int NClientSocket) throws UnknownHostException, IOException {

        InetAddress IPAddress = InetAddress.getByName(IP);
        byte[] Ack = new byte[5];
        Ack[0] = 1;

        //we must send the sequence no of the packet
        ByteBuffer bz = ByteBuffer.allocate(4);
        bz.putInt(expected_seqNo);

        byte seq_noBytes[] = new byte[4];
        seq_noBytes = bz.array();
        copyArray(seq_noBytes, Ack, 1, 4);

        DatagramPacket sendPacket = new DatagramPacket(Ack, Ack.length, IPAddress, NClientSocket);
        clientSocket.send(sendPacket);
        
    }

}
