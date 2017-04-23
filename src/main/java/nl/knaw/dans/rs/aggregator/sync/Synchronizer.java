package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.discover.ResultIndex;
import nl.knaw.dans.rs.aggregator.discover.RsExplorer;
import nl.knaw.dans.rs.aggregator.discover.UrlSetPivot;
import nl.knaw.dans.rs.aggregator.http.Result;
import nl.knaw.dans.rs.aggregator.http.UriRegulator;
import nl.knaw.dans.rs.aggregator.xml.Capability;
import nl.knaw.dans.rs.aggregator.xml.ResourceSyncContext;
import nl.knaw.dans.rs.aggregator.xml.RsMd;
import nl.knaw.dans.rs.aggregator.xml.UrlItem;
import nl.knaw.dans.rs.aggregator.xml.Urlset;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created on 2017-04-15 14:36.
 */
public class Synchronizer {

  private final static Logger logger = LoggerFactory.getLogger(Synchronizer.class);

  private static final String NULL_DATE = "2000-01-01T00:00:00.000Z";
  private static final String CH_CREATED = "created";
  private static final String CH_UPDATED = "updated";
  private static final String CH_DELETED = "deleted";
  private static final String CH_REMAIN = "remain";

  private final PathFinder pathFinder;

  private Reporter reporter;
  private RsDocumentReader rsDocReader;
  private CloseableHttpClient httpClient;
  private ResourceSyncContext rsContext;
  private boolean trialRun = false;

  private Map<URI, UrlItem> resourceItems;
  private Map<URI, UrlItem> createdItems;
  private Map<URI, UrlItem> updatedItems;
  private Map<URI, UrlItem> deletedItems;
  private ZonedDateTime ultimateResourceListAt;
  private ZonedDateTime ultimateChangeListFrom;

  public Synchronizer(PathFinder pathFinder) {
    this.pathFinder = pathFinder;
    this.reporter = reporter;
  }

  public RsDocumentReader getRsDocReader() {
    if (rsDocReader == null) {
      rsDocReader = new RsDocumentReader(getHttpClient(), getRsContext());
    }
    return rsDocReader;
  }

  public Synchronizer withRsDocReader(RsDocumentReader rsDocReader) {
    this.rsDocReader = rsDocReader;
    return this;
  }

  private CloseableHttpClient getHttpClient() {
    if (httpClient == null) {
      httpClient = HttpClients.createDefault();
    }
    return httpClient;
  }

  public Synchronizer withHttpClient(CloseableHttpClient httpClient) {
    this.httpClient = httpClient;
    return this;
  }

  private ResourceSyncContext getRsContext() {
    if (rsContext == null) {
      try {
        rsContext = new ResourceSyncContext();
      } catch (JAXBException e) {
        throw new RuntimeException(e);
      }
    }
    return rsContext;
  }

  public Synchronizer withRsContext(ResourceSyncContext rsContext) {
    this.rsContext = rsContext;
    return this;
  }

  public Reporter getReporter() {
    if (reporter == null) {
      reporter = new Reporter();
    }
    return reporter;
  }

  public Synchronizer withReporter(Reporter reporter) {
    this.reporter = reporter;
    return this;
  }

  public boolean isTrialRun() {
    return trialRun;
  }

  public void setTrialRun(boolean trialRun) {
    this.trialRun = trialRun;
  }

  public Map<URI, UrlItem> getResourceItems() {
    return resourceItems;
  }

  public Map<URI, UrlItem> getCreatedItems() {
    return createdItems;
  }

  public Map<URI, UrlItem> getUpdatedItems() {
    return updatedItems;
  }

  public Map<URI, UrlItem> getDeletedItems() {
    return deletedItems;
  }

  public ZonedDateTime getUltimateResourceListAt() {
    return ultimateResourceListAt;
  }

  public ZonedDateTime getUltimateChangeListFrom() {
    return ultimateChangeListFrom;
  }

  public void synchronize() {
    reset();
    readRemoteItems();
    syncLocalResources();
  }

  private void reset() {
    resourceItems = new HashMap<>();
    createdItems = new HashMap<>();
    updatedItems = new HashMap<>();
    deletedItems = new HashMap<>();

    ultimateResourceListAt = ZonedDateTime.parse(NULL_DATE).withZoneSameInstant(ZoneOffset.UTC);
    ultimateChangeListFrom = ZonedDateTime.parse(NULL_DATE).withZoneSameInstant(ZoneOffset.UTC);
  }

