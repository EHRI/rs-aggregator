package nl.knaw.dans.rs.aggregator.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created on 2017-05-08 16:34.
 */
public class RunAtFixedRate implements RunScheduler {

  private static Logger logger = LoggerFactory.getLogger(RunAtFixedRate.class);

  private int runCounter;
  private int errorCounter;
  private int maxErrorCount = 3;
  private SyncMaster syncMaster;
  private int period = 60;
  private int startHour;
  private int startMinute;
  private boolean stop;
  private ZonedDateTime next;

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

  public int getPeriod() {
    return period;
  }

  public void setPeriod(int period) {
    this.period = period;
  }

  public int getStartHour() {
    return startHour;
  }

  public void setStartHour(int startHour) {
    this.startHour = startHour;
  }

  public int getStartMinute() {
    return startMinute;
  }

  public void setStartMinute(int startMinute) {
    this.startMinute = startMinute;
  }

  @Override
  public void start() throws Exception {
    logger.info("Started {}", this.getClass().getName());

    ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
    ZonedDateTime start = now.withHour(startHour).withMinute(startMinute).withSecond(0).withNano(0);
    if (start.isBefore(now)) start = start.plusDays(1);

    long initialDelay = ChronoUnit.MINUTES.between(now, start);
    next = start;
    logger.info("######### Starting synchronisation at {}", next);

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    Runnable syncer = () -> {
      runCounter++;
      next = next.plusMinutes(period);
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
        logger.info("Next synchronisation will start at {}", next);
      }
    };
    scheduler.scheduleAtFixedRate(syncer, initialDelay, period, TimeUnit.MINUTES);

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
