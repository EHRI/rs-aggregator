package nl.knaw.dans.rs.aggregator.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * A JobScheduler that executes its {@link Job} repeatedly at a fixed time.
 */
public class RunAtFixedRate implements JobScheduler {

  private static Logger logger = LoggerFactory.getLogger(RunAtFixedRate.class);

  private int runCounter;
  private int errorCounter;
  private int maxErrorCount = 3;
  private int period = 60;
  private int hourOfDay;
  private int minuteOfHour;
  private boolean stop;
  private ZonedDateTime next;
  private ScheduledFuture scheduledFuture;

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
    if (period < 1) {
      throw new IllegalArgumentException("Period cannot be less then 1 minute.");
    }
    this.period = period;
  }

  public int getHourOfDay() {
    return hourOfDay;
  }

  public void setHourOfDay(int hourOfDay) {
    this.hourOfDay = hourOfDay;
  }

  public int getMinuteOfHour() {
    return minuteOfHour;
  }

  public void setMinuteOfHour(int minuteOfHour) {
    this.minuteOfHour = minuteOfHour;
  }

  @Override
  public void schedule(Job job) throws Exception {
    logger.info("Started {} with job {}.", this.getClass().getName(), job.getClass().getName());

    final String home = new File(".").getAbsoluteFile().getParent();

    ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC).withSecond(0).withNano(0);
    ZonedDateTime start = now.withHour(hourOfDay).withMinute(minuteOfHour).withSecond(0).withNano(0);
    while (start.isBefore(now)) {
      start = start.plusMinutes(period);
    }

    long initialDelay = ChronoUnit.MINUTES.between(now, start);
    next = start;
    logger.info("Starting job execution in {} minutes at {}.", initialDelay, next);

    ScheduledFuture sfuture = null;
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    Runnable jobRunner = () -> {
      runCounter++;
      next = next.plusMinutes(period);
      logger.info(">>>>>>>>>> Starting job execution #{} on {}.", runCounter, job.getClass().getName());
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
      logger.info("<<<<<<<<<< End of job execution #{} on {}", runCounter, job.getClass().getName());
      if (stop) {
        logger.info("Stopped application at synchronisation run #{}, because file named 'stop' was found.",
          runCounter);
      } else {
        logger.info("delay={}", scheduledFuture.getDelay(TimeUnit.MINUTES));
        logger.info("# touch {}/stop # - to stop this service gracefully.", home);
        // in case execution takes longer then period, adjust next
        ZonedDateTime rightnow = ZonedDateTime.now(ZoneOffset.UTC).withSecond(0).withNano(0);
        if (next.isBefore(rightnow)) {
          long excess = ChronoUnit.MINUTES.between(next, rightnow);
          logger.warn("Job execution takes longer then period. Job execution exceeds {}-minute period with {} minutes.",
            period, excess);
        }
        //
        logger.info("Next job execution planned at {}", next);
      }
    };
    scheduledFuture = scheduler.scheduleAtFixedRate(jobRunner, initialDelay, period, TimeUnit.MINUTES);

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
