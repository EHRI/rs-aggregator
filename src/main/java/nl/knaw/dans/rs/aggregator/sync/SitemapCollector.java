package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.discover.RemoteResourceSyncFrameworkException;
import nl.knaw.dans.rs.aggregator.discover.ResultIndex;
import nl.knaw.dans.rs.aggregator.discover.RsExplorer;
import nl.knaw.dans.rs.aggregator.http.Result;
import nl.knaw.dans.rs.aggregator.http.NormURI;
import nl.knaw.dans.rs.aggregator.util.LambdaUtil;
import nl.knaw.dans.rs.aggregator.xml.Capability;
import nl.knaw.dans.rs.aggregator.xml.ResourceSyncContext;
import nl.knaw.dans.rs.aggregator.xml.RsBuilder;
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
public class SitemapCollector {

  private static Logger logger = LoggerFactory.getLogger(SitemapCollector.class);

  private static final String NULL_DATE = "2000-01-01T00:00:00.000Z";
  private static final String CH_CREATED = "created";
  private static final String CH_UPDATED = "updated";
  private static final String CH_DELETED = "deleted";

  private final PathFinder pathFinder;

  private CloseableHttpClient httpClient;
  private ResourceSyncContext rsContext;

  private boolean collected;

  private Set<String> invalidUris;
  private List<Result<?>> errorResults;
  private List<Result<?>> unhandledResults;
  private Map<URI, UrlItem> latestItems;
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

  public SitemapCollector(PathFinder pathFinder) {
    this.pathFinder = pathFinder;
  }

  private PathFinder getPathFinder() {
    return pathFinder;
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

  public boolean isCollected() {
    return collected;
  }

  public Set<String> getInvalidUris() {
    if (!collected) collectSitemaps();
    return invalidUris;
  }

  public List<Result<?>> getErrorResults() {
    if (!collected) collectSitemaps();
    return errorResults;
  }

  public List<Result<?>> getUnhandledResults() {
    if (!collected) collectSitemaps();
    return unhandledResults;
  }

  public boolean hasErrors() {
    if (!collected) collectSitemaps();
    return invalidUris.isEmpty() && errorResults.isEmpty() && unhandledResults.isEmpty();
  }

  public int getCountCapabilityLists() {
    if (!collected) collectSitemaps();
    return countCapabilityLists;
  }

  public int getCountResourceListIndexes() {
    if (!collected) collectSitemaps();
    return countResourceListIndexes;
  }

  public int getCountChangelistIndexes() {
    if (!collected) collectSitemaps();
    return countChangelistIndexes;
  }

  public int getCountResourceLists() {
    if (!collected) collectSitemaps();
    return countResourceLists;
  }

  public int getCountChangeLists() {
    if (!collected) collectSitemaps();
    return countChangeLists;
  }

  public int getCountRemain() {
    if (!collected) collectSitemaps();
    return countRemain;
  }

  public int getCountCreated() {
    if (!collected) collectSitemaps();
    return countCreated;
  }

  public int getCountUpdated() {
    if (!collected) collectSitemaps();
    return countUpdated;
  }

  public int getCountDeleted() {
    if (!collected) collectSitemaps();
    return countDeleted;
  }

  public ZonedDateTime getUltimateResourceListAt() {
    if (!collected) collectSitemaps();
    return ultimateResourceListAt;
  }

  public ZonedDateTime getUltimateChangeListFrom() {
    if (!collected) collectSitemaps();
    return ultimateChangeListFrom;
  }

  public ZonedDateTime getUltmateListDate() {
    if (!collected) collectSitemaps();
    return ultimateChangeListFrom.isAfter(ultimateResourceListAt) ? ultimateChangeListFrom : ultimateResourceListAt;
  }

  public Map<URI, UrlItem> getLatestItems() {
    if (!collected) collectSitemaps();
    return latestItems;
  }

  public void collectSitemaps() {
    reset();
    RsExplorer explorer = new RsExplorer(getHttpClient(), getRsContext())
      .withConverter(fileSavingConverter)
      .withFollowChildLinks(true)
      .withFollowIndexLinks(false)
      .withFollowParentLinks(false);
    ResultIndex index = explorer.explore(pathFinder.getCapabilityListUri());

    invalidUris = index.getInvalidUris();

    for (Result<?> result : index.getResultMap().values()) {
      if (result.hasErrors()) {
        errorResults.add(result);
      } else {
        analyze(result);
      }
    }
    collected = true;
  }

  private void reset() {
    errorResults = new ArrayList<>();
    unhandledResults = new ArrayList<>();
    latestItems = new HashMap<>();

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
    countResourceLists++;

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

    // walk item list
    for (UrlItem item : resourcelist.getItemList()) {
      countRemain++;

      // set rs:at on item if not present
      Optional<ZonedDateTime> maybeAt = item.getMetadata().flatMap(RsMd::getAt);
      if (!maybeAt.isPresent()) item.getMetadata().map(rsMd1 -> rsMd1.withAt(listAt));

      // merge item with latestItems
      mergeItem(usResult, item);
    }
  }

  private void analyzeChangeList(Result<Urlset> usResult) {
    countChangeLists++;

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

      // merge item with latestItems
      mergeItem(usResult, item);
    }
  }

  private void mergeItem(Result<Urlset> usResult, UrlItem item) {
    Optional<URI> maybeUri = NormURI.normalize(item.getLoc());
    if (maybeUri.isPresent()) {
      latestItems.merge(maybeUri.get(), item, UrlItem::latest);
    } else {
      usResult.addError(new RemoteResourceSyncFrameworkException("Missing required loc element on urlItem"));
      errorResults.add(usResult);
      logger.warn("Missing required loc element on urlItem: {}", usResult);
    }
  }

  private LambdaUtil.BiFunction_WithExceptions<URI, HttpResponse, RsRoot, Exception>
    fileSavingConverter = (uri, response) -> {

    HttpEntity entity = response.getEntity();
    RsRoot rsRoot = null;
    if (entity != null) {
      File file = getPathFinder().findMetadataFilePath(uri);
      File directoryPath = file.getParentFile();
      if (directoryPath.mkdirs()) logger.debug("Created directory path {}", directoryPath);
      InputStream instream = entity.getContent();
      boolean saved = saveFile(instream, file);
      if (saved) {
        logger.debug("Saved {} --> {}", uri, file);
        Header lmh = response.getFirstHeader("Last-Modified");
        if (lmh != null) {
          Date date = DateUtils.parseDate(lmh.getValue());
          if (file.setLastModified(date.getTime())) logger.debug("Last modified from remote: {} on {}", date, file);
        }
        rsRoot = new RsBuilder(getRsContext()).setFile(file).build().orElse(null);
        if (rsRoot != null) {
          logger.debug("Collected sitemap with capability {} from {}", rsRoot.getCapability(), uri);
        }
      }
    }
    return rsRoot;
  };

  private boolean saveFile(InputStream instream, File file) throws IOException {
    boolean saved = false;
    OutputStream outstream = new FileOutputStream(file);
    byte[] buffer = new byte[8 * 1024];
    int bytesRead;
    try {
      while ((bytesRead = instream.read(buffer)) != -1) {
        outstream.write(buffer, 0, bytesRead);
      }
      saved = true;
    } finally {
      IOUtils.closeQuietly(instream);
      IOUtils.closeQuietly(outstream);
    }
    return saved;
  }
}
