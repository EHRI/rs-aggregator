package nl.knaw.dans.rs.aggregator.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JobScheduler that runs a given {@link Job} once.
 */
public class RunOnceScheduler implements JobScheduler {

  private static Logger logger = LoggerFactory.getLogger(RunOnceScheduler.class);

  @Override
  public void schedule(Job job) throws Exception {
    logger.info("Starting one-time job execution of {}.", job.getClass().getName());
    job.execute();
    logger.info("End of one-time job execution of {}.", job.getClass().getName());
  }
}
