package nl.knaw.dans.rs.aggregator.main;

import nl.knaw.dans.rs.aggregator.sync.RunScheduler;
import nl.knaw.dans.rs.aggregator.sync.SyncMaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * Created on 2017-05-04 16:47.
 */
public class SyncApp {

  public static final String APP_CONTEXT_LOCATION = "cfg/syncapp-context.xml";
  public static final String BN_SYNC_MASTER = "sync-master";
  public static final String BN_RUN_SCHEDULER = "run-scheduler";

  private static Logger logger = LoggerFactory.getLogger(SyncApp.class);

  public static void main(String[] args) throws Exception {
    String appContextLocation;
    if (args.length > 0) {
      appContextLocation = args[0];
    } else {
      appContextLocation = APP_CONTEXT_LOCATION;
    }
    logger.info("Configuration file: {}", appContextLocation);

    try (FileSystemXmlApplicationContext applicationContext = new FileSystemXmlApplicationContext(appContextLocation)) {

      RunScheduler scheduler = (RunScheduler) applicationContext.getBean(BN_RUN_SCHEDULER);
      SyncMaster syncMaster = (SyncMaster) applicationContext.getBean(BN_SYNC_MASTER);
      scheduler.setSyncMaster(syncMaster);
      scheduler.start();

    } catch (Exception e) {
      logger.error("Last capture caught: ", e);
      throw e;
    }

  }

}
