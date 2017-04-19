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

  private final PathFinder pathFinder;

  private Reporter reporter;
  private RsDocumentReader rsDocReader;
  private CloseableHttpClient httpClient;
  private ResourceSyncContext rsContext;

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

  public void setRsDocReader(RsDocumentReader rsDocReader) {
    this.rsDocReader = rsDocReader;
  }

  private CloseableHttpClient getHttpClient() {
    if (httpClient == null) {
      httpClient = HttpClients.createDefault();
    }
    return httpClient;
  }

  public void setHttpClient(CloseableHttpClient httpClient) {
    this.httpClient = httpClient;
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

  public void setRsContext(ResourceSyncContext rsContext) {
    this.rsContext = rsContext;
  }

  public Reporter getReporter() {
    if (reporter == null) {
      reporter = new Reporter();
    }
    return reporter;
  }

  public void setReporter(Reporter reporter) {
    this.reporter = reporter;
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
  }

  private void reset() {
    resourceItems = new HashMap<>();
    createdItems = new HashMap<>();
    updatedItems = new HashMap<>();
    deletedItems = new HashMap<>();
    ultimateResourceListAt = ZonedDateTime.parse(NULL_DATE).withZoneSameInstant(ZoneOffset.UTC);
    ultimateChangeListFrom = ZonedDateTime.parse(NULL_DATE).withZoneSameInstant(ZoneOffset.UTC);
  }

  void readRemoteItems() {
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

        // ultimate date for resourceLists is in required md:at attribute
        RsMd rsMd = resourcelist.getMetadata();
        if (rsMd.getAt().isPresent()) {
          ZonedDateTime at = rsMd.getAt().get();
          if (at.isAfter(ultimateResourceListAt)) ultimateResourceListAt = at;
        } else {
          logger.warn("Missing required md:at attribute on resourceList at {}", result.getUri());
        }

        // add items to resourceItems
        for (UrlItem item : resourcelist.getItemList()) {
          Optional<URI> maybeUri = UriRegulator.regulate(item.getLoc());
          if (maybeUri.isPresent()) {
            resourceItems.put(maybeUri.get(), item);
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

        // ultimate date for changeLists is in required md:from attribute
        RsMd rsMd = changelist.getMetadata();
        if (rsMd.getFrom().isPresent()) {
          ZonedDateTime from = rsMd.getFrom().get();
          if (from.isAfter(ultimateChangeListFrom)) ultimateChangeListFrom = from;
        } else {
          logger.warn("Missing required md:from attribute on changeList at {}", result.getUri());
        }

        // add items to created-, updated-, deletedItems
        for (UrlItem item : changelist.getItemList()) {
          Optional<URI> maybeUri = UriRegulator.regulate(item.getLoc());
          if (maybeUri.isPresent()) {
            URI locUri = maybeUri.get();
            Optional<String> maybeChange = item.getMetadata().flatMap(RsMd::getChange);
            if (maybeChange.isPresent()) {
              String change = maybeChange.get();
              if ("created".equalsIgnoreCase(change)) {
                createdItems.put(locUri, item);
              } else if ("updated".equalsIgnoreCase(change)) {
                updatedItems.put(locUri, item);
              } else if ("deleted".equalsIgnoreCase(change)) {
                deletedItems.put(locUri, item);
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


}
