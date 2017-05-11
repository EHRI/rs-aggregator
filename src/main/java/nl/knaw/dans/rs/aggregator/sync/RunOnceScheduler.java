package nl.knaw.dans.rs.aggregator.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created on 2017-05-08 16:22.
 */
public class RunOnceScheduler implements RunScheduler {

  private static Logger logger = LoggerFactory.getLogger(RunOnceScheduler.class);

  private SyncMaster syncMaster;

  @Override
  public void setSyncMaster(SyncMaster syncMaster) {
    this.syncMaster = syncMaster;
  }

  @Override
  public void start() throws Exception {
    logger.info("Starting one-time synchronization run.");
    syncMaster.readListAndSynchronize();
    logger.info("End of one-time synchronization run.");
  }
}
