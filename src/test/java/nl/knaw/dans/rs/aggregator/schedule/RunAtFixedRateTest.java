package nl.knaw.dans.rs.aggregator.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created on 2017-05-14 13:36.
 */
public class RunAtFixedRateTest {

  private static Logger logger = LoggerFactory.getLogger(RunAtFixedRateTest.class);

  public static void main(String[] args) throws Exception {
    RunAtFixedRateTest test = new RunAtFixedRateTest();
    test.testStop();
  }

  public void testStop() throws Exception {
    RunAtFixedRate runAtFixedRate = new RunAtFixedRate();
    runAtFixedRate.setPeriod(1); //minute. will start immediately with period < 3
    runAtFixedRate.setHourOfDay(6);
    runAtFixedRate.setMinuteOfHour(42);
    TestJob job = new TestJob();
    job.executionTimes = new int[] {3*60, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10}; // seconds

    runAtFixedRate.schedule(job);
  }

  private class TestJob implements Job {

    int[] executionTimes;
    int count;

    @Override
    public void execute() throws Exception {
      logger.info("Start execution {}", System.nanoTime());
      TimeUnit.SECONDS.sleep(executionTimes[count]);
      count++;
      if (count >= executionTimes.length) count = 0;
      logger.info("End execution   {}", System.nanoTime());
    }
  }
}
