package nl.knaw.dans.rs.aggregator.util;


public class LambdaUtil {

  @FunctionalInterface
  public interface Function_WithExceptions<T, R, E extends Exception> {
    R apply(T val) throws E;
  }
}
