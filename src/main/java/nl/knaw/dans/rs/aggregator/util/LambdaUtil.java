package nl.knaw.dans.rs.aggregator.util;

/**
 * Extended functions.
 */
public class LambdaUtil {

  /**
   * {@link java.util.function.Function} that may throw Exceptions.
   * @param <T> the type of the argument to the function
   * @param <R> the type of the result of the function
   * @param <E> the type of exception that may be thrown
   */
  @FunctionalInterface
  public interface Function_WithExceptions<T, R, E extends Exception> {
    R apply(T val) throws E;
  }

  /**
   * {@link java.util.function.BiFunction} that may throw Exceptions.
   * @param <T> the type of the first argument to the function
   * @param <U> the type of the second argument to the function
   * @param <R> the type of the result of the function
   * @param <E> the type of exception that may be thrown
   */
  @FunctionalInterface
  public interface BiFunction_WithExceptions<T, U, R, E extends Exception> {
    R apply(T val1, U val2) throws E;
  }
}
