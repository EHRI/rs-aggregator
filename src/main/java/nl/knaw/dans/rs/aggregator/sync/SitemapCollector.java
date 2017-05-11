package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.discover.RemoteResourceSyncFrameworkException;
import nl.knaw.dans.rs.aggregator.discover.ResultIndex;
import nl.knaw.dans.rs.aggregator.discover.RsExplorer;
import nl.knaw.dans.rs.aggregator.http.Result;
import nl.knaw.dans.rs.aggregator.util.NormURI;
import nl.knaw.dans.rs.aggregator.util.LambdaUtil;
import nl.knaw.dans.rs.aggregator.xml.Capability;
import nl.knaw.dans.rs.aggregator.xml.ResourceSyncContext;
import nl.knaw.dans.rs.aggregator.xml.RsBuilder;
import nl.knaw.dans.rs.aggregator.xml.RsConstants;
import nl.knaw.dans.rs.aggregator.xml.RsMd;
import nl.knaw.dans.rs.aggregator.xml.RsRoot;
import nl.knaw.dans.rs.aggregator.xml.Sitemapindex;
import nl.knaw.dans.rs.aggregator.xml.UrlItem;
import nl.knaw.dans.rs.aggregator.xml.Urlset;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Created on 2017-04-26 09:13.
 */
public class SitemapCollector implements RsConstants {

  private static final String CN = SitemapCollector.class.getSimpleName() + ".";
  public static final String PROP_AS_OF_DATE_TIME = CN + "a1.as.of.date.time";
  public static final String PROP_CONVERTER = CN + "a2.converter";

  public static final String PROP_COUNT_INVALID_URIS = CN + "cr.invalid.uris";
  public static final String PROP_COUNT_ERROR_RESULTS = CN + "cr.error.results";
  public static final String PROP_COUNT_UNHNDLED_RESULTS = CN + "cr.unhandled.results";

  public static final String PROP_COUNT_CAPABILITY_LISTS = CN + "cl.capability.lists";
  public static final String PROP_COUNT_RESOURCELIST_INDEXES = CN + "cl.resource.list.indexes";
  public static final String PROP_COUNT_CHANGELIST_INDEXES = CN + "cl.change.list.indexes";
  public static final String PROP_COUNT_RESOURCELISTS = CN + "cl.resource.lists";
  public static final String PROP_COUNT_CHANGELISTS = CN + "cl.change.lists";

  public static final String PROP_DATE_LATEST_RESOURCELIST = CN + "date.latest.resource.list";
  public static final String PROP_DATE_LATEST_CHANGELIST = CN + "date.latest.change.list";

  public static final String PROP_ITEMS_RECENT = CN + "items.recent";
  public static final String PROP_ITEMS_REMAINING = CN + "items.remaining";
  public static final String PROP_ITEMS_CREATED = CN + "items.created";
  public static final String PROP_ITEMS_UPDATED = CN + "items.updated";
  public static final String PROP_ITEMS_DELETED = CN + "items.deleted";

  private static Logger logger = LoggerFactory.getLogger(SitemapCollector.class);

  private CloseableHttpClient httpClient;
  private ResourceSyncContext rsContext;

  private ZonedDateTime asOfDateTime;
  private LambdaUtil.BiFunction_WithExceptions<URI, HttpResponse, RsRoot, Exception> converter;

  private PathFinder currentPathFinder;

  private Set<String> invalidUris;
  private List<Result<?>> errorResults;
  private List<Result<?>> unhandledResults;
  private Map<URI, UrlItem> recentItems;
  private ZonedDateTime ultimateResourceListAt;
  private ZonedDateTime ultimateChangeListFrom;

  private int countCapabilityLists;
  private int countResourceListIndexes;
  private int countChangelistIndexes;
  private int countResourceLists;
  private int countChangeLists;

  private int countRemain;
  private int countCreated;
  private int countUpdated;
  private int countDeleted;

