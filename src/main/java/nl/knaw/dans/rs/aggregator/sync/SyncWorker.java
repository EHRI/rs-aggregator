package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.xml.RsConstants;
import nl.knaw.dans.rs.aggregator.xml.RsMd;
import nl.knaw.dans.rs.aggregator.xml.UrlItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Created on 2017-04-15 14:36.
 */
public class SyncWorker implements RsConstants {

  private static final String CN = SyncWorker.class.getSimpleName() + ".";

  public static final String PROP_SYNC_START = CN + "z1.sync.start";
  public static final String PROP_SYNC_END = CN + "z2.sync.end";

  public static final String PROP_MAX_DOWNLOADS = CN + "a1.max.downloads";
  public static final String PROP_MAX_DOWNLOAD_RETRY = CN + "a2.max.download.retry";
  public static final String PROP_TRIAL_RUN = CN + "a3.trial.run";
  public static final String PROP_SITEMAP_COLLECTOR = CN + "class.sitemap.collector";
  public static final String PROP_RESOURCE_MANAGER = CN + "class.resource.manager";
  public static final String PROP_VERIFICATION_POLICY = CN + "class.verification.policy";

  public static final String PROP_TOTAL_ITEMS = CN + "total.items";
  public static final String PROP_ITEMS_VERIFIED = CN + "items.verified";
  public static final String PROP_ITEMS_DELETED = CN + "items.deleted";
  public static final String PROP_ITEMS_CREATED = CN + "items.created";
  public static final String PROP_ITEMS_UPDATED = CN + "items.updated";
  public static final String PROP_ITEMS_REMAIN = CN + "items.remain";
  public static final String PROP_ITEMS_NO_ACTION = CN + "items.no.action";
  public static final String PROP_TOTAL_FAILED_ITEMS = CN + "total.failed.items";
  public static final String PROP_FAILED_DELETIONS = CN + "failed.deletions";
  public static final String PROP_FAILED_CREATIONS = CN + "failed.creations";
  public static final String PROP_FAILED_UPDATES = CN + "failed.updates";
  public static final String PROP_FAILED_REMAINS = CN + "failed.remains";

  public static final String PROP_TOTAL_DOWNLOAD_COUNT = CN + "total.download.count";

  private final static Logger logger = LoggerFactory.getLogger(SyncWorker.class);

  private static final int MAX_DOWNLOADS = Integer.MAX_VALUE;
  private static final int MAX_DOWNLOAD_RETRY = 3;

  private SitemapCollector sitemapCollector;
  private ResourceManager resourceManager;
  private VerificationPolicy verificationPolicy;

  private int maxDownloads = MAX_DOWNLOADS;
  private int maxDownloadRetry = MAX_DOWNLOAD_RETRY;
  private boolean trialRun = false;

  private int itemCount;
  private int verifiedItems;
  private int itemsDeleted;
  private int itemsCreated;
  private int itemsUpdated;
  private int itemsRemain;
  private int itemsNoAction; // change='deleted' and resource does not exists.
  private int failedDeletions;
  private int failedCreations;
  private int failedUpdates;
  private int failedRemains;

  private int downloadCount;

  public SyncWorker() {

  }

  public SitemapCollector getSitemapCollector() {
    if (sitemapCollector == null) {
      sitemapCollector = new SitemapCollector();
    }
    return sitemapCollector;
  }

  public SyncWorker withSitemapCollector(SitemapCollector sitemapCollector) {
    this.sitemapCollector = sitemapCollector;
    return this;
  }

  public ResourceManager getResourceManager() {
    if (resourceManager == null) {
      resourceManager = new FsResourceManager();
    }
    return resourceManager;
  }

  public SyncWorker withResourceManager(ResourceManager resourceManager) {
    this.resourceManager = resourceManager;
    return this;
  }

  public VerificationPolicy getVerificationPolicy() {
    if (verificationPolicy == null) {
      verificationPolicy = new DefaultVerificationPolicy();
    }
    return verificationPolicy;
  }

  public SyncWorker withVerificationPolicy(VerificationPolicy verificationPolicy) {
    this.verificationPolicy = verificationPolicy;
    return this;
  }

  public int getMaxDownloads() {
    return maxDownloads;
  }

  public SyncWorker withMaxDownloads(int maxDownloads) {
    this.maxDownloads = maxDownloads;
    return this;
  }

  public int getMaxDownloadRetry() {
    return maxDownloadRetry;
  }

  public SyncWorker withMaxDownloadRetry(int maxDownloadRetry) {
    this.maxDownloadRetry = maxDownloadRetry;
    return this;
  }

