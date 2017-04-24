package nl.knaw.dans.rs.aggregator.xml;

import org.junit.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Created on 2017-04-20 16:18.
 */
public class RsItemTest {

  @Test
  public void testIsAfterDatTime() {
    UrlItem item1 = new UrlItem("location1");
    UrlItem item2 = new UrlItem("location2");
    assertThat(item1.getRsMdDateTime(), is(nullValue()));
    assertThat(item1.isZDTAfter(item2), is(0));

    RsMd rsmd1 = new RsMd()
      .withDateTime(ZonedDateTime.parse("2000-01-01T00:00:00.000Z").withZoneSameInstant(ZoneOffset.UTC));
    item1.withMetadata(rsmd1);
    assertThat(item1.isZDTAfter(item2), is(0));

    RsMd rsmd2 = new RsMd()
      .withDateTime(ZonedDateTime.parse("2000-01-01T00:00:00.000Z").withZoneSameInstant(ZoneOffset.UTC));
    item2.withMetadata(rsmd2);
    assertThat(item1.isZDTAfter(item2), is(0));

    rsmd2.withDateTime(ZonedDateTime.parse("2000-01-01T00:00:00.001Z").withZoneSameInstant(ZoneOffset.UTC));
    assertThat(item1.isZDTAfter(item2), is(-1));
    assertThat(item2.isZDTAfter(item1), is(1));

    assertThat(item1.isZDTAfter(item1), is(0));

    UrlItem latest = item1.latest(item2);
    assertThat(latest, equalTo(item2));

    latest = item2.latest(item1);
    assertThat(latest, equalTo(item2));

    rsmd2.withDateTime(ZonedDateTime.parse("2000-01-01T00:00:00.000Z").withZoneSameInstant(ZoneOffset.UTC));
    latest = item1.latest(item2);
    assertThat(latest, equalTo(item2));
    latest = item2.latest(item1);
    assertThat(latest, equalTo(item1));

  }

  @Test
  public void maybeHash() {
    UrlItem item = new UrlItem("location1");
    Optional<String> maybeHash = item.getMetadata().flatMap(RsMd::getHash);
    assertThat(maybeHash.isPresent(), is(false));
    item.withMetadata(new RsMd().withHash("HasHhasH"));
    maybeHash = item.getMetadata().flatMap(RsMd::getHash);
    assertThat(maybeHash.isPresent(), is(true));
    assertThat(maybeHash.get(), equalTo("HasHhasH"));
  }
}