  private boolean collected;

  public SitemapCollector() {
  }

  public CloseableHttpClient getHttpClient() {
    if (httpClient == null) {
      httpClient = HttpClients.createDefault();
    }
    return httpClient;
  }

  public SitemapCollector withHttpClient(CloseableHttpClient httpClient) {
    this.httpClient = httpClient;
    return this;
  }

  public ResourceSyncContext getRsContext() {
    if (rsContext == null) {
      try {
        rsContext = new ResourceSyncContext();
      } catch (JAXBException e) {
        throw new RuntimeException(e);
      }
    }
    return rsContext;
  }

  public SitemapCollector withRsContext(ResourceSyncContext rsContext) {
    this.rsContext = rsContext;
    return this;
  }

  public ZonedDateTime getAsOfDateTime() {
    if (asOfDateTime == null) {
      asOfDateTime = ZonedDateTime.parse(NULL_DATE).withZoneSameInstant(ZoneOffset.UTC);
    }
    return asOfDateTime;
  }

  public SitemapCollector withAsOfDateTime(ZonedDateTime asOfDateTime) {
    this.asOfDateTime = asOfDateTime;
    return this;
  }

  public LambdaUtil.BiFunction_WithExceptions<URI, HttpResponse, RsRoot, Exception> getConverter() {
    if (converter == null) {
      throw new IllegalStateException("No converter set");
    }
    return converter;
  }

  public SitemapCollector withConverter(LambdaUtil.BiFunction_WithExceptions<URI, HttpResponse, RsRoot, Exception> converter) {
    this.converter = converter;
    return this;
  }

  public boolean isCollected() {
    return collected;
  }

  public Set<String> getInvalidUris() {
    if (!collected) throw new IllegalStateException("SitemapCollector has not collected sitemaps.");
    return invalidUris;
  }

  public List<Result<?>> getErrorResults() {
    if (!collected) throw new IllegalStateException("SitemapCollector has not collected sitemaps.");
    return errorResults;
  }

  public List<Result<?>> getUnhandledResults() {
    if (!collected) throw new IllegalStateException("SitemapCollector has not collected sitemaps.");
    return unhandledResults;
  }

  public boolean hasErrors() {
    if (!collected) throw new IllegalStateException("SitemapCollector has not collected sitemaps.");
    return !(invalidUris.isEmpty() && errorResults.isEmpty() && unhandledResults.isEmpty());
  }

  public int countErrors() {
    if (!collected) throw new IllegalStateException("SitemapCollector has not collected sitemaps.");
    return invalidUris.size() + errorResults.size() + unhandledResults.size();
  }

  public int getCountCapabilityLists() {
    if (!collected) throw new IllegalStateException("SitemapCollector has not collected sitemaps.");
    return countCapabilityLists;
  }

  public int getCountResourceListIndexes() {
    if (!collected) throw new IllegalStateException("SitemapCollector has not collected sitemaps.");
    return countResourceListIndexes;
  }

  public int getCountChangelistIndexes() {
    if (!collected) throw new IllegalStateException("SitemapCollector has not collected sitemaps.");
    return countChangelistIndexes;
  }

  public int getCountResourceLists() {
    if (!collected) throw new IllegalStateException("SitemapCollector has not collected sitemaps.");
    return countResourceLists;
  }

  public int getCountChangeLists() {
    if (!collected) throw new IllegalStateException("SitemapCollector has not collected sitemaps.");
    return countChangeLists;
  }

  public int getCountRemain() {
    if (!collected) throw new IllegalStateException("SitemapCollector has not collected sitemaps.");
    return countRemain;
  }

  public int getCountCreated() {
    if (!collected) throw new IllegalStateException("SitemapCollector has not collected sitemaps.");
    return countCreated;
  }

  public int getCountUpdated() {
    if (!collected) throw new IllegalStateException("SitemapCollector has not collected sitemaps.");
    return countUpdated;
  }

