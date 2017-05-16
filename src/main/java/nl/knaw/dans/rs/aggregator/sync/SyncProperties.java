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


  public static final String CL = "cl.";
  // properties set by SitemapCollector
  public static final String PROP_CL_ITEMS_DELETED = CL + "items.deleted";
  public static final String PROP_CL_ITEMS_UPDATED = CL + "items.updated";
  public static final String PROP_CL_ITEMS_CREATED = CL + "items.created";
  public static final String PROP_CL_ITEMS_REMAINING = CL + "items.remaining";
  public static final String PROP_CL_ITEMS_RECENT = CL + "items.recent";
  public static final String PROP_CL_FOUND_NEW_RESOURCELIST = CL + "found.new.resourcelist";
  public static final String PROP_CL_DATE_LATEST_CHANGELIST = CL + "date.latest.change.list";
  public static final String PROP_CL_DATE_LATEST_RESOURCELIST = CL + "date.latest.resource.list";
  public static final String PROP_CL_COUNT_CHANGELISTS = CL + "cl.change.lists";
  public static final String PROP_CL_COUNT_RESOURCELISTS = CL + "cl.resource.lists";
  public static final String PROP_CL_COUNT_CHANGELIST_INDEXES = CL + "cl.change.list.indexes";
  public static final String PROP_CL_COUNT_RESOURCELIST_INDEXES = CL + "cl.resource.list.indexes";
  public static final String PROP_CL_COUNT_CAPABILITY_LISTS = CL + "cl.capability.lists";
  public static final String PROP_CL_COUNT_UNHNDLED_RESULTS = CL + "cr.unhandled.results";
  public static final String PROP_CL_COUNT_ERROR_RESULTS = CL + "cr.error.results";
  public static final String PROP_CL_COUNT_INVALID_URIS = CL + "cr.invalid.uris";
  public static final String PROP_CL_CONVERTER = CL + "a2.converter";
  public static final String PROP_CL_AS_OF_DATE_TIME = CL + "a1.as.of.date.time";

  public static final String SW = "sw.";
  // properties set by SyncWorker
  public static final String PROP_SW_TOTAL_DOWNLOAD_COUNT = SW + "total.download.count";
  public static final String PROP_SW_FAILED_REMAINS = SW + "failed.remains";
  public static final String PROP_SW_FAILED_UPDATES = SW + "failed.updates";
  public static final String PROP_SW_FAILED_CREATIONS = SW + "failed.creations";
  public static final String PROP_SW_FAILED_DELETIONS = SW + "failed.deletions";
  public static final String PROP_SW_TOTAL_FAILED_ITEMS = SW + "total.failed.items";
  public static final String PROP_SW_ITEMS_NO_ACTION = SW + "items.no.action";
  public static final String PROP_SW_ITEMS_REMAIN = SW + "items.remain";
  public static final String PROP_SW_ITEMS_UPDATED = SW + "items.updated";
  public static final String PROP_SW_ITEMS_CREATED = SW + "items.created";
  public static final String PROP_SW_ITEMS_DELETED = SW + "items.deleted";
  public static final String PROP_SW_ITEMS_VERIFIED = SW + "items.verified";
  public static final String PROP_SW_TOTAL_ITEMS = SW + "total.items";
  public static final String PROP_SW_VERIFICATION_POLICY = SW + "class.verification.policy";
  public static final String PROP_SW_RESOURCE_MANAGER = SW + "class.resource.manager";
  public static final String PROP_SW_SITEMAP_COLLECTOR = SW + "class.sitemap.collector";
  public static final String PROP_SW_TRIAL_RUN = SW + "a3.trial.run";
  public static final String PROP_SW_MAX_DOWNLOAD_RETRY = SW + "a2.max.download.retry";
  public static final String PROP_SW_MAX_DOWNLOADS = SW + "a1.max.downloads";
  public static final String PROP_SW_FULLY_SYNCHRONIZED = SW + "z3.fully.synchronized";
  public static final String PROP_SW_SYNC_END = SW + "z2.sync.end";
  public static final String PROP_SW_SYNC_START = SW + "z1.sync.start";


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
