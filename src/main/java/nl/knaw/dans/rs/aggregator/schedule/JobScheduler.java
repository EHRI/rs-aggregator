package nl.knaw.dans.rs.aggregator.schedule;

/**
 * A JobScheduler is capable of repeatedly executing a {@link Job} according to a schedule.
 */
public interface JobScheduler {

  void schedule(Job job) throws Exception;

}