  public boolean isTrialRun() {
    return trialRun;
  }

  public SyncWorker withTrialRun(boolean trialRun) {
    this.trialRun = trialRun;
    return this;
  }

  public void synchronize(PathFinder pathFinder, SyncProperties syncProps) {
    reset();
    getResourceManager().setPathFinder(pathFinder);
    syncLocalResources(pathFinder, syncProps);
    report(pathFinder, syncProps);
  }

  private void reset() {
    itemCount = 0;
    verifiedItems = 0;

    itemsCreated = 0;
    itemsUpdated = 0;
    itemsRemain = 0;
    itemsDeleted = 0;

    itemsNoAction = 0;

    failedCreations = 0;
    failedUpdates = 0;
    failedRemains = 0;
    failedDeletions = 0;

    downloadCount = 0;
  }

  private void syncLocalResources(PathFinder pathFinder, SyncProperties syncProps) {
    SitemapCollector collector = getSitemapCollector();
    collector.collectSitemaps(pathFinder, syncProps);
    if (collector.hasErrors()) {
      logger.warn("Not synchronizing because of previous {} errors: {}",
        collector.countErrors(), pathFinder.getCapabilityListUri());
    } else {
      for (Map.Entry<URI, UrlItem> entry : collector.getMostRecentItems().entrySet()) {
        syncItem(entry.getKey(), entry.getValue());
      }
    }
    int failures = failedCreations + failedUpdates + failedDeletions + failedRemains;
    logger.info("====> items={}, verified={}, failures={}, downloads={} [success/failures] " +
        "created={}/{}, updated={}/{}, remain={}/{}, deleted={}/{}, " +
        "no_action={}, trial run={}, resource set={}",
      itemCount, verifiedItems, failures, downloadCount, itemsCreated, failedCreations, itemsUpdated,
      failedUpdates, itemsRemain, failedRemains,
      itemsDeleted, failedDeletions, itemsNoAction, trialRun, pathFinder.getCapabilityListUri());
  }

  private void syncItem(URI normalizedURI, UrlItem item) {
    itemCount++;
    String change = item.getMetadata().flatMap(RsMd::getChange).orElse(CH_REMAIN);
    boolean resourceExists = resourceManager.exists(normalizedURI);

    logger.debug("------> {} {}, exists={}, normalizedURI={}", itemCount, change, resourceExists, normalizedURI);

    if (CH_REMAIN.equalsIgnoreCase(change)) {
      if (verifyChange(normalizedURI, item, resourceExists)) {
        itemsRemain++;
      } else {
        failedRemains++;
      }
    } else if (CH_CREATED.equalsIgnoreCase(change)) {
      if (verifyChange(normalizedURI, item, resourceExists)) {
        itemsCreated++;
      } else {
        failedCreations++;
      }
    } else if (CH_UPDATED.equalsIgnoreCase(change)) {
      if (verifyChange(normalizedURI, item, resourceExists)) {
        itemsUpdated++;
      } else {
        failedUpdates++;
      }
    } else if (CH_DELETED.equalsIgnoreCase(change) && resourceExists) {
      if (actionAllowed(normalizedURI) && resourceManager.delete(normalizedURI)) {
        itemsDeleted++;
      } else {
        failedDeletions++;
      }
    } else if (CH_DELETED.equalsIgnoreCase(change) && !resourceExists) {
      itemsNoAction++;
    }
  }

  private boolean verifyChange(URI normalizedURI, UrlItem item, boolean resourceExists) {
    boolean success;
    if (resourceExists) {
      boolean verified = doVerify(normalizedURI, item);
      if (verified) {
        success = actionAllowed(normalizedURI) && resourceManager.keep(normalizedURI);
      } else {
        success = actionAllowed(normalizedURI) && resourceManager.update(normalizedURI)
          && verifyAndUpdate(normalizedURI, item);
        if (success) downloadCount++;
      }
    } else { // resource does not exist
      success = actionAllowed(normalizedURI) && resourceManager.create(normalizedURI)
        && verifyAndUpdate(normalizedURI, item);
      if (success) downloadCount++;
    }
    return success;
  }

  private boolean verifyAndUpdate(URI normalizedURI, UrlItem item) {
    boolean verified = false;
    for (int i=0; i < getMaxDownloadRetry(); i++) {
      verified = doVerify(normalizedURI, item);
      if (verified) {
        break;
      } else {
        if (!actionAllowed(normalizedURI)) {
          break;
        } else {
          logger.info("Repeating download. download count={}, uri={}", i, normalizedURI);
          resourceManager.update(normalizedURI);
        }
      }
    }
    return verified;
  }

