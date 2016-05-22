package it.simonedegiacomi.sync;

import it.simonedegiacomi.goboxapi.client.GBClient;
import org.apache.log4j.Logger;

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
public class WorkManager {

    /**
     * Logger of the class
     */
    private final Logger log = Logger.getLogger(WorkManager.class);

    /**
     * Default number of concurrent threads
     */
    public final static int DEFAULT_THREADS = 4;

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
     * Employee that do the works. This employee is thread-safe so one in enough
     */
    private final Employee employee;

    public WorkManager(GBClient client, Sync sync, int threads){
        executor = Executors.newFixedThreadPool(threads);
        employee = new Employee(client, sync, this);
    }

    /**
     * Add a new work to the queue
     * @param newWork Work to add
     */
    public void addWork(Work newWork) {
        executor.submit(() -> {
            log.info("running work " + newWork);

            // Add to the current works set
            currentWorks.add(newWork);

            if (!employee.submit(newWork)) {
                log.warn("work failed " + newWork);
                failedWorks.add(newWork);
            }
            log.info("work completed " + newWork);
            currentWorks.remove(newWork);
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