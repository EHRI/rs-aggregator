package nl.knaw.dans.rs.aggregator.xml;

import nl.knaw.dans.rs.aggregator.util.ZonedDateTimeUtil;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Adapter for conversion between a {@link ZonedDateTime} and an ISO 8601 profile know as W3C Datetime format.
 * <p>
 * The unmarshal proces will convert given datetime strings to UTC (Coordinated Universal Time).
 * If a datetime string has no timezone info, we assume that the datetime is in the timezone
 * that is returned by the static {@link ZonedDateTimeUtil#getZoneId()}. This timezone will default to
 * {@link ZoneId#systemDefault()}, and can be set with {@link ZonedDateTimeUtil#setZoneId(ZoneId)}.
 *  </p>
 * The following examples illustrate the conversion of several valid W3C Datetime strings (unmarshal -&gt; marshal).
 * Datetimes without timezone info were calculated with an offset of UTC+10:00.
 * <pre>
 *    2016 -&gt; 2015-12-31T14:00Z
 *    2015-08 -&gt; 2015-07-31T14:00Z
 *    2014-08-09 -&gt; 2014-08-08T14:00Z
 *    2013-03-09T14 -&gt; 2013-03-09T04:00Z
 *    2012-03-09T14:30 -&gt; 2012-03-09T04:30Z
 *    2011-03-09T14:30:29 -&gt; 2011-03-09T04:30:29Z
 *    2010-01-09T14:30:29.1 -&gt; 2010-01-09T04:30:29.100Z
 *    2010-03-09T14:30:29.123 -&gt; 2010-03-09T04:30:29.123Z
 *    2010-04-09T14:30:29.1234 -&gt; 2010-04-09T04:30:29.123400Z
 *    2010-06-09T14:30:29.123456 -&gt; 2010-06-09T04:30:29.123456Z
 *
 *    2009-03-09T14:30:29.123+01:00 -&gt; 2009-03-09T13:30:29.123Z
 *    2008-03-09T14:30:29.123-01:00 -&gt; 2008-03-09T15:30:29.123Z
 *    2007-03-09T14:30:29.123Z -&gt; 2007-03-09T14:30:29.123Z
 *    2006-03-09T14:30:29.123456789Z -&gt; 2006-03-09T14:30:29.123456789Z
 *    2005-03-09T14:30Z -&gt; 2005-03-09T14:30Z
 * </pre>
 *
 * @see
 * <a href="https://www.w3.org/TR/1998/NOTE-datetime-19980827">https://www.w3.org/TR/1998/NOTE-datetime-19980827</a>
 * <a href="https://tools.ietf.org/html/rfc3339">https://tools.ietf.org/html/rfc3339</a>
 *
*/
public class ZonedDateTimeAdapter extends XmlAdapter<String, ZonedDateTime> {


  @Override
  public ZonedDateTime unmarshal(String value) throws Exception {
    return ZonedDateTimeUtil.fromXmlString(value);
  }

  @Override
  public String marshal(ZonedDateTime value) throws Exception {
    return  ZonedDateTimeUtil.toXmlString(value);
  }


}
