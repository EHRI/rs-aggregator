package nl.knaw.dans.rs.aggregator.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Date;

/**
 * Created on 2017-04-28 13:49.
 */
public class ZonedDateTimeUtil {

  private static ZoneId ZONE_ID;

  public static ZoneId getZoneId() {
    if (ZONE_ID == null) {
      ZONE_ID = ZoneId.systemDefault();
    }
    return ZONE_ID;
  }

  public static ZoneId setZoneId(ZoneId zoneId) {
    ZoneId oldZoneId = ZONE_ID;
    ZONE_ID = zoneId;
    return oldZoneId;
  }

  private static DateTimeFormatter localFormat = new DateTimeFormatterBuilder()
    .appendPattern("yyyy[-MM[-dd['T'HH[:mm[:ss]]]]]")
    .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
    .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
    .optionalStart()
    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
    .toFormatter();

  public static ZonedDateTime fromXmlString(String value) throws Exception {
    if (value == null) {
      return null;
    }
    if (value.matches(".*([Z]|[+-][0-9]{1,2}:[0-9]{1,2})$")) {
      return ZonedDateTime.parse(value).withZoneSameInstant(ZoneOffset.UTC);
    } else {
      LocalDateTime local = LocalDateTime.parse(value, localFormat);
      ZonedDateTime localZ = ZonedDateTime.of(local, getZoneId());
      return localZ.withZoneSameInstant(ZoneOffset.UTC);
    }
  }

  public static String toXmlString(ZonedDateTime value) throws Exception {
    if (value == null) {
      return null;
    }
    return value.toString();
  }

  public static ZonedDateTime fromLong(long value) {
    Date date = new Date(value);
    return ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
  }

  public static long toLong(ZonedDateTime value) {
    return Date.from(value.toInstant()).getTime();
  }
}