  public int getCountDeleted() {
    if (!collected) throw new IllegalStateException("SitemapCollector has not collected sitemaps.");
    return countDeleted;
  }

  public ZonedDateTime getUltimateResourceListAt() {
    if (!collected) throw new IllegalStateException("SitemapCollector has not collected sitemaps.");
    return ultimateResourceListAt;
  }

  public ZonedDateTime getUltimateChangeListFrom() {
    if (!collected) throw new IllegalStateException("SitemapCollector has not collected sitemaps.");
    return ultimateChangeListFrom;
  }

  public ZonedDateTime getUltmateListDate() {
    if (!collected) throw new IllegalStateException("SitemapCollector has not collected sitemaps.");
    return ultimateChangeListFrom.isAfter(ultimateResourceListAt) ? ultimateChangeListFrom : ultimateResourceListAt;
  }

  public Map<URI, UrlItem> getMostRecentItems() {
    if (!collected) throw new IllegalStateException("SitemapCollector has not collected sitemaps.");
    return recentItems;
  }

  public void collectSitemaps(PathFinder pathFinder, SyncProperties syncProps) {
    reset();
    currentPathFinder = pathFinder;
    RsExplorer explorer = new RsExplorer(getHttpClient(), getRsContext())
      .withConverter(getConverter())
      .withFollowChildLinks(true)
      .withFollowIndexLinks(false)
      .withFollowParentLinks(false);
    ResultIndex index = explorer.explore(pathFinder.getCapabilityListUri());

    invalidUris = index.getInvalidUris();
    for (String invalidUri : invalidUris) {
      logger.warn("Found invalid URI: {}", invalidUri);
    }

    for (Result<?> result : index.getResultMap().values()) {
      if (result.hasErrors()) {
        errorResults.add(result);
      } else {
        analyze(result);
        collected = true;
      }
    }
    reportResults(pathFinder, syncProps);
    currentPathFinder = null;
  }

  private void reportResults(PathFinder pathFinder, SyncProperties syncProps) {
    syncProps.setDateTime(PROP_AS_OF_DATE_TIME, asOfDateTime);
    syncProps.setProperty(PROP_CONVERTER, getConverter().toString());
    syncProps.setInt(PROP_COUNT_INVALID_URIS, invalidUris.size());
    syncProps.setInt(PROP_COUNT_ERROR_RESULTS, errorResults.size());
    syncProps.setInt(PROP_COUNT_UNHNDLED_RESULTS, unhandledResults.size());

    syncProps.setInt(PROP_COUNT_CAPABILITY_LISTS, countCapabilityLists);
    syncProps.setInt(PROP_COUNT_RESOURCELIST_INDEXES, countResourceListIndexes);
    syncProps.setInt(PROP_COUNT_CHANGELIST_INDEXES, countChangelistIndexes);
    syncProps.setInt(PROP_COUNT_RESOURCELISTS, countResourceLists);
    syncProps.setInt(PROP_COUNT_CHANGELISTS, countChangeLists);

    syncProps.setDateTime(PROP_DATE_LATEST_RESOURCELIST, ultimateResourceListAt);
    syncProps.setDateTime(PROP_DATE_LATEST_CHANGELIST, ultimateChangeListFrom);

    syncProps.setInt(PROP_ITEMS_RECENT, recentItems.size());
    syncProps.setInt(PROP_ITEMS_REMAINING, countRemain);
    syncProps.setInt(PROP_ITEMS_CREATED, countCreated);
    syncProps.setInt(PROP_ITEMS_UPDATED, countUpdated);
    syncProps.setInt(PROP_ITEMS_DELETED, countDeleted);

    try {
      File file = pathFinder.getSyncPropXmlFile();
      String lsb = "Last saved by " + this.getClass().getName();
      syncProps.storeToXML(file, lsb);
      logger.debug("Saved SitemapCollector properties to {}", file);
    } catch (IOException e) {
      logger.error("Could not save syncProps", e);
      throw new RuntimeException(e);
    }
  }

