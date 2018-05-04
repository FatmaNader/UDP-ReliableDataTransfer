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

public class ClientSelectiveRpt {

    public static byte[] line = null;
    public static int packets_needed;
    public static int NClientSocket;
    public static int WellKnownServer;
    public static int Dpacket_length = 500;//the length of data in every packet
    public static int detail_length = 14;
    public static String IP;
    public static int clientPort;
    public static  String filename;
    public static int windowSize;
    static int color;
    static String name;
      static DatagramSocket clientSocket;

    public static ArrayList<packet> Packets = new ArrayList<>();

    public ClientSelectiveRpt(String IP, int WellKnownServer, int clientPort, String filename, int windowSize, DatagramSocket clientSocket) {
        this.IP = IP;
        this.WellKnownServer = WellKnownServer;
        this.clientPort = clientPort;
        this.filename = filename;
        this.windowSize = windowSize;
        this.clientSocket=clientSocket;

    }

    public static void run() {
        try {
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

            //clientSocket = new DatagramSocket(clientPort);

            //define Ip an Socket
            InetAddress IPAddress = InetAddress.getByName(IP);

            byte[] receiveData = new byte[512];
            byte[] init = new byte[8];
            byte [] file_info =new byte [50];
            System.out.println("Welcome to selective repeat client!");
            System.out.println("-------------------------------------");
            
             byte x = (byte) 3;
            file_info[0]= x;
            ClientServerUtils.copyArray(filename.getBytes(), file_info, 1, filename.getBytes().length);
            //get file name from user
            //String FileName = inFromUser.readLine();

            DatagramPacket sendPacket = new DatagramPacket(file_info, file_info.length, IPAddress, WellKnownServer);
            clientSocket.send(sendPacket);
            DatagramPacket receivePacket = new DatagramPacket(init, 
                    init.length);
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
                System.out.println("-----------------------------------------------------------------------------------------------------------------");
                System.out.println("-----------------------------------------------------------------------------------------------------------------");

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
        } catch (SocketException ex) {
            Logger.getLogger(ClientSelectiveRpt.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnknownHostException ex) {
            Logger.getLogger(ClientSelectiveRpt.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ClientSelectiveRpt.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void get_file(DatagramSocket clientSocket) throws IOException {
        init();
        int windowBase = -1;
        while (windowBase < packets_needed - 1) { // while all packets not recieved
            windowBase = recieve_packet(clientSocket, windowBase);
        }
    }

    public static class packet {

        public int seq_no;
        public boolean isRecieved;
        public byte[] data;

        public packet() {
            this.isRecieved = false;
        }

        public packet(int seq_no, byte[] data) {
            this.seq_no = seq_no;
            this.isRecieved = false;
            this.data = data;

        }

    }

    public static void init() {

        for (int i = 0; i < packets_needed; i++) {
            packet pkt = new packet();
            Packets.add(pkt);
        }
    }

    public static void SaveFile(String NfileName) throws FileNotFoundException, IOException {
        FileOutputStream file = new FileOutputStream(NfileName);

        for (int i = 0; i < packets_needed; i++) {
            byte[] x;
            x = Packets.get(i).data;
            line = ArrayUtils.addAll(line, x);
        }
        file.write(line);
        file.close();
    }

    public static int recieve_packet(DatagramSocket clientSocket, int WindowBase) throws IOException {
        byte[] receiveData = new byte[Dpacket_length + detail_length];

        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);

        byte bytes_rec[];
        byte data_rec[];
        bytes_rec = receivePacket.getData();
        Checksum ch = new CRC32();

        long expected_checkSum = ClientServerUtils.get_checkSum(bytes_rec);
        //System.out.println("EXPECTED CHECK SUM IS:" + expected_checkSum);
        int seq_no = ClientServerUtils.get_seq_no(bytes_rec, detail_length);

        int len = ClientServerUtils.get_dataLength(bytes_rec);
        data_rec = Arrays.copyOfRange(bytes_rec, detail_length, len + detail_length);
        InetAddress IPAddress = InetAddress.getByName(IP);
        ch.update(data_rec, 0, data_rec.length);
        long current_checksum = ch.getValue();

        //System.out.println("SEQUENCE NO : " +seq_no);
        if (seq_no > WindowBase && seq_no < WindowBase + windowSize && current_checksum == expected_checkSum) {

            Packets.get(seq_no).isRecieved = true;
            Packets.get(seq_no).data = data_rec;
            //System.out.println("Window base: " + (WindowBase+1)+ "           Window high: " + (WindowBase+windowSize));
            System.out.println("Received packet with sequence number " + seq_no + " length " + len + " expected checksum " + expected_checkSum
                    + " and current checksum: " + current_checksum);

            ClientServerUtils.sendAck(clientSocket, seq_no, IP, NClientSocket);

            if (seq_no == WindowBase + 1) {

                while (Packets.get(seq_no).isRecieved) {
                    WindowBase++;
                    seq_no++;
                    System.out.println("Window base: " + (WindowBase + 1) + "           Window high: " + (WindowBase + windowSize));
                    System.out.println("-----------------------------------------------------------------------------------------------------------------");
                    if (seq_no == packets_needed) {
                        break;
                    }
                }
            }
        } else if (current_checksum != expected_checkSum) {
            System.err.println("Packet corrupted!");
            System.out.println("Received packet with sequence number " + seq_no + " length " + len + " expected checksum " + expected_checkSum
                    + " and current checksum: " + current_checksum);
        }

        return WindowBase;
    }

}
