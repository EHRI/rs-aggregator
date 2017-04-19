package nl.knaw.dans.rs.aggregator.discover;

import nl.knaw.dans.rs.aggregator.http.Result;
import nl.knaw.dans.rs.aggregator.xml.Capability;
import nl.knaw.dans.rs.aggregator.xml.RsMd;
import nl.knaw.dans.rs.aggregator.xml.UrlItem;
import nl.knaw.dans.rs.aggregator.xml.Urlset;
import org.junit.Test;

import java.net.URI;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created on 2017-04-18 15:06.
 */
public class UrlsetPivotTest {

  @Test
  public void testMapUrlItemsByUri() throws Exception {
    final ResultIndex index = new ResultIndex();

    Result<Urlset> result1 = new Result<>(new URI("doc1"));
    Urlset urlset1 = new Urlset(new RsMd((Capability.RESOURCELIST.xmlValue)));
    urlset1.addItem(new UrlItem("http://example.com/rs1.txt"));
    urlset1.addItem(new UrlItem("http://example.com/rs2"));
    urlset1.addItem(new UrlItem("http://example.com/rs3"));
    urlset1.addItem(new UrlItem("http://www.EXAMPLE.com/abx.doc"));
    result1.accept(urlset1);
    index.add(result1);

    Result<Urlset> result2 = new Result<>(new URI("doc2"));
    Urlset urlset2 = new Urlset(new RsMd((Capability.RESOURCELIST.xmlValue)));
    urlset2.addItem(new UrlItem("http://example.com/ignore/../rs1.txt"));
    urlset2.addItem(new UrlItem("http://example.com/abx.doc"));
    urlset2.addItem(new UrlItem("http://i n v a l i d .com/abc/def.doc"));
    result2.accept(urlset2);
    index.add(result2);

    Result<Urlset> result3 = new Result<>(new URI("doc3"));
    Urlset urlset3 = new Urlset(new RsMd((Capability.RESOURCELIST.xmlValue)));
    urlset3.addItem(new UrlItem("http://i n v a l i d .com/abc/2nd.doc"));
    urlset3.addItem(new UrlItem(""));
    result3.accept(urlset3);
    index.add(result3);

    UrlSetPivot pivot = new UrlSetPivot(index);

    pivot.listSortedUrlsetResults(Capability.RESOURCELIST).forEach(urlsetResult -> System.out.println(urlsetResult.getUri()));

    System.out.println();
    Map<String, UrlItem> locs = pivot.mapUrlItemsByLoc(Capability.RESOURCELIST);
    locs.entrySet().forEach(System.out::println);
    assertThat(locs.size(), is(9));

    System.out.println();
    Map<URI, UrlItem> uris = pivot.mapUrlItemsByUri(Capability.RESOURCELIST);
    uris.entrySet().forEach(System.out::println);
    assertThat(uris.size(), is(7));

    long invalids = uris.keySet().stream().filter(uri -> uri.getPath().startsWith("invalid")).count();
    assertThat(invalids, is(2L));
  }

}
