package nl.knaw.dans.rs.aggregator.schedule;

/**
 * A job can be executed.
 */
public interface Job {

  void execute() throws Exception;

}
