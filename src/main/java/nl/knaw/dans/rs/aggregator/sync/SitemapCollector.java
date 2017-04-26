package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.discover.ResultIndex;
import nl.knaw.dans.rs.aggregator.discover.RsExplorer;
import nl.knaw.dans.rs.aggregator.util.LambdaUtil;
import nl.knaw.dans.rs.aggregator.xml.Capability;
import nl.knaw.dans.rs.aggregator.xml.ResourceSyncContext;
import nl.knaw.dans.rs.aggregator.xml.RsBuilder;
import nl.knaw.dans.rs.aggregator.xml.RsRoot;
import nl.knaw.dans.rs.aggregator.xml.UrlItem;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

/**
 * Created on 2017-04-26 09:13.
 */
public class SitemapCollector {

  private static Logger logger = LoggerFactory.getLogger(SitemapCollector.class);

  private final PathFinder pathFinder;

  private CloseableHttpClient httpClient;
  private ResourceSyncContext rsContext;

  private Map<URI, UrlItem> resourceItems;
  private Map<URI, UrlItem> createdItems;
  private Map<URI, UrlItem> updatedItems;
  private Map<URI, UrlItem> deletedItems;
  private ZonedDateTime ultimateResourceListAt;
  private ZonedDateTime ultimateChangeListFrom;


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

  public ResultIndex collectSitemaps() {
    RsExplorer explorer = new RsExplorer(getHttpClient(), getRsContext())
      .withConverter(fileSavingConverter)
      .withFollowChildLinks(true)
      .withFollowIndexLinks(false)
      .withFollowParentLinks(false);
    ResultIndex index = explorer.explore(pathFinder.getCapabilityListUri());
    return index;
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
