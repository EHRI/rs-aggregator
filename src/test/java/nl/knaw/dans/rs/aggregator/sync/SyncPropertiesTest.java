package nl.knaw.dans.rs.aggregator.sync;

import org.junit.Test;

import java.io.File;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Created on 2017-05-01 09:51.
 */
public class SyncPropertiesTest {

  @Test
  public void testPersist() throws Exception {
    SyncProperties sp1 = new SyncProperties();
    File file1 = new File("target/test-output/syncproperties/syncprops-empty.xml");
    sp1.storeToXML(file1, "testPersist empty");

    ZonedDateTime start = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC);
    sp1.setDateTime(SyncProperties.PROP_SW_SYNC_START, start);

    ZonedDateTime end = ZonedDateTime.now();
    sp1.setDateTime(SyncProperties.PROP_SW_SYNC_END, end);

    ZonedDateTime asOfDateTime = null;
    sp1.setDateTime(SyncProperties.PROP_CL_AS_OF_DATE_TIME, asOfDateTime);

    File file2 = new File("target/test-output/syncproperties/syncprops-full.xml");
    sp1.storeToXML(file2, "testPersist full");

    //
    SyncProperties sp2 = new SyncProperties();
    sp2.loadFromXML(file2);

    assertThat(sp2.getDateTime(SyncProperties.PROP_SW_SYNC_START), equalTo(start));
    assertThat(sp2.getDateTime(SyncProperties.PROP_SW_SYNC_END), equalTo(end.withZoneSameInstant(ZoneOffset.UTC)));
    assertThat(sp2.getDateTime(SyncProperties.PROP_CL_AS_OF_DATE_TIME), is(asOfDateTime));
  }
}
