package nl.knaw.dans.rs.aggregator.http;

import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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

  @Test
  public void testCompareTo() throws Exception {
    Result<?> r0 = new Result<>(URI.create("http://example.com/resourcelist_0000.xml"));
    Result<?> r1 = new Result<>(URI.create("http://example.com/resourcelist_0001.xml"));
    Result<?> r2 = new Result<>(URI.create("http://example.com/resourcelist_0002.xml"));
    Result<?> r3 = new Result<>(null);

    assertThat(r0.compareTo(r1), is(-1));
    assertThat(r1.compareTo(r2), is(-1));
    assertThat(r2.compareTo(r3), is(-1));
    assertThat(r3.compareTo(r3), is(0));
    assertThat(r2.compareTo(r2), is(0));
    assertThat(r3.compareTo(r2), is(1));
    assertThat(r2.compareTo(r1), is(1));
    assertThat(r1.compareTo(r0), is(1));

    List<Result<?>> results = new ArrayList<>();
    results.add(r3);
    results.add(r1);
    results.add(r2);
    results.add(r0);

    results.stream().sorted().map(Result::getUri).forEach(System.out::println);
  }
}
