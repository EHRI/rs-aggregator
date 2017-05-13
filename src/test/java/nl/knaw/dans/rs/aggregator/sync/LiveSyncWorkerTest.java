package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.http.Testing;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assume.assumeTrue;

/**
 * Created on 2017-04-19 17:25.
 */
public class LiveSyncWorkerTest {

  private static String baseDirectory = "target/test-output/synchronizer";
  private static String capabilityListUrl = "http://zandbak11.dans.knaw.nl/ehri2/mdx/capabilitylist.xml";
  //private static  String capabilityListUrl = "http://publisher-connector.core.ac.uk/resourcesync/sitemaps/elsevier/pdf/capabilitylist.xml";
  //private static String capabilityListUrl = "http://publisher-connector.core.ac.uk/resourcesync/sitemaps/elsevier/metadata/capabilitylist.xml";

  @BeforeClass
  public static void initialize() throws Exception {
    assumeTrue(Testing.LIVE_TESTS);
  }

  @Test
  public void testSynchronize() throws Exception {
    PathFinder pathFinder = new PathFinder(baseDirectory, URI.create(capabilityListUrl));
    SyncProperties syncProps = new SyncProperties();
    SyncWorker syncWorker = new SyncWorker()
      .withMaxDownloadRetry(3)
      .withTrialRun(false)
      //.withMaxDownloads(1000)
      .withSitemapCollector(new SitemapCollector())
      .withResourceManager(new FsResourceManager());

      syncWorker.synchronize(pathFinder, syncProps);

  }
}