  private void reset() {
    errorResults = new ArrayList<>();
    unhandledResults = new ArrayList<>();
    recentItems = new HashMap<>();

    countCapabilityLists = 0;
    countResourceListIndexes = 0;
    countChangelistIndexes = 0;
    countResourceLists = 0;
    countChangeLists = 0;

    countRemain = 0;
    countCreated = 0;
    countUpdated = 0;
    countDeleted = 0;

    ultimateResourceListAt = ZonedDateTime.parse(NULL_DATE).withZoneSameInstant(ZoneOffset.UTC);
    ultimateChangeListFrom = ZonedDateTime.parse(NULL_DATE).withZoneSameInstant(ZoneOffset.UTC);
  }

  @SuppressWarnings("unchecked")
  private void analyze(Result<?> result) {
    if (result.getContent().isPresent()) {
      Object content = result.getContent().get();
      if (content instanceof Sitemapindex) {
        Result<Sitemapindex> siResult = (Result<Sitemapindex>) result;
        analyzeSitemapIndex(siResult);
      } else if (content instanceof Urlset) {
        Result<Urlset> usResult = (Result<Urlset>) result;
        analyzeUrlset(usResult);
      } else {
        throw new RuntimeException("Unexpected result content: " + result);
      }
    } else {
      throw new RuntimeException("Unexpected result with no error and no content: " + result);
    }
  }

  private void analyzeSitemapIndex(Result<Sitemapindex> siResult) {
    Sitemapindex sitemapindex = siResult.getContent().orElse(null);
    String xmlValue = sitemapindex.getMetadata().getCapability().orElse("");
    try {
      Capability capa = Capability.forString(xmlValue);
      if (capa == Capability.RESOURCELIST) {
        countResourceListIndexes++;
      } else if (capa == Capability.CHANGELIST) {
        countChangelistIndexes++;
      } else {
        siResult.addError(new RemoteResourceSyncFrameworkException("Unexpected capability on sitemapindex: " + xmlValue));
        errorResults.add(siResult);
        logger.error("Unexpected capability on sitemapindex '{}' : {}", xmlValue, siResult);
      }
    } catch (IllegalArgumentException e) {
      siResult.addError(e);
      errorResults.add(siResult);
      logger.error("Invalid capability on sitemapindex '{}' : {}", xmlValue, siResult);
    }
  }

  private void analyzeUrlset(Result<Urlset> usResult) {
    Urlset urlset = usResult.getContent().orElse(null);
    String xmlValue = urlset.getMetadata().getCapability().orElse("");
    try {
      Capability capa = Capability.forString(xmlValue);
      if (capa == Capability.RESOURCELIST) {
        analyzeResourceList(usResult);
      } else if (capa == Capability.CHANGELIST) {
        analyzeChangeList(usResult);
      } else if (capa == Capability.CAPABILITYLIST) {
        countCapabilityLists++;
      } else {
        unhandledResults.add(usResult);
        logger.warn("Cannot handle urlsets with capability {} : {}", xmlValue, usResult);
      }
    } catch (IllegalArgumentException e) {
      usResult.addError(e);
      errorResults.add(usResult);
      logger.error("Invalid capability on sitemapindex '{}' : {}", xmlValue, usResult);
    }
  }

