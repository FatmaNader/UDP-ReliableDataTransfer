/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client_server1;

public class Client_Server implements Runnable {

    static Method method;
    static int portNumber;
    Thread thread;

    @Override
    public void run() {
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    public enum Method {
        StopAndWait,
        SR,
        GBN;
    }
}
