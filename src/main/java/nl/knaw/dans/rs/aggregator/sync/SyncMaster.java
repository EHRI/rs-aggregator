package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.util.NormURI;
import nl.knaw.dans.rs.aggregator.xml.ResourceSyncContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Created on 2017-05-03 17:05.
 */
public class SyncMaster {

  private static Logger logger = LoggerFactory.getLogger(SyncMaster.class);

  private CloseableHttpClient httpClient;
  private ResourceSyncContext rsContext;

  private SitemapConverterProvider sitemapConverterProvider;
  private VerificationPolicy verificationPolicy;
  private ResourceManager resourceManager;

  private SitemapCollector sitemapCollector;
  private SyncWorker syncWorker;

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

    for (URI uri : uriList) {
      PathFinder pathFinder = new PathFinder(getBaseDirectory(), uri);
      SyncProperties syncProps = new SyncProperties();
      sitemapConverterProvider.setPathFinder(pathFinder);
      syncWorker.synchronize(pathFinder, syncProps);
    }

  }
}
