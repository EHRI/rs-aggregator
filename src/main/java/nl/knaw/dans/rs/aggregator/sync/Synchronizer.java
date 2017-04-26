package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.discover.ResultIndex;
import nl.knaw.dans.rs.aggregator.discover.RsExplorer;
import nl.knaw.dans.rs.aggregator.discover.UrlSetPivot;
import nl.knaw.dans.rs.aggregator.http.Result;
import nl.knaw.dans.rs.aggregator.http.NormURI;
import nl.knaw.dans.rs.aggregator.xml.Capability;
import nl.knaw.dans.rs.aggregator.xml.ResourceSyncContext;
import nl.knaw.dans.rs.aggregator.xml.RsMd;
import nl.knaw.dans.rs.aggregator.xml.UrlItem;
import nl.knaw.dans.rs.aggregator.xml.Urlset;
import org.apache.commons.io.IOUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created on 2017-04-15 14:36.
 */
public class Synchronizer {

  private final static Logger logger = LoggerFactory.getLogger(Synchronizer.class);

  private static final int MAX_DOWNLOADS = Integer.MAX_VALUE;
  private static final int MAX_DOWNLOAD_RETRY = 3;
  private static final String NULL_DATE = "2000-01-01T00:00:00.000Z";

  private static final String CH_CREATED = "created";
  private static final String CH_UPDATED = "updated";
  private static final String CH_DELETED = "deleted";
  private static final String CH_REMAIN = "remain";

  private final PathFinder pathFinder;

  private Reporter reporter;
  private RsDocumentReader rsDocReader;
  private ResourceReader resourceReader;
  private CloseableHttpClient httpClient;
  private ResourceSyncContext rsContext;
  private VerificationPolicy verificationPolicy;
  private int maxDownloads = MAX_DOWNLOADS;
  private int maxDownloadRetry = MAX_DOWNLOAD_RETRY;
  private boolean trialRun = false;

  private Map<URI, UrlItem> resourceItems;
  private Map<URI, UrlItem> createdItems;
  private Map<URI, UrlItem> updatedItems;
  private Map<URI, UrlItem> deletedItems;
  private ZonedDateTime ultimateResourceListAt;
  private ZonedDateTime ultimateChangeListFrom;
  private int itemCount;
  private int itemsRemoved;
  private int itemsDownloaded;
  private int itemsVerified;
  private int itemsNoAction; // change='deleted' and resource does not exists.
  private int failedRemoves;
  private int failedDownloads;
  private int failedVerifications;

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

  public ResourceReader getResourceReader() {
    if (resourceReader == null) {
      resourceReader = new ResourceReader(getHttpClient());
    }
    return resourceReader;
  }

  public VerificationPolicy getVerificationPolicy() {
    if (verificationPolicy == null) {
      verificationPolicy = new DefaultVerificationPolicy();
    }
    return verificationPolicy;
  }

  public Synchronizer withVerificationPolicy(VerificationPolicy verificationPolicy) {
    this.verificationPolicy = verificationPolicy;
    return this;
  }

