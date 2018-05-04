package client_server1;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import org.apache.commons.lang3.ArrayUtils;
import java.util.zip.Checksum;

public class ClientGBN {

    public ClientGBN(String IP, int MainServer, int clientPort, String filename, int windowSize, DatagramSocket clientSocket) {
        this.IP = IP;
        this.MainServer = MainServer;
        this.clientPort = clientPort;
        this.filename = filename;
        this.windowSize = windowSize;
        this.clientSocket = clientSocket;

    }

    public static byte[] line = null;
    public static int packets_needed;
    public static int NClientSocket;
    public static int MainServer;
    public static int Dpacket_length = 500;                //the length of data in every packet
    public static int detail_length = 14;
    public static String IP;
    public static int clientPort;
    public static String filename;
    int windowSize;
    int color;
    static DatagramSocket clientSocket;

    public static void run() {
        try {
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            //define Ip an Socket
            InetAddress IPAddress = InetAddress.getByName(IP);

            byte[] receiveData = new byte[512];
            byte[] init = new byte[8];
            byte[] file_info = new byte[50];
            System.out.println("Welcome to Go back N client!");
            System.out.println("-------------------------------------");

            //get file name from user
            //String FileName = inFromUser.readLine();
            byte x = (byte) 2;
            file_info[0] = x;
            ClientServerUtils.copyArray(filename.getBytes(), file_info, 1, filename.getBytes().length);

            DatagramPacket sendPacket = new DatagramPacket(file_info, file_info.length, IPAddress, MainServer);
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
            get_file(clientSocket);

            System.out.println("The file is received successfully");
            System.out.print("->Please enter file name to save: ");
            String NFileName = inFromUser.readLine();

            ClientServerUtils.SaveFile(NFileName, line);
            System.out.println("----------YOUR FILE IS SAVED NOW!----------");
        } catch (SocketException ex) {
            Logger.getLogger(ClientGBN.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnknownHostException ex) {
            Logger.getLogger(ClientGBN.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ClientGBN.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void get_file(DatagramSocket clientSocket) throws IOException {
        int SequenceNum = 0;
        
        while (SequenceNum < packets_needed) {
             //recieve Packet 
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
                // if there is error SEND ACK with previous sequence no
                ClientServerUtils.sendAck(clientSocket, SequenceNum - 1, IP, NClientSocket);
                x = recieve_packet(clientSocket, SequenceNum);
                seq_no = x[0];
                expected_checkSum = x[1];
                curr_sheckSum = x[2];
            }
            //IF THERE IS NO ERROR SEND POSITIVE ACKNOWLEDGEMENT
            byte type = 1;
            ClientServerUtils.sendAck(clientSocket, SequenceNum, IP, NClientSocket);
            SequenceNum++;
        }
    }

    public static long[] recieve_packet(DatagramSocket clientSocket, int pckt_no) throws IOException {
        byte[] receiveData = new byte[Dpacket_length + detail_length];

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);

        byte bytes_rec[];
        bytes_rec = receivePacket.getData();

        long expected_checkSum = ClientServerUtils.get_checkSum(bytes_rec);
        int seq_no = ClientServerUtils.get_seq_no(bytes_rec, detail_length);
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
        short len = ClientServerUtils.get_dataLength(bytes_rec);

        //get data needed from the packet
        data_rec = Arrays.copyOfRange(bytes_rec, detail_length, len + detail_length);

        //get checkSum from the packet
        int seq_no = ClientServerUtils.get_seq_no(bytes_rec, detail_length);
        int seq = (seq_no);

        //calculate the current check sum
        ch.update(data_rec, 0, data_rec.length);
        long current_checksum = ch.getValue();

        System.out.println("Received packet with sequence number " + seq_no + " length " + len + " expected checksum " + expected_check
                + " and current checksum: " + current_checksum);
        System.out.println("-----------------------------------------------------------------------------------------------------------------");
        ch.reset();
        if (seq_no == expected_seq && current_checksum == expected_check) {
            line = ArrayUtils.addAll(line, data_rec);
        }
        return current_checksum;
    }
}
