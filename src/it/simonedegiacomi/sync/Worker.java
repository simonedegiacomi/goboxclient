package it.simonedegiacomi.sync;

import it.simonedegiacomi.goboxapi.client.Client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Degiacomi Simone
 * Created on 17/02/16.
 */
public class Worker {

    public final static int DEFAULT_THREADS = 2;

    private final ExecutorService executor;

    private final Client client;

    public Worker(Client client, int threads){
        executor = Executors.newFixedThreadPool(threads);
        this.client = client;
    }

    public void addWork(Work newWork) {
        executor.submit(newWork.getWork(client));
    }

    public void stop () {
        executor.shutdownNow();
    }
}
