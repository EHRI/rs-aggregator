package nl.knaw.dans.rs.aggregator.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created on 2017-05-09 13:12.
 */
public class RunWithFixedDelay implements RunScheduler {

  private static Logger logger = LoggerFactory.getLogger(RunWithFixedDelay.class);

  private int runCounter;
  private int errorCounter;
  private int maxErrorCount = 3;
  private SyncMaster syncMaster;
  private int delay = 60;
  private boolean stop;

  @Override
  public void setSyncMaster(SyncMaster syncMaster) {
    this.syncMaster = syncMaster;
  }

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
  public void start() throws Exception {
    logger.info("Started {}", this.getClass().getName());

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    Runnable syncer = () -> {
      runCounter++;
      logger.info("######### Starting synchronisation run #{}", runCounter);
      try {
        syncMaster.readListAndSynchronize();
      } catch (Exception e) {
        errorCounter++;
        logger.error("Premature end of synchronisation run #{}. error count={}", runCounter, errorCounter, e);
        if (errorCounter >= maxErrorCount) {
          logger.info("Stopping application because errorCount >= {}", maxErrorCount);
          System.exit(-1);
        }
      }
      logger.info("#########   End of synchronisation run #{}", runCounter);
      if (stop) {
        logger.info("######### Stopped application at synchronisation run #{}, because file named 'stop' was found.",
          runCounter);
      } else {
        logger.info("# touch stop - to stop this service gracefully.");
        logger.info("Next synchronisation will start in {} minutes.", delay);
      }
    };
    scheduler.scheduleWithFixedDelay(syncer, 0, delay, TimeUnit.MINUTES);

    // Watch the file system for a file named 'stop'
    ScheduledExecutorService watch = Executors.newScheduledThreadPool(1);
    Runnable watcher = () -> {
      if (new File("stop").exists()) {
        stop = true;
        logger.info("######### Stopping application at synchronisation run #{}, because file named 'stop' was found.",
          runCounter);
        scheduler.shutdown();
        watch.shutdown();
      }
    };

    watch.scheduleWithFixedDelay(watcher, 0, 1, TimeUnit.SECONDS);
  }
}