  public Synchronizer withResourceReader(ResourceReader resourceReader) {
    this.resourceReader = resourceReader;
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

  public int getMaxDownloads() {
    return maxDownloads;
  }

  public Synchronizer withMaxDownloads(int maxDownloads) {
    this.maxDownloads = maxDownloads;
    return this;
  }

  public int getMaxDownloadRetry() {
    return maxDownloadRetry;
  }

  public Synchronizer withMaxDownloadRetry(int maxDownloadRetry) {

    this.maxDownloadRetry = maxDownloadRetry;
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

    itemCount = 0;
    itemsDownloaded = 0;
    itemsVerified = 0;
    itemsRemoved = 0;
    itemsNoAction = 0;
    failedDownloads = 0;
    failedVerifications = 0;
    failedRemoves = 0;
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
          Optional<URI> maybeUri = NormURI.normalize(item.getLoc());
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
          Optional<URI> maybeUri = NormURI.normalize(item.getLoc());
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
    itemCount = 0;
    for (Map.Entry<URI, UrlItem> entry : resourceItems.entrySet()) {
      updateItem(entry.getKey(), entry.getValue(), CH_REMAIN);
    }

    for (Map.Entry<URI, UrlItem> entry : updatedItems.entrySet()) {
      updateItem(entry.getKey(), entry.getValue(), CH_UPDATED);
    }

    for (Map.Entry<URI, UrlItem> entry : createdItems.entrySet()) {
      updateItem(entry.getKey(), entry.getValue(), CH_CREATED);
    }
    int failures = failedDownloads + failedRemoves + failedVerifications;
    logger.info("====> items={}, failures={} [success/failures] newly downloaded={}/{}, verified={}/{}, removed={}/{}, " +
        "no_action={}, trial run={}, resource set={}",
      itemCount, failures, itemsDownloaded, failedDownloads, itemsVerified, failedVerifications,
      itemsRemoved, failedRemoves, itemsNoAction, trialRun, pathFinder.getCapabilityListUri());
  }

  private void updateItem(URI locUri, UrlItem item, String change) {
    itemCount++;
    UrlItem latestItem = latest(locUri, item, change);
    String state = latestItem.getMetadata().flatMap(RsMd::getChange).orElse(CH_REMAIN);
    File resourcePath = pathFinder.findResourceFilePath(locUri);
    // existing resources
    if (resourcePath.exists()) {
      if (CH_DELETED.equals(state)) {
        logger.debug("------> {} State={}. Removing {}", itemCount, state, locUri);
        boolean removed = remove(locUri, latestItem, resourcePath);
        if (removed) {
          itemsRemoved++;
        } else {
          failedRemoves++;
        }
      } else {
        logger.debug("------> {} State={}. Verifying {}", itemCount, state, locUri);
        boolean verified = verify(locUri, latestItem, resourcePath, 0);
        if (verified) {
          itemsVerified++;
        } else {
          failedVerifications++;
        }
      }
    // missing resources
    } else { // resource does not exist
      if (!CH_DELETED.equals(state)) {
        logger.debug("------> {} State={}. Downloading {}", itemCount, state, locUri);
        boolean downloaded = download(locUri, latestItem, resourcePath, 0);
        if (downloaded) {
          itemsDownloaded++;
        } else {
          failedDownloads++;
        }
      } else {
        logger.debug("------> {} State={}. No action {}", itemCount, state, locUri);
        itemsNoAction++;
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

  private boolean download(URI locUri, UrlItem item, File resourcePath, int downloadCounter) {

    if (++downloadCounter > getMaxDownloadRetry()) {
      logger.warn("Giving up on download because retry exceeds {}: {}", getMaxDownloadRetry(), locUri);
      return false;
    }
    if (trialRun) {
      logger.debug("Trial run. Not downloading: {}", locUri);
      verify(locUri, item, resourcePath, downloadCounter);
      return false;
    }
    if (itemsDownloaded >= maxDownloads) {
      logger.debug("Stopped downloading. itemsDownloaded ({}) >= maxDownloads ({}). Not downloading: {}"
        , itemsDownloaded, maxDownloads, locUri);
      verify(locUri, item, resourcePath, downloadCounter);
      return false;
    }

    boolean downloaded = false;
    Result<File> result = getResourceReader().read(locUri, resourcePath);
    if (result.getContent().isPresent()) {
      downloaded = verify(locUri, item, resourcePath, downloadCounter);
    } else {
      logger.warn("Failed download: ", result.lastError());
    }
    return downloaded;
  }

  private boolean verify(URI locUri, UrlItem item, File resourcePath, int downloadCounter) {
    VerificationPolicy policy = getVerificationPolicy();
    VerificationStatus stHash = VerificationStatus.not_verified;
    VerificationStatus stLastMod = VerificationStatus.not_verified;
    VerificationStatus stLength = VerificationStatus.not_verified;

    if (resourcePath.exists()) {
      if (policy.continueVerification(stHash, stLastMod, stLength)) {
        Optional<String> maybeHash = item.getMetadata().flatMap(RsMd::getHash);
        if (maybeHash.isPresent()) {
          stHash = verifyHash(resourcePath, maybeHash.get(), locUri);
        }
      }

      if (policy.continueVerification(stHash, stLastMod, stLength)) {
        Optional<ZonedDateTime> maybeLastModified = item.getLastmod();
        if (maybeLastModified.isPresent()) {
          stLastMod = verifyLastMod(resourcePath, maybeLastModified.get(), locUri);
        }
      }

      if (policy.continueVerification(stHash, stLastMod, stLength)) {
        Optional<Long> maybeSize = item.getMetadata().flatMap(RsMd::getLength);
        if (maybeSize.isPresent()) {
          stLength = verifyLength(resourcePath, maybeSize.get(), locUri);
        }
      }
    }

    boolean verified = false;
    if (policy.repeatDownload(stHash, stLastMod, stLength, downloadCounter, resourcePath.exists())) {
      if (trialRun) {
        logger.debug("Trial run. Not repeating download: {}", locUri);
      } else if (itemsDownloaded >= maxDownloads) {
        logger.debug("Max downloads reached. Not downloading: {}", locUri);
      } else {
        logger.info("Repeating download. stHash={}, stLastMod={}, stLength={}, download count={}, uri={}",
          stHash, stLastMod, stLength, downloadCounter, locUri);
        verified = download(locUri, item, resourcePath, downloadCounter);
      }
    }

    if (!verified) {
      verified = policy.isVerified(stHash, stLastMod, stLength, resourcePath.exists());
      if (!verified)  logger.warn("Resource not verified: {}", locUri);
    }
    return verified;
  }

  private VerificationStatus verifyHash(File resourcePath, String hash, URI locUri) {
    VerificationStatus status = VerificationStatus.not_verified;
    String algorithm = "md5";
    String remoteHash = hash;
    String localHash = null;
    String[] splitHash = hash.split(":");
    if (splitHash.length > 1) {
      algorithm = splitHash[0];
      remoteHash = splitHash[1];
    }
    FileInputStream fis = null;
    try {
      MessageDigest digest = MessageDigest.getInstance(algorithm);
      fis = new FileInputStream(resourcePath);
      byte[] byteArray = new byte[1024];
      int bytesCount = 0;
      while ((bytesCount = fis.read(byteArray)) != -1) {
        digest.update(byteArray, 0, bytesCount);
      };
      byte[] bytes = digest.digest();
      StringBuilder sb = new StringBuilder();
      for (byte aByte : bytes) {
        sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
      }
      localHash = sb.toString();
      if (remoteHash.equalsIgnoreCase(localHash)) {
        status = VerificationStatus.verification_success;
        logger.debug("Verified {} hash of {}.", algorithm, locUri);
      } else {
        status = VerificationStatus.verification_failure;
        logger.warn("Hash failure. algorithm={}, remote={}, local={}, uri={}", algorithm, remoteHash, localHash, locUri);
      }
    } catch (NoSuchAlgorithmException e) {
      logger.warn("No such algorithm for hash: '{}'. Found on {}", algorithm, locUri, e);
      status = VerificationStatus.verification_error;
    } catch (FileNotFoundException e) {
      logger.warn("While verifying hash on {}: ", locUri, e);
      status = VerificationStatus.verification_error;
    } catch (IOException e) {
      logger.warn("While verifying hash on {}", locUri, e);
      status = VerificationStatus.verification_error;
    } finally {
      IOUtils.closeQuietly(fis);
    }
    return status;
  }

  private VerificationStatus verifyLastMod(File resourcePath, ZonedDateTime zdt, URI locUri) {
    // FYI: if remote renames a directory, the files in that directory may get different timestamps,
    // even though the files did not change.
    VerificationStatus status = VerificationStatus.not_verified;
    long remoteLastMod = Date.from(zdt.toInstant()).getTime();
    long localLastMod = resourcePath.lastModified();
    if (remoteLastMod == localLastMod) {
      status = VerificationStatus.verification_success;
      logger.debug("Verified Last-modified of {}", locUri);
    } else {
      status = VerificationStatus.verification_failure;
      logger.warn("Last-modified not equal. remote={} ({}), local={} ({}), uri={}",
        remoteLastMod, new Date(remoteLastMod),
        localLastMod, new Date(localLastMod), locUri);
    }
    return status;
  }

  private VerificationStatus verifyLength(File resourcePath, Long remoteSize, URI locUri) {
    VerificationStatus status = VerificationStatus.not_verified;
    long localSize = resourcePath.length();
    if (remoteSize == localSize) {
      status = VerificationStatus.verification_success;
      logger.debug("Verified Length of {}", locUri);
    } else {
      status = VerificationStatus.verification_failure;
      logger.warn("Length not equal. remote={}, local={}, uri={}", remoteSize, localSize, locUri);
    }
    return status;
  }

  private boolean remove(URI locUri, UrlItem item, File resourcePath) {
    boolean removed = false;
    if (resourcePath.exists()) {
      removed = resourcePath.delete();
      if (!removed) {
        logger.warn("Resource not removed: {} --> {}", locUri, resourcePath);
      } else {
        logger.debug("Resource removed: {} --> {}", locUri, resourcePath);
      }
    }
    return removed;
  }

}
