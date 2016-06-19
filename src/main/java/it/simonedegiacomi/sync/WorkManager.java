package it.simonedegiacomi.sync;

import it.simonedegiacomi.goboxapi.client.GBClient;
import it.simonedegiacomi.goboxclient.GoBoxEnvironment;
import it.simonedegiacomi.goboxclient.GoBoxFacade;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

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
    private final ThreadPoolExecutor executor;

    private WorkStateListener workStateListener;

    /**
     * Employee that do the works. This employee is thread-safe so one in enough
     */
    private final Employee employee;

    public WorkManager(GoBoxEnvironment env, int threads){
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
        employee = new Employee(env.getClient(), env.getSync(), this);
    }

    /**
     * Add a new work to the queue
     * @param newWork Work to add
     */
    public void addWork(Work newWork) {
        executor.submit(() -> {
            log.info("running work " + newWork);

            // Call the listener
            workStateListener.onStartWork(newWork);

            if (!employee.submit(newWork)) {
                log.warn("work failed " + newWork);
                workStateListener.onFailWork(newWork);
            }

            // Call the listener
            log.info("work completed " + newWork);
            workStateListener.onCompleteWork(newWork);
        });
    }

    /**
     * Stop all the works immediately
     */
    public void shutdown () {
        executor.shutdownNow();
    }

    /**
     * Return the size of the queue
     * @return Number of queued works
     */
    public int getQueueSize () {
        return executor.getQueue().size();
    }

    public void setWorkStateListener(WorkStateListener workStateListener) {
        this.workStateListener = workStateListener;
    }

    public interface WorkStateListener {
        public void onStartWork (Work startedWork);
        public void onFailWork (Work startedWork);
        public void onCompleteWork (Work startedWork);
    }
}