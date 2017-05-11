package nl.knaw.dans.rs.aggregator.sync;

/**
 * Created on 2017-05-08 16:14.
 */
public interface RunScheduler {

  void setSyncMaster(SyncMaster syncMaster);

  void start() throws Exception;
}
