package nl.knaw.dans.rs.aggregator.discover;

import nl.knaw.dans.rs.aggregator.http.Result;
import nl.knaw.dans.rs.aggregator.http.Testing;
import nl.knaw.dans.rs.aggregator.xml.ResourceSyncContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.Assume.assumeTrue;

/**
 * Created on 2017-04-06 16:38.
 */
public class LiveExpeditionTest {

  private static ResourceSyncContext rsContext;
  private static CloseableHttpClient httpclient;
  private static String zandbak_url = "http://zandbak11.dans.knaw.nl/ehri2/";

  @BeforeClass
  public static void initialize() throws Exception {
    assumeTrue(Testing.LIVE_TESTS);
    httpclient = HttpClients.createDefault();
    rsContext = new ResourceSyncContext();
  }

  @Test
  public void testZandbakExplore() throws Exception {
    Expedition expedition = new Expedition(httpclient, rsContext);
    List<ResultIndex> indexes = expedition.explore(zandbak_url);
    //indexes.forEach(System.out::println);
    for (ResultIndex ri : indexes) {
      System.out.println(ri);
      for (Result r : ri.getResultMap().values()) {
        System.out.println("\t" + r.getUri() + " " + r.getStatusCode());
        System.out.println("\t" + r.getContent().orElse("No Content"));
        Map<URI, Result<?>> rChildren = r.getChildren();
        System.out.println(rChildren.size());
        for (URI key : rChildren.keySet()) {
          System.out.println("\t\t"  + key);
        }
      }
    }
  }

  @Test
  public void testZandbakExploreAndMerge() throws Exception {
    Expedition expedition = new Expedition(httpclient, rsContext);
    ResultIndex index = expedition.exploreAndMerge(zandbak_url);
    System.out.println(index.getResultMap().size());
    for (Result result : index.getResultMap().values()) {
      System.out.println(result.getUri() + " " + result.getStatusCode());
      System.out.println("children: " + result.getChildren().size() + " parents: " + result.getParents().size());

    }
    ResultIndexPivot pivot = new ResultIndexPivot((index));
    pivot.listUrlsetResults()
      .forEach(urlsetResult -> System.out.println(urlsetResult.getUri()));
  }

}
