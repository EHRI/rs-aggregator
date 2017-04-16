package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.http.Result;
import nl.knaw.dans.rs.aggregator.xml.ResourceSyncContext;
import nl.knaw.dans.rs.aggregator.xml.RsRoot;
import nl.knaw.dans.rs.aggregator.xml.UrlItem;
import nl.knaw.dans.rs.aggregator.xml.Urlset;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;

/**
 * Created on 2017-04-15 14:36.
 */
public class Synchronizer {

  private final static Logger logger = LoggerFactory.getLogger(Synchronizer.class);

  private final PathFinder pathFinder;

  private RsDocumentReader rsDocReader;
  private CloseableHttpClient httpClient;
  private ResourceSyncContext rsContext;

  public Synchronizer(PathFinder pathFinder) {
    this.pathFinder = pathFinder;
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

  public void synchronize() {
    Result<RsRoot> result = rsDocReader.read(pathFinder.getCapabilityListUri());
    if (rsDocReader.getLatestUrlset().isPresent()) {
      syncCapabilities(rsDocReader.getLatestUrlset().get());
    } else {
      logger.error("Invalid result: {}", result.toString());
    }
  }

  private void syncCapabilities(Urlset urlset) {
    for (UrlItem item : urlset.getItemList()) {
      item.getLoc();
    }
  }
}
