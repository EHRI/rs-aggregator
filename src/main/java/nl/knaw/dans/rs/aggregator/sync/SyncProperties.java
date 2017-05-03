package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.util.ZonedDateTimeUtil;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Created on 2017-05-01 09:27.
 */
public class SyncProperties extends Properties {


  public Object setDateTime(String key, ZonedDateTime zdt) {
    if (zdt == null) {
      return remove(key);
    } else {
      return setProperty(key, ZonedDateTimeUtil.toXmlString(zdt));
    }
  }

  public ZonedDateTime getDateTime(String key) {
    return ZonedDateTimeUtil.fromXmlString(getProperty(key));
  }

  public Object setInt(String key, int value) {
    return setProperty(key, Integer.toString(value));
  }

  public int getInt(String key) {
    String value = getProperty(key);
    if (value == null) {
      value = "0";
    }
    return Integer.parseInt(value);
  }

  public Object setBool(String key, boolean value) {
    return setProperty(key, Boolean.toString(value));
  }

  public boolean getBool(String key) {
    String value = getProperty(key);
    return "true".equals(value);
  }

  public void storeToXML(File file, String comment) throws IOException {
    if (!file.getParentFile().exists()) {
      if (!file.getParentFile().mkdirs()) {
        throw new IOException("Cannot create directories for " + file);
      }
    }
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(file);
      keys();
      storeToXML(fos, comment);
    } finally {
      IOUtils.closeQuietly(fos);
    }
  }

  public synchronized void loadFromXML(File file) throws IOException {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(file);
      loadFromXML(fis);
    } finally {
      IOUtils.closeQuietly(fis);
    }
  }

  // xml output sorted on entry key.
  @Override
  public Set<Map.Entry<Object, Object>> entrySet() {
    TreeMap<Object, Object> tm = new TreeMap<>();
    for (Map.Entry<Object, Object> entry : super.entrySet()) {
      tm.put(entry.getKey(), entry.getValue());
    }
    return tm.entrySet();
  }
}
