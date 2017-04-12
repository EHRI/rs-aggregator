package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.http.Result;
import nl.knaw.dans.rs.aggregator.http.Testing;
import nl.knaw.dans.rs.aggregator.xml.Capability;
import nl.knaw.dans.rs.aggregator.xml.ResourceSyncContext;
import nl.knaw.dans.rs.aggregator.xml.RsRoot;
import nl.knaw.dans.rs.aggregator.xml.SitemapItem;
import nl.knaw.dans.rs.aggregator.xml.Sitemapindex;
import nl.knaw.dans.rs.aggregator.xml.UrlItem;
import nl.knaw.dans.rs.aggregator.xml.Urlset;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeTrue;

/**
 * Created on 2017-04-10 13:02.
 */
public class LiveRsDocumentReaderTest {

  private static ResourceSyncContext rsContext;
  private static CloseableHttpClient httpclient;
  private static String zandbak_url = "http://zandbak11.dans.knaw.nl";
  // the capabilitylist url at this site has a resourcelist-index

  @BeforeClass
  public static void initialize() throws Exception {
    assumeTrue(Testing.LIVE_TESTS);
    httpclient = HttpClients.createDefault();
    rsContext = new ResourceSyncContext();
  }

  @Test
  public void testRead() throws Exception {
    RsDocumentReader reader = new RsDocumentReader(httpclient, rsContext);
    URI uri = URI.create(zandbak_url + "/ehri2/mdx/capabilitylist.xml");
    Result<RsRoot> result = reader.read(uri);
    assertThat(result.getStatusCode(), is(200));
    assertThat(result.getContent().isPresent(), is(true));
    RsRoot document = result.getContent().orElse(null);
    assertThat(document.getCapability().isPresent(), is(true));
    assertThat(document.getCapability().get(), is(Capability.CAPABILITYLIST));

    assertThat(reader.getLatestUrlset().isPresent(), is(true));
    assertThat(reader.getLatestSitemapindex().isPresent(), is(false));

    Urlset capabilitylist = reader.getLatestUrlset().get();
    List<UrlItem> itemList = capabilitylist.getItemList();
    itemList.forEach(urlItem -> System.out.println(urlItem.getLoc()));

    for (UrlItem urlItem : capabilitylist.getItemList()) {
      reader.read(urlItem.getLoc());

      if (reader.getLatestSitemapindex().isPresent()) {
        Sitemapindex rsIndex = reader.getLatestSitemapindex().get();
        for (SitemapItem sitemapItem : rsIndex.getItemList()) {
          System.out.println("\t" + sitemapItem.getLoc());
          reader.read(sitemapItem.getLoc());
          assertThat(reader.getLatestUrlset().isPresent(), is(true));
          assertThat(reader.getLatestSitemapindex().isPresent(), is(false));
        }
      }
    }
  }

  @Test
  public void testReadNoContent() throws Exception {
    RsDocumentReader reader = new RsDocumentReader(httpclient, rsContext);
    URI uri = URI.create(zandbak_url + "/ehri2");
    Result<RsRoot> result = reader.read(uri);
  }
}