  private boolean actionAllowed(URI normalizedURI) {
    boolean allowed = true;
    if (trialRun) {
      logger.debug("Trial run. No action on: {}", normalizedURI);
      allowed = false;
    } else if (downloadCount >= maxDownloads) {
      logger.debug("Max downloads reached. No further action on: {}", normalizedURI);
      allowed = false;
    }
    return allowed;
  }

  private boolean doVerify(URI normalizedURI, UrlItem item) {
    VerificationPolicy policy = getVerificationPolicy();
    VerificationStatus stHash = VerificationStatus.not_verified;
    VerificationStatus stLastMod = VerificationStatus.not_verified;
    VerificationStatus stSize = VerificationStatus.not_verified;

    if (policy.continueVerification(stHash, stLastMod, stSize)) {
      Optional<String> maybeHash = item.getMetadata().flatMap(RsMd::getHash);
      if (maybeHash.isPresent()) {
        String hash = maybeHash.get();
        String algorithm = "md5";
        String[] splitHash = hash.split(":");
        if (splitHash.length > 1) {
          algorithm = splitHash[0];
          hash = splitHash[1];
        }
        stHash = resourceManager.verifyHash(normalizedURI, algorithm, hash);
      }
    }

    if (policy.continueVerification(stHash, stLastMod, stSize)) {
      Optional<ZonedDateTime> maybeLastModified = item.getLastmod();
      if (maybeLastModified.isPresent()) {
        stLastMod = resourceManager.verifyLastModified(normalizedURI, maybeLastModified.get());
      }
    }

    if (policy.continueVerification(stHash, stLastMod, stSize)) {
      Optional<Long> maybeSize = item.getMetadata().flatMap(RsMd::getLength);
      if (maybeSize.isPresent()) {
        stSize = resourceManager.verifySize(normalizedURI, maybeSize.get());
      }
    }
    boolean verified = policy.isVerified(stHash, stLastMod, stSize, true);
    if (verified) verifiedItems++;
    logger.debug("Verification status={}, Hash={}, LastMod={}, Size={}, uri={}",
      verified, stHash, stLastMod, stSize, normalizedURI);
    return verified;
  }

  private void report(PathFinder pathFinder, SyncProperties syncProps) {
    ZonedDateTime syncEnd = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC);

    syncProps.setDateTime(PROP_SYNC_START, pathFinder.getSyncStart());
    syncProps.setDateTime(PROP_SYNC_END, syncEnd);

    syncProps.setInt(PROP_MAX_DOWNLOADS, getMaxDownloads());
    syncProps.setInt(PROP_MAX_DOWNLOAD_RETRY, getMaxDownloadRetry());
    syncProps.setBool(PROP_TRIAL_RUN, isTrialRun());
    syncProps.setProperty(PROP_SITEMAP_COLLECTOR, getSitemapCollector().getClass().getName());
    syncProps.setProperty(PROP_RESOURCE_MANAGER, getResourceManager().getClass().getName());
    syncProps.setProperty(PROP_VERIFICATION_POLICY, getVerificationPolicy().getClass().getName());

    syncProps.setInt(PROP_TOTAL_ITEMS, itemCount);
    syncProps.setInt(PROP_ITEMS_VERIFIED, verifiedItems);
    syncProps.setInt(PROP_ITEMS_DELETED, itemsDeleted);
    syncProps.setInt(PROP_ITEMS_CREATED, itemsCreated);
    syncProps.setInt(PROP_ITEMS_UPDATED, itemsUpdated);
    syncProps.setInt(PROP_ITEMS_REMAIN, itemsRemain);
    syncProps.setInt(PROP_ITEMS_NO_ACTION, itemsNoAction);
    int failures = failedCreations + failedUpdates + failedDeletions + failedRemains;
    syncProps.setInt(PROP_TOTAL_FAILED_ITEMS, failures);
    syncProps.setInt(PROP_FAILED_DELETIONS, failedDeletions);
    syncProps.setInt(PROP_FAILED_CREATIONS, failedCreations);
    syncProps.setInt(PROP_FAILED_UPDATES, failedUpdates);
    syncProps.setInt(PROP_FAILED_REMAINS, failedRemains);

    syncProps.setInt(PROP_TOTAL_DOWNLOAD_COUNT, downloadCount);

    try {
      File file = pathFinder.getSyncPropXmlFile();
      String lsb = "Last saved by " + this.getClass().getName();
      syncProps.storeToXML(file, lsb);
      logger.debug("Saved SyncWorker properties to {}", file);
    } catch (IOException e) {
      logger.error("Could not save syncProps", e);
      throw new RuntimeException(e);
    }
  }

}
