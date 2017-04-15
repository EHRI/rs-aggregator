package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.http.Result;
import nl.knaw.dans.rs.aggregator.http.Testing;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assume.assumeTrue;

/**
 * Created on 2017-04-12 16:20.
 */
public class LiveResourceReaderTest {

  private static CloseableHttpClient httpclient;

  @BeforeClass
  public static void initialize() throws Exception {
    assumeTrue(Testing.LIVE_TESTS);
    httpclient = HttpClients.createDefault();
  }

  @Test
  public void testRead() throws Exception {
    String[] urls = {
      //"https://hc.apache.org/httpcomponents-client-ga/examples.html",
      //"http://www.openarchives.org/rs/1.1/resourcesync#Introduction",
      //"http://rspub-gui.readthedocs.io",
      "http://zandbak11.dans.knaw.nl/ehri2/mdx/resourcelist_0000.xml"
    };

    ResourceReader rsReader = new ResourceReader(httpclient);
    rsReader.setKeepingHeaders(true);
    for (String url : urls) {
      File file = new File("target/test-output/live/some.xml");
      Result<File> result = rsReader.read(url, file);
      assertThat(result.getContent().isPresent(), is(true));
      assertThat(result.getContent().get().exists(), is(true));
      result.getHeaders().forEach((key, value) -> System.out.println(key + ": " + value));
    }
  }
}