  /**
   * - Takes capabilityList URI from pathFinder.
   * - Reads remote rs documents from capabilityList down.
   * - Fills the item maps with key=normalized URI, value=UrlItem.
   * - Sets ultimate (latest) dates found on resourceLists and changeLists.
   */
  private void readRemoteItems() {
    RsExplorer explorer = new RsExplorer(getHttpClient(), getRsContext())
      .withFollowChildLinks(true)
      .withFollowIndexLinks(false)
      .withFollowParentLinks(false);
    ResultIndex index = explorer.explore(pathFinder.getCapabilityListUri());
    getReporter().reportResults(index);

    UrlSetPivot pivot = new UrlSetPivot(index);
    if (pivot.listErrorResults().size() > 0) {
      logger.warn("Not synchronizing because of previous errors: {}", pathFinder.getCapabilityListUri());
      return;
    }

    for (Result<Urlset> result : pivot.listSortedUrlsetResults(Capability.RESOURCELIST)) {
      if (result.getContent().isPresent()) {
        Urlset resourcelist = result.getContent().get();
        ZonedDateTime at = ultimateResourceListAt;

        // ultimate date for resourceLists is in required md:at attribute
        RsMd rsMd = resourcelist.getMetadata();
        if (rsMd.getAt().isPresent()) {
          at = rsMd.getAt().get();
          if (at.isAfter(ultimateResourceListAt)) ultimateResourceListAt = at;
        } else {
          logger.warn("Missing required md:at attribute on resourceList at {}", result.getUri());
        }

        // add items to resourceItems
        for (UrlItem item : resourcelist.getItemList()) {
          Optional<URI> maybeUri = UriRegulator.regulate(item.getLoc());
          if (maybeUri.isPresent()) {
            ZonedDateTime finalAt = at;
            item.getMetadata().map(rsMd1 -> rsMd1.withAt(finalAt));
            resourceItems.merge(maybeUri.get(), item, UrlItem::latest);
          } else {
            logger.warn("Missing required loc element in an item in resourceList at {}", result.getUri());
          }
        }
      } else {
        logger.warn("No content for resourceList at {}", result.getUri());
      }
    }

    for (Result<Urlset> result : pivot.listSortedUrlsetResults(Capability.CHANGELIST)) {
      if (result.getContent().isPresent()) {
        Urlset changelist = result.getContent().get();
        ZonedDateTime from = ultimateChangeListFrom;

        // ultimate date for changeLists is in required md:from attribute
        RsMd rsMd = changelist.getMetadata();
        if (rsMd.getFrom().isPresent()) {
          from = rsMd.getFrom().get();
          if (from.isAfter(ultimateChangeListFrom)) ultimateChangeListFrom = from;
        } else {
          logger.warn("Missing required md:from attribute on changeList at {}", result.getUri());
        }

        // add items to created-, updated-, deletedItems
        for (UrlItem item : changelist.getItemList()) {
          Optional<URI> maybeUri = UriRegulator.regulate(item.getLoc());
          if (maybeUri.isPresent()) {
            URI locUri = maybeUri.get();
            Optional<ZonedDateTime> dateTime = item.getMetadata().flatMap(RsMd::getDateTime);
            if (!dateTime.isPresent()) {
              ZonedDateTime finalFrom = from;
              item.getMetadata().map(rsMd1 -> rsMd1.withFrom(finalFrom));
            }
            Optional<String> maybeChange = item.getMetadata().flatMap(RsMd::getChange);
            if (maybeChange.isPresent()) {
              String change = maybeChange.get();
              if (CH_CREATED.equalsIgnoreCase(change)) {
                createdItems.merge(locUri, item, UrlItem::latest);
              } else if (CH_UPDATED.equalsIgnoreCase(change)) {
                updatedItems.merge(locUri, item, UrlItem::latest);
              } else if (CH_DELETED.equalsIgnoreCase(change)) {
                deletedItems.merge(locUri, item, UrlItem::latest);
              } else {
                logger.warn("Unrecognized change '{}' in md:change attribute on an item in changeList at {}",
                  change, result.getUri());
              }
            } else {
              logger.warn("Missing required md:change attribute on an item in changeList at {}", result.getUri());
            }
          } else {
            logger.warn("Missing required loc element in an item in changeList at {}", result.getUri());
          }
        }
      } else {
        logger.warn("No content for changeList at {}", result.getUri());
      }
    }
  }

  private void syncLocalResources() {
    for (Map.Entry<URI, UrlItem> entry : resourceItems.entrySet()) {
      updateItem(entry.getKey(), entry.getValue(), CH_REMAIN);
    }

    for (Map.Entry<URI, UrlItem> entry : updatedItems.entrySet()) {
      updateItem(entry.getKey(), entry.getValue(), CH_UPDATED);
    }

    for (Map.Entry<URI, UrlItem> entry : createdItems.entrySet()) {
      updateItem(entry.getKey(), entry.getValue(), CH_CREATED);
    }
  }

  private void updateItem(URI locUri, UrlItem item, String change) {
    UrlItem latestItem = latest(locUri, item, change);
    String state = latestItem.getMetadata().flatMap(RsMd::getChange).orElse(CH_REMAIN);
    File resourcePath = pathFinder.findResourceFilePath(locUri);
    if (resourcePath.exists()) {
      if (CH_DELETED.equals(state)) {
        // remove resource
        logger.debug("{} Removing {}", state, locUri);
      } else {
        // verify hash last mod, length, if neither download
        logger.debug("{} Verifying {}", state, locUri);
      }
    } else { // resource does not exist
      if (!CH_DELETED.equals(state)) {
        // download
        logger.debug("{} Downloading {}", state, locUri);
      }
    }
  }

  private UrlItem latest(URI uri, UrlItem item, String change) {
    UrlItem uri2 = item;
    if (!CH_CREATED.equals(change)) uri2 = uri2.latest(createdItems.remove(uri));
    if (!CH_UPDATED.equals(change)) uri2 = uri2.latest(updatedItems.remove(uri));
    uri2 = uri2.latest(deletedItems.remove(uri));
    return uri2;
  }

}
