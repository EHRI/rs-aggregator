package nl.knaw.dans.rs.aggregator.sync;

import java.io.File;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created on 2017-05-10 10:19.
 */
public class TestTest {

  public static void main(String[] args) throws Exception {
    //fixedRate();
    //fixedDelay();
    computestart();
  }

  private static void fixedDelay() {
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    Runnable task = () -> {
      try {
        System.out.println("Scheduling: " + System.nanoTime());
        TimeUnit.SECONDS.sleep(2);
        System.out.println("Ready:      " +  System.nanoTime());
      }
      catch (InterruptedException e) {
        System.err.println("task interrupted");
      }
    };

    executor.scheduleWithFixedDelay(task, 0, 4, TimeUnit.SECONDS);
  }

  private static void fixedRate() throws Exception {
    // starts immediately after the task has run
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    Runnable task = () -> {
      System.out.println("Scheduling: " + System.nanoTime() + " " + ZonedDateTime.now());
      try {
        TimeUnit.SECONDS.sleep(1);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      System.out.println("Ready:      " +  System.nanoTime() + " " + ZonedDateTime.now());
    };

    int initialDelay = 0;
    int period = 3;
    executor.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.SECONDS);

    ScheduledExecutorService watch = Executors.newScheduledThreadPool(1);
    Runnable watcher = () -> {
      if (new File("stop").exists()) {
        System.out.println("Stopping executor because file 'stop' exists.");
        executor.shutdown();
        watch.shutdown();
      }
    };

    watch.scheduleWithFixedDelay(watcher, 1, 1, TimeUnit.SECONDS);
  }

  private static void computestart() {
    int startHour = 14;
    int startMinute = 10;
    ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
    ZonedDateTime start = now.withHour(startHour).withMinute(startMinute).withSecond(0).withNano(0);
    if (start.isBefore(now)) start = start.plusDays(1);

    long initialDelay = ChronoUnit.MINUTES.between(now, start);
    System.out.println(now);
    System.out.println(start);
    System.out.println(initialDelay + " -> " + initialDelay/60);
  }
}