  private void analyzeResourceList(Result<Urlset> usResult) {
    Urlset resourcelist = usResult.getContent().orElse(null);
    ZonedDateTime listAt;
    // ultimate date for resourceLists is in required md:at attribute
    Optional<ZonedDateTime> maybeListAt = resourcelist.getMetadata().getAt();
    if (maybeListAt.isPresent()) {
      listAt = maybeListAt.get();
      if (listAt.isAfter(ultimateResourceListAt)) ultimateResourceListAt = listAt;
    } else {
      usResult.addError(new RemoteResourceSyncFrameworkException("Missing required md:at attribute on resourceList"));
      errorResults.add(usResult);
      logger.warn("Missing required md:at attribute on resourceList at {}", usResult);
      return;
    }

    if (listAt.isAfter(getAsOfDateTime())) {
      countResourceLists++;

      // walk item list
      for (UrlItem item : resourcelist.getItemList()) {
        countRemain++;

        // set rs:at on item if not present
        Optional<ZonedDateTime> maybeAt = item.getMetadata().flatMap(RsMd::getAt);
        if (!maybeAt.isPresent()) item.getMetadata().map(rsMd1 -> rsMd1.withAt(listAt));

        // merge item with recentItems
        mergeItem(usResult, item);
      }
    } else {
      logger.info("Skipping resourceList because {} <= {}: {}", listAt, getAsOfDateTime(), usResult);
    }
  }

  private void analyzeChangeList(Result<Urlset> usResult) {


    Urlset changelist = usResult.getContent().orElse(null);
    ZonedDateTime listFrom;

    // ultimate date for changeLists is in required md:from attribute
    Optional<ZonedDateTime> maybeListFrom = changelist.getMetadata().getFrom();
    if (maybeListFrom.isPresent()) {
      listFrom = maybeListFrom.get();
      if (listFrom.isAfter(ultimateChangeListFrom)) ultimateChangeListFrom = listFrom;
    } else {
      usResult.addError(new RemoteResourceSyncFrameworkException("Missing required md:from attribute on changeList"));
      errorResults.add(usResult);
      logger.warn("Missing required md:from attribute on changeList: {}", usResult);
      return;
    }

    if (listFrom.isAfter(getAsOfDateTime())) {
      countChangeLists++;

      // walk item list
      for (UrlItem item : changelist.getItemList()) {

        // set rs:datetime on item if not present
        Optional<ZonedDateTime> dateTime = item.getMetadata().flatMap(RsMd::getDateTime);
        if (!dateTime.isPresent()) item.getMetadata().map(rsMd1 -> rsMd1.withFrom(listFrom));

        // keep count of changes
        Optional<String> maybeChange = item.getMetadata().flatMap(RsMd::getChange);
        if (maybeChange.isPresent()) {
          String change = maybeChange.get();
          if (CH_CREATED.equalsIgnoreCase(change)) {
            countCreated++;
          } else if (CH_UPDATED.equalsIgnoreCase(change)) {
            countUpdated++;
          } else if (CH_DELETED.equalsIgnoreCase(change)) {
            countDeleted++;
          } else {
            usResult.addError(
              new RemoteResourceSyncFrameworkException("Unrecognized md:change attribute on changeList: " + change));
            errorResults.add(usResult);
            logger.warn("Unrecognized md:change attribute on changeList '{} : {}", change, usResult);
            return;
          }
        } else {
          usResult.addError(new RemoteResourceSyncFrameworkException("Missing required md:change attribute on changeList"));
          errorResults.add(usResult);
          logger.warn("Missing required md:change attribute on changeList: {}", usResult);
          return;
        }

        // merge item with recentItems
        mergeItem(usResult, item);
      }
    } else {
      logger.info("Skipping changeList because {} <= {}: {}", listFrom, getAsOfDateTime(), usResult);
    }
  }

  private void mergeItem(Result<Urlset> usResult, UrlItem item) {
    Optional<URI> maybeUri = NormURI.normalize(item.getLoc());
    if (maybeUri.isPresent()) {
      recentItems.merge(maybeUri.get(), item, UrlItem::latest);
    } else {
      usResult.addError(new RemoteResourceSyncFrameworkException("Missing required loc element on urlItem"));
      errorResults.add(usResult);
      logger.warn("Missing required loc element on urlItem: {}", usResult);
    }
  }

  private PathFinder getCurrentPathFinder() {
    if (currentPathFinder == null) throw new IllegalStateException("No current PathFinder");
    return currentPathFinder;
  }


}
