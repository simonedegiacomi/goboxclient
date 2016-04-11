package it.simonedegiacomi.storage.direct;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.CountDownLatch;

/**
 * Created by simone on 19/03/16.
 */
public class UDPStorageServer extends Thread {

    public final static int DEFAULT_PORT = 4513;
    private final static int DEFAULT_RECEIVE_RIMEOUT = 500;

    private DatagramSocket socket;

    private int goBoxStoragePort;

    private CountDownLatch shutdown;

    private volatile boolean stop;

    public UDPStorageServer (int UDPPort, int goBoxStorage) throws UnknownHostException, SocketException {
        this.goBoxStoragePort = goBoxStorage;
        socket = new DatagramSocket(UDPPort, InetAddress.getByName("0.0.0.0"));
        socket.setBroadcast(true);
        socket.setSoTimeout(DEFAULT_RECEIVE_RIMEOUT);
    }

    public UDPStorageServer (int goBoxStorage) throws UnknownHostException, SocketException {
        this(DEFAULT_PORT, goBoxStorage);
    }

    @Override
    public void run () {

        shutdown = new CountDownLatch(1);
        stop = false;

        byte[] answerBytes = ("GOBOX_DIRECT_PORT:" + goBoxStoragePort).getBytes();
        byte[] inBuffer = new byte[2048];

        while(!stop) {
            DatagramPacket packet = new DatagramPacket(inBuffer, inBuffer.length);


            try {
                socket.receive(packet);

                if(!packet.getData().toString().trim().equals("GOBOX_DIRECT"))
                    continue;

                DatagramPacket answer = new DatagramPacket(answerBytes, answerBytes.length, packet.getAddress(), packet.getPort());
                socket.send(answer);
            } catch (SocketTimeoutException ex) {
                // ok
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        shutdown.countDown();
    }

    public void shutdown() throws InterruptedException {
        stop = true;
        shutdown.await();
        socket.close();
    }
}