/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client_server1;

 public class packet {

        public int seq_no;
        public long time;
        public boolean isAck;
        public byte[] packet;

        public packet(int seq_no, byte[] packet) {
            this.seq_no = seq_no;
            this.isAck = false;
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


        public boolean isIsAck() {
            return isAck;
        }

        public void setIsAck(boolean isAck) {
            this.isAck = isAck;
        }

    }
