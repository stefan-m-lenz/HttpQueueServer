package de.imbei.httpqueueserver;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author lenzstef
 */
public class CleanUpTask {
    
    private final RequestManager requestManager = RequestManager.getInstance();
    
    public CleanUpTask() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        Runnable task = () -> requestManager.cleanUp();

        // This will schedule the task to run after 10 seconds delay
        executor.schedule(task, 10, TimeUnit.SECONDS);

        // If you want to run a task periodically, you can use scheduleAtFixedRate or scheduleWithFixedDelay
        // This will schedule the task to run initially after 10 seconds, and then every 5 seconds
        executor.scheduleAtFixedRate(task, 10, 5, TimeUnit.SECONDS);
    }    
    
}
