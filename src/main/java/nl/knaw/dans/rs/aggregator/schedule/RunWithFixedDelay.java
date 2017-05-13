package nl.knaw.dans.rs.aggregator.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A JobScheduler that executes its {@link Job} with a fixed delay between successive executions.
 */
public class RunWithFixedDelay implements JobScheduler {

  private static Logger logger = LoggerFactory.getLogger(RunWithFixedDelay.class);

  private int runCounter;
  private int errorCounter;
  private int maxErrorCount = 3;

  private int delay = 60;
  private boolean stop;

  public int getMaxErrorCount() {
    return maxErrorCount;
  }

  public void setMaxErrorCount(int maxErrorCount) {
    this.maxErrorCount = maxErrorCount;
  }

  public int getDelay() {
    return delay;
  }

  public void setDelay(int delay) {
    this.delay = delay;
  }

  @Override
  public void schedule(Job job) throws Exception {
    logger.info("Started {} with job {}", this.getClass().getName(), job.getClass().getName());

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    Runnable jobRunner = () -> {
      runCounter++;
      logger.info(">>>>>>>>>> Starting job execution #{} on {}", runCounter, job.getClass().getName());
      try {
        job.execute();
      } catch (Exception e) {
        errorCounter++;
        logger.error("Premature end of job execution #{}. error count={}", runCounter, errorCounter, e);
        if (errorCounter >= maxErrorCount) {
          logger.info("Stopping application because errorCount >= {}", maxErrorCount);
          System.exit(-1);
        }
      }
      logger.info("<<<<<<<<<<   End of job execution #{} on {}", runCounter, job.getClass().getName());
      if (stop) {
        logger.info("Stopped application at job execution #{}, because file named 'stop' was found.",
          runCounter);
      } else {
        logger.info("# touch stop - to stop this service gracefully.");
        logger.info("Next job execution will start in {} minutes.", delay);
      }
    };
    scheduler.scheduleWithFixedDelay(jobRunner, 0, delay, TimeUnit.MINUTES);

    // Watch the file system for a file named 'stop'
    ScheduledExecutorService watch = Executors.newScheduledThreadPool(1);
    Runnable watcher = () -> {
      if (new File("stop").exists()) {
        stop = true;
        logger.info("Stopping scheduler after job execution #{}, because file named 'stop' was found.",
          runCounter);
        scheduler.shutdown();
        watch.shutdown();
      }
    };

    watch.scheduleWithFixedDelay(watcher, 0, 1, TimeUnit.SECONDS);
  }
}
