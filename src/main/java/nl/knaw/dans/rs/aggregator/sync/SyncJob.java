package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.schedule.Job;
import nl.knaw.dans.rs.aggregator.syncore.ResourceManager;
import nl.knaw.dans.rs.aggregator.syncore.SitemapConverterProvider;
import nl.knaw.dans.rs.aggregator.syncore.Sync;
import nl.knaw.dans.rs.aggregator.syncore.SyncPostProcessor;
import nl.knaw.dans.rs.aggregator.syncore.VerificationPolicy;
import nl.knaw.dans.rs.aggregator.util.NormURI;
import nl.knaw.dans.rs.aggregator.syncore.PathFinder;
import nl.knaw.dans.rs.aggregator.util.RsProperties;
import nl.knaw.dans.rs.aggregator.xml.ResourceSyncContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Created on 2017-05-03 17:05.
 */
public class SyncJob implements Job {

  private static Logger logger = LoggerFactory.getLogger(SyncJob.class);

  private CloseableHttpClient httpClient;
  private ResourceSyncContext rsContext;

  private SitemapConverterProvider sitemapConverterProvider;
  private VerificationPolicy verificationPolicy;
  private ResourceManager resourceManager;

  private SitemapCollector sitemapCollector;
  private SyncPostProcessor syncPostProcessor;

  private String uriListLocation;
  private String baseDirectory;

  public SitemapConverterProvider getSitemapConverterProvider() {
    if (sitemapConverterProvider == null) {
      sitemapConverterProvider = new FsSitemapConverterProvider();
    }
    return sitemapConverterProvider;
  }

  public void setSitemapConverterProvider(SitemapConverterProvider sitemapConverterProvider) {
    this.sitemapConverterProvider = sitemapConverterProvider;
  }

  public VerificationPolicy getVerificationPolicy() {
    if (verificationPolicy == null) {
      verificationPolicy = new DefaultVerificationPolicy();
    }
    return verificationPolicy;
  }

  public void setVerificationPolicy(VerificationPolicy verificationPolicy) {
    logger.debug("Verification policy: {}", verificationPolicy);
    this.verificationPolicy = verificationPolicy;
  }

  public ResourceManager getResourceManager() {
    if (resourceManager == null) {
      resourceManager = new FsResourceManager();
    }
    return resourceManager;
  }

  public void setResourceManager(ResourceManager resourceManager) {
    logger.debug("Resource manager: {}", resourceManager);
    this.resourceManager = resourceManager;
  }

  public SyncPostProcessor getSyncPostProcessor() {
    if (syncPostProcessor == null) {
      syncPostProcessor = new DefaultSyncPostProcessor();
    }
    return syncPostProcessor;
  }

  public void setSyncPostProcessor(SyncPostProcessor syncPostProcessor) {
    this.syncPostProcessor = syncPostProcessor;
  }

  public CloseableHttpClient getHttpClient() {
    if (httpClient == null) {
      httpClient = HttpClients.createDefault();
    }
    return httpClient;
  }

  public void setHttpClient(CloseableHttpClient httpClient) {
    this.httpClient = httpClient;
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

  public void setRsContext(ResourceSyncContext rsContext) {
    this.rsContext = rsContext;
  }

  public SitemapCollector getSitemapCollector() {
    if (sitemapCollector == null) {
      sitemapCollector = new SitemapCollector();
    }
    return sitemapCollector;
  }

  public void setSitemapCollector(SitemapCollector sitemapCollector) {
    this.sitemapCollector = sitemapCollector;
  }

  public String getUriListLocation() {
    if (uriListLocation == null) {
      throw new IllegalStateException("No urilist set");
    }
    return uriListLocation;
  }

  public void setUriListLocation(String uriListLocation) {
    this.uriListLocation = uriListLocation;
  }

  public String getBaseDirectory() {
    if (baseDirectory == null) {
      baseDirectory = "base-directory";
    }
    return baseDirectory;
  }

  public void setBaseDirectory(String baseDirectory) {
    this.baseDirectory = baseDirectory;
  }

  public void readListAndSynchronize() throws Exception {
    List<URI> uriList = new ArrayList<>();
    Scanner scanner = new Scanner(new File(getUriListLocation()));
    while (scanner.hasNextLine()) {
      String uriString = scanner.nextLine();
      Optional<URI> maybeUri = NormURI.normalize(uriString);
      if (maybeUri.isPresent()) {
        uriList.add(maybeUri.get());
      } else {
        logger.warn("Unable to convert {} to a URI", uriString);
      }
    }
    synchronize(uriList);
  }

  public void synchronize(List<URI> uriList) throws Exception {
    SitemapConverterProvider sitemapConverterProvider = getSitemapConverterProvider()
      .withResourceSyncContext(getRsContext());
    SitemapCollector sitemapCollector = getSitemapCollector()
      .withHttpClient(getHttpClient())
      .withRsContext(getRsContext())
      .withConverter(sitemapConverterProvider.getConverter());
    SyncWorker syncWorker = new SyncWorker()
      .withSitemapCollector(sitemapCollector)
      .withVerificationPolicy(getVerificationPolicy())
      .withResourceManager(getResourceManager());
    SyncPostProcessor syncPostProcessor = getSyncPostProcessor();

    for (URI uri : uriList) {
      PathFinder pathFinder = new PathFinder(getBaseDirectory(), uri);
      RsProperties currentSyncProps = new RsProperties();
      setLatestSyncRun(pathFinder, sitemapCollector);

      sitemapConverterProvider.setPathFinder(pathFinder);
      syncWorker.synchronize(pathFinder, currentSyncProps);
      syncPostProcessor.postProcess(sitemapCollector.getCurrentIndex(), pathFinder, currentSyncProps);
    }
  }

  private void setLatestSyncRun(PathFinder pathFinder, SitemapCollector sitemapCollector) {
    File prevSyncPropFile = pathFinder.getPrevSyncPropXmlFile();
    ZonedDateTime latestSyncRun = null;
    if (prevSyncPropFile != null) {
      RsProperties prevSyncProps = new RsProperties();
      try {
        prevSyncProps.loadFromXML(prevSyncPropFile);
        latestSyncRun = prevSyncProps.getDateTime(Sync.PROP_SW_SYNC_START);
      } catch (IOException e) {
        throw new RuntimeException("Could not load syncProps from " + prevSyncPropFile, e);
      }
    }
    sitemapCollector.withAsOfDateTime(latestSyncRun);
    logger.info("only looking at item-events after {}", sitemapCollector.getAsOfDateTime());
  }

  @Override
  public void execute() throws Exception {
    readListAndSynchronize();
  }
}
