package nl.knaw.dans.rs.aggregator.schedule;

/**
 * Created on 2017-05-08 16:14.
 */
public interface JobScheduler {

  void schedule(Job job) throws Exception;

}
