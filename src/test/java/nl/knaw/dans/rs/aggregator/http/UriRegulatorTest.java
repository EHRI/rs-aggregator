package nl.knaw.dans.rs.aggregator.http;

import org.junit.Test;

import java.net.URI;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Created on 2017-04-18 10:41.
 */
public class UriRegulatorTest {

  @Test
  public void testRegulate() throws Exception {
    String[][] expectations = {
      {"http://www.ZANdbak02.dans.KNAW.nl/ehri2/..///mdy//capabilitylist.xml", "http://zandbak02.dans.knaw.nl/mdy/capabilitylist.xml"},
      {"http://www.ZANdbak02.dans.KNAW.nl/ehri2/..///mdy//capabilitylist.xml#abc", "http://zandbak02.dans.knaw.nl/mdy/capabilitylist.xml"},
      {"http://example.com", "http://example.com"},
      {"foo", "foo"},
      {"urn:nbn:nl:ui:13-k6k2-ih", "urn:nbn:nl:ui:13-k6k2-ih"},
      {null, null},
      {"", ""}
    };

    for (String[] expectation : expectations) {
      Optional<URI> maybeURI = UriRegulator.regulate(expectation[0]);
      String regulated = null;
      if (maybeURI.isPresent()) regulated = maybeURI.get().toString();
      System.out.println(expectation[0] + " -> " + expectation[1]);
      assertThat(regulated, equalTo(expectation[1]));
    }
  }
}
