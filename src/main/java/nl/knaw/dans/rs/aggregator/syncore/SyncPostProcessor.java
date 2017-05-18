package nl.knaw.dans.rs.aggregator.syncore;

import nl.knaw.dans.rs.aggregator.discover.ResultIndex;
import nl.knaw.dans.rs.aggregator.syncore.PathFinder;
import nl.knaw.dans.rs.aggregator.util.RsProperties;

/**
 * Implementations of SyncPostProcessor are called to post process after a synchronisation run.
 */
public interface SyncPostProcessor {

  /**
   * Post process after the synchronisation that was done with the given resultIndex and pathFinder and that
   * resulted in the given syncProps.
   *
   * @param resultIndex the resultIndex used in synchronisation
   * @param pathFinder the pathFinder that was used in the synchronisation
   * @param syncProps synchronisation properties that were the result of the synchronisation
   * @throws Exception the SyncPostProcessor may throw exceptions
   */
  void postProcess(ResultIndex resultIndex, PathFinder pathFinder, RsProperties syncProps) throws Exception;
}
