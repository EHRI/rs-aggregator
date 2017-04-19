package nl.knaw.dans.rs.aggregator.discover;

import nl.knaw.dans.rs.aggregator.http.Result;
import nl.knaw.dans.rs.aggregator.http.Testing;
import nl.knaw.dans.rs.aggregator.xml.Capability;
import nl.knaw.dans.rs.aggregator.xml.ResourceSyncContext;
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
 * Created on 2017-04-17 15:32.
 */
public class LiveRsExplorerTest {

  private static ResourceSyncContext rsContext;
  private static CloseableHttpClient httpclient;

  @BeforeClass
  public static void initialize() throws Exception {
    assumeTrue(Testing.LIVE_TESTS);
    httpclient = HttpClients.createDefault();
    rsContext = new ResourceSyncContext();
  }

  @Test
  public void testDirections() throws Exception {
    RsExplorer rsExplorer = new RsExplorer(httpclient,rsContext)
      .withFollowChildLinks(false)
      .withFollowIndexLinks(false)
      .withFollowParentLinks(false);

    URI uri = URI.create("http://zandbak11.dans.knaw.nl/.well-known/resourcesync");
    ResultIndex index = rsExplorer.explore(uri);
    assertThat(index.getCount(), is(1));
    assertThat(index.contains(uri), is(true));

    rsExplorer = rsExplorer
      .withFollowChildLinks(true)
      .withFollowIndexLinks(true)
      .withFollowParentLinks(true);

    index = rsExplorer.explore(uri);
    assertThat(index.getCount() > 1, is(true));
    assertThat(index.contains(uri), is(true));
  }

  @Test
  public void testSetOfResources() throws Exception {
    URI uri = URI.create("http://zandbak11.dans.knaw.nl/ehri2/mdx/capabilitylist.xml");
    RsExplorer rsExplorer = new RsExplorer(httpclient,rsContext)
      .withFollowChildLinks(true)
      .withFollowIndexLinks(false)
      .withFollowParentLinks(false);

    ResultIndex index = rsExplorer.explore(uri);
    assertThat(index.contains(uri), is(true));

    UrlSetPivot rip = new UrlSetPivot(index);
    System.out.println("RESOURCELISTS");
    List<Result<Urlset>> resourcelists = rip.listUrlsetResults(Capability.RESOURCELIST);
    resourcelists.stream().map(Result::getUri).forEach(System.out::println);
    System.out.println("CHANGELISTS");
    List<Result<Urlset>> changelists = rip.listUrlsetResults(Capability.CHANGELIST);
    changelists.stream().map(Result::getUri).forEach(System.out::println);
  }
}
