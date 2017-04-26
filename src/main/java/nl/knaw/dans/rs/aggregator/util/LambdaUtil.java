package nl.knaw.dans.rs.aggregator.util;


public class LambdaUtil {

  @FunctionalInterface
  public interface Function_WithExceptions<T, R, E extends Exception> {
    R apply(T val) throws E;
  }

  @FunctionalInterface
  public interface BiFunction_WithExceptions<T, U, R, E extends Exception> {
    R apply(T val1, U val2) throws E;
  }
}
