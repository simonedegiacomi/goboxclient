package it.simonedegiacomi.storage.direct;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.CountDownLatch;

/**
 * Created on 19/03/16.
 * @author Degiacomi Simone
 */
public class UDPStorageServer extends Thread {

    public final static int DEFAULT_PORT = 4513;
    private final static int DEFAULT_RECEIVE_TIMEOUT = 500;

    private DatagramSocket socket;

    private int goBoxStoragePort;

    private CountDownLatch shutdown;

    private volatile boolean stop;

    private final InetAddress listenAddress;

    private final int UDPPort;

    /**
     * Create a new udp server
     * @param UDPPort Port to which this server will listen
     * @param goBoxStorage Port of the https server
     */
    public UDPStorageServer (int UDPPort, int goBoxStorage) throws UnknownHostException {
        this.goBoxStoragePort = goBoxStorage;
        this.UDPPort = UDPPort;
        this.listenAddress = InetAddress.getByName("0.0.0.0");
    }

    /**
     * Create a new udp server that will listen at the default port.
     * @param goBoxStorage Port of the https server
     */
    public UDPStorageServer (int goBoxStorage) throws UnknownHostException {
        this(DEFAULT_PORT, goBoxStorage);
    }

    /**
     * Initialize the server. this method must be called before the run method
     */
    public void init () throws SocketException {
        socket = new DatagramSocket(UDPPort, listenAddress);
        socket.setBroadcast(true);
        socket.setSoTimeout(DEFAULT_RECEIVE_TIMEOUT);
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
        if (shutdown != null)
            shutdown.await();
        socket.disconnect();
        socket.close();
    }
}