package nl.knaw.dans.rs.aggregator.util;

import nl.knaw.dans.rs.aggregator.http.Testing;
import nl.knaw.dans.rs.aggregator.util.NormURI;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import java.net.URI;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assume.assumeTrue;

/**
 * Created on 2017-04-18 10:41.
 */
public class NormURITest {

  @Test
  public void testNormalize() throws Exception {
    String[][] expectations = {
      {"http://www.ZANdbak02.dans.KNAW.nl/ehri2/..///mdy//capabilitylist.xml", "http://zandbak02.dans.knaw.nl/mdy/capabilitylist.xml"},
      {"http://www.ZANdbak02.dans.KNAW.nl/ehri2/..///mdy//capabilitylist.xml#abc", "http://zandbak02.dans.knaw.nl/mdy/capabilitylist.xml"},
      {"http://example.com", "http://example.com"},
      {"foo", "foo"},
      {"urn:nbn:nl:ui:13-k6k2-ih", "urn:nbn:nl:ui:13-k6k2-ih"},
      {null, null},
      {"", ""},
      {"http://Publisher-Connector.CORE.ac.uk/resourcesync/data/elsevier/pdf/000/aHR0cDovL2FwaS5lbHNldmllci5jb20vY29udGVudC9hcnRpY2xlL3BpaS8wMDE0NTc5MzkwODA1MTdt.pdf",
        "http://publisher-connector.core.ac.uk/resourcesync/data/elsevier/pdf/000/aHR0cDovL2FwaS5lbHNldmllci5jb20vY29udGVudC9hcnRpY2xlL3BpaS8wMDE0NTc5MzkwODA1MTdt.pdf"},
      //{"http://www.düsseldorf.de/resources", "http://düsseldorf.de/resources"},
      //{"http://fußball.de/resources", "http://fußball.de/resources"}
    };

    for (String[] expectation : expectations) {
      Optional<URI> maybeURI = NormURI.normalize(expectation[0]);
      String normalized = null;
      if (maybeURI.isPresent()) normalized = maybeURI.get().toString();
      //System.out.println(expectation[0] + " -> " + expectation[1] + " -->> " + normalized);
      assertThat(normalized, equalTo(expectation[1]));
    }
  }

  @Test
  public void testLive() throws Exception {
    assumeTrue(Testing.LIVE_TESTS);
    CloseableHttpClient httpClient = HttpClients.createDefault();
    String link = "http://publisher-connector.core.ac.uk/resourcesync/data/elsevier/pdf/000/aHR0cDovL2FwaS5lbHNldmllci5jb20vY29udGVudC9hcnRpY2xlL3BpaS8wMDE0NTc5MzkwODA1MTdt.pdf";
    URI uri = NormURI.normalize(link).orElse(null);

    System.out.println("link=" + link);
    System.out.println(" uri=" + uri);
    HttpGet request = new HttpGet(uri);
    CloseableHttpResponse response = httpClient.execute(request);

    assertThat(response.getStatusLine().getStatusCode(), is(200));
  }
}
