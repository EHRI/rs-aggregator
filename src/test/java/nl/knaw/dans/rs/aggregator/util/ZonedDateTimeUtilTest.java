package nl.knaw.dans.rs.aggregator.util;


import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.is;

public class ZonedDateTimeUtilTest {

  @Test
  public void testMarshalUnmarshal() throws Exception {

    // assumed timezone of strings with no timezone info
    ZoneId newZoneId = ZoneId.of("UTC+10:00");
    // keep old zoneId for reset
    ZoneId oldZoneId = ZonedDateTimeUtil.setZoneId(newZoneId);

    String[] inpexp = {
      // local dates
      "2016", "2015-12-31T14:00Z",
      "2015-08", "2015-07-31T14:00Z",
      "2014-08-09", "2014-08-08T14:00Z",
      // local datetimes
      "2013-03-09T14", "2013-03-09T04:00Z",
      "2012-03-09T14:30", "2012-03-09T04:30Z",
      "2011-03-09T14:30:29", "2011-03-09T04:30:29Z",
      "2010-01-09T14:30:29.1", "2010-01-09T04:30:29.100Z",
      "2010-03-09T14:30:29.123", "2010-03-09T04:30:29.123Z",
      "2010-04-09T14:30:29.1234", "2010-04-09T04:30:29.123400Z",
      "2010-06-09T14:30:29.123456", "2010-06-09T04:30:29.123456Z",
      // zoned datetimes
      "2009-03-09T14:30:29.123+01:00", "2009-03-09T13:30:29.123Z",
      "2008-03-09T14:30:29.123-01:00", "2008-03-09T15:30:29.123Z",
      "2007-03-09T14:30:29.123Z", "2007-03-09T14:30:29.123Z",
      "2006-03-09T14:30:29.123456789Z", "2006-03-09T14:30:29.123456789Z",
      "2005-03-09T14:30Z", "2005-03-09T14:30Z"
    };

    for (int i = 0; i < inpexp.length; i += 2) {
      String input = inpexp[i];
      String expected = inpexp[i + 1];
      ZonedDateTime zdt1 = ZonedDateTimeUtil.fromXmlString(input);
      String str1 = ZonedDateTimeUtil.toXmlString(zdt1);
      //System.out.println(input + " -> " + str1);
      assertThat(str1, equalTo(expected));

      ZonedDateTime zdt2 = ZonedDateTimeUtil.fromXmlString(str1);
      String str2 = ZonedDateTimeUtil.toXmlString(zdt2);
      assertThat(str2, equalTo(expected));

      long lm1 = ZonedDateTimeUtil.toLong(zdt1);
      ZonedDateTime zdt3 = ZonedDateTimeUtil.fromLong(lm1);
      long lm2 = ZonedDateTimeUtil.toLong(zdt3);
      ZonedDateTime zdt4 = ZonedDateTimeUtil.fromLong(lm1);
      String str4 = ZonedDateTimeUtil.toXmlString(zdt4);
      String str5 = ZonedDateTimeUtil.toFileSaveFormat(zdt4);
      ZonedDateTime zdt5 = ZonedDateTimeUtil.fromFileSaveFormat(str5);
      assertThat(str5, equalTo(ZonedDateTimeUtil.toFileSaveFormat(zdt1)));
      assertThat(str5, equalTo(ZonedDateTimeUtil.toFileSaveFormat(zdt2)));
      assertThat(str5, equalTo(ZonedDateTimeUtil.toFileSaveFormat(zdt3)));
      assertThat(str5, equalTo(ZonedDateTimeUtil.toFileSaveFormat(zdt4)));
      assertThat(str5, equalTo(ZonedDateTimeUtil.toFileSaveFormat(zdt5)));

      System.out.println(input + " -> " + str1 + " -> " + new Date(lm1) + " = " + lm1 + " -> "
        + str4 + " -> " + str5);
      assertThat(lm1, is(lm2));
      //assertThat(str1, equalTo(str4));
    }

    // reset zoneId
    ZoneId replacedZoneId = ZonedDateTimeUtil.setZoneId(oldZoneId);
    assertThat(replacedZoneId, equalTo(newZoneId));
  }

  @Test(expected = DateTimeParseException.class)
  public void testUnmarshalWithInvalidString() throws Exception {
    ZonedDateTimeUtil.fromXmlString("ivalid string");
  }

  @Test
  public void testWithNullInput() throws Exception {

    String str = null;
    assertThat(ZonedDateTimeUtil.fromXmlString(str), nullValue(ZonedDateTime.class));

    ZonedDateTime zdt = null;
    assertThat(ZonedDateTimeUtil.toXmlString(zdt), nullValue(String.class));
  }

  @Test
  public void testLong() {
    long lm1 = 0L;
    ZonedDateTime zdt = ZonedDateTimeUtil.fromLong(lm1);
    System.out.println(zdt);
    long lm2 = ZonedDateTimeUtil.toLong(zdt);
    //System.out.println(lm1 + " -> " + lm2);
    assertThat(lm2, is(0L));
  }

}
