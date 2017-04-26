package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.discover.ResultIndex;
import nl.knaw.dans.rs.aggregator.http.Testing;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeTrue;

/**
 * Created on 2017-04-26 10:43.
 */
public class LiveSitemapCollectorTest {

  private static String baseDirectory = "target/test-output/synchronizer";
  private static String capabilityListUrl = "http://zandbak11.dans.knaw.nl/ehri2/mdx/capabilitylist.xml";

  @BeforeClass
  public static void initialize() throws Exception {
    //assumeTrue(Testing.LIVE_TESTS);
  }

  @Test
  public void testCollectSitemaps() throws Exception {
    PathFinder pathFinder = new PathFinder(baseDirectory, URI.create(capabilityListUrl));
    SitemapCollector collector = new SitemapCollector(pathFinder);
    //collector.collectSitemaps();
    assertThat(collector.getCountCapabilityLists(), is(1));
  }
}
