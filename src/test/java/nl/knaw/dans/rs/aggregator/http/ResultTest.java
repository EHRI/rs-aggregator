package nl.knaw.dans.rs.aggregator.http;

import org.junit.Test;

import java.net.URI;

/**
 * Created on 2017-04-16 15:26.
 */
public class ResultTest {

  @Test
  public void testToString() throws Exception {
    URI uri = URI.create("http://example.com/testing/resulttest.txt");
    Result<?> result = new Result(uri);
    result.addError(new RuntimeException("bla"));
    result.addError(new IllegalArgumentException("Do not *@!"));

    //System.out.println(result);
  }
}
