package it.simonedegiacomi.sync;

import it.simonedegiacomi.goboxapi.client.Client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Degiacomi Simone
 * Created on 17/02/16.
 */
public class Worker {

    /**
     * Default number of concurrent threads
     */
    public final static int DEFAULT_THREADS = 2;

    /**
     * Real java executor.
     * (yes, this is just another wrapper...)
     */
    private final ExecutorService executor;

    /**
     * Client used by the works
     */
    private final Client client;

    public Worker(Client client, int threads){
        executor = Executors.newFixedThreadPool(threads);
        this.client = client;
    }

    /**
     * Add a new work to the queue
     * @param newWork Work to add
     */
    public void addWork(Work newWork) {
        executor.submit(newWork.getWork(client));
    }

    /**
     * Stop all the works immediately
     */
    public void shutdown () {
        executor.shutdownNow();
    }
}