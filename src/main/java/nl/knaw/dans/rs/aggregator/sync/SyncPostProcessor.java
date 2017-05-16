package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.discover.ResultIndex;

/**
 * Created on 2017-05-15 16:08.
 */
public interface SyncPostProcessor {

  void postProcess(PathFinder pathFinder, SyncProperties syncProps, ResultIndex resultIndex) throws Exception;
}
