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
 * A {@link JobScheduler}  that executes its {@link Job} repeatedly at a fixed time.
 * <p>
 *   The RunAtFixedRate JobScheduler works with minute precision. You can set an initial start time
 *   with {@link RunAtFixedRate#setHourOfDay(int)} and {@link RunAtFixedRate#setMinuteOfHour(int)}.
 *   The lapse of time between successive runs of the submitted Job can be set
 *   with {@link RunAtFixedRate#setPeriod(int)}.
 * </p>
 * <p>In the following examples <code>myJob</code> is an instance of a {@link Job} implementation.</p>
 * <p>
 *   Example: Run a Job every 10 minutes.
 *   <pre>
 *     RunAtFixedRate runAtFixedRate = new RunAtFixedRate();
 *     runAtFixedRate.setPeriod(10);
 *     runAtFixedRate.schedule(myJob);
 *   </pre>
 *   This wil execute <code>myJob</code> on every multiple of 10 minutes, starting from the next multiple of
 *   10 minutes past the whole hour. This is because in our code snippet we didn't touch start time and so initial
 *   start time was counted from 00.00h. If the execution of <code>myJob</code> takes longer then the 10-minute
 *   period, the next execution will take place immediately after the ending of the previous one.
 * </p>
 * <p>
 *   Example: Run a Job at 06.42 every day.
 *   <pre>
 *     RunAtFixedRate runAtFixedRate = new RunAtFixedRate();
 *     runAtFixedRate.setHourOfDay(6);
 *     runAtFixedRate.setMinuteOfHour(42);
 *     runAtFixedRate.setPeriod(24 * 60);
 *     runAtFixedRate.schedule(myJob);
 *   </pre>
 *   This will execute <code>myJob</code> every day at 06.42, starting from the first occasion counted from now.
 * </p>
 * <p>
 *   The scheduler can be stopped gracefully by creating a file named 'stop' in the working directory.
 *   Upon detection, a currently executing job will be left to finish first, after which the scheduler will stop.
 * </p>
 *
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
        logger.info("Stopped application at synchronisation run #{}, because file named 'cfg/stop' was found.",
          runCounter);
      } else {
        logger.info("delay={}", scheduledFuture.getDelay(TimeUnit.MINUTES));
        logger.info("# touch cfg/stop # - to stop this service gracefully.");
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
      if (new File("cfg/stop").exists()) {
        stop = true;
        logger.info("Stopping scheduler after job execution #{}, because file named 'cfg/stop' was found.",
          runCounter);
        scheduler.shutdown();
        watch.shutdown();
      }
    };

    watch.scheduleWithFixedDelay(watcher, 0, 1, TimeUnit.SECONDS);
  }
}
