package it.simonedegiacomi.sync;

import it.simonedegiacomi.goboxapi.client.Client;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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
     * List of running works
     */
    private final Set<Work> currentWorks = new HashSet<>();

    /**
     * List of failed works
     */
    private final List<Work> failedWorks = new LinkedList<>();

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
        executor.submit(new Runnable() {
            @Override
            public void run() {

                // Add to the crrent works set
                currentWorks.add(newWork);

                // Run the work
                newWork.getWork(client).run();

                // Remove from the set
                currentWorks.remove(newWork);

                // Get the state
                Work.WorkState state = newWork.getState();

                // If it's failed, add to the list
                if (state == Work.WorkState.FAILED)
                    failedWorks.add(newWork);
            }
        });
    }

    /**
     * Stop all the works immediately
     */
    public void shutdown () {
        executor.shutdownNow();
    }

    /**
     * Return the set with the running works
     * @return Set of current works
     */
    public Set<Work> getCurrentWorks () {
        return currentWorks;
    }
}