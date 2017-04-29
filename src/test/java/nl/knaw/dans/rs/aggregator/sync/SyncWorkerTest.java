package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.xml.RsMd;
import nl.knaw.dans.rs.aggregator.xml.UrlItem;
import org.junit.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Created on 2017-04-23 14:53.
 */
public class SyncWorkerTest {

  @Test
  public void testMerge() {
    Map<String, UrlItem> map = new HashMap<>();

    UrlItem item1 = new UrlItem("abc");
    UrlItem item2 = new UrlItem("abc");

    RsMd rsmd1 = new RsMd()
      .withDateTime(ZonedDateTime.parse("2000-01-01T00:00:00.000Z").withZoneSameInstant(ZoneOffset.UTC));
    item1.withMetadata(rsmd1);

    RsMd rsmd2 = new RsMd()
      .withDateTime(ZonedDateTime.parse("2000-01-01T00:00:01.000Z").withZoneSameInstant(ZoneOffset.UTC));
    item2.withMetadata(rsmd2);


    map.merge("abc", item1, UrlItem::latest);
    assertThat(map.get("abc"), equalTo(item1));

    map.merge("abc", item2, UrlItem::latest);
    assertThat(map.get("abc"), equalTo(item2));

    map.merge("abc", item1, UrlItem::latest);
    assertThat(map.get("abc"), equalTo(item2));

  }
}
