package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.syncore.SitemapConverterProvider;
import nl.knaw.dans.rs.aggregator.util.LambdaUtil;
import nl.knaw.dans.rs.aggregator.syncore.PathFinder;
import nl.knaw.dans.rs.aggregator.xml.ResourceSyncContext;
import nl.knaw.dans.rs.aggregator.xml.RsBuilder;
import nl.knaw.dans.rs.aggregator.xml.RsRoot;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Date;

/**
 * A {@link SitemapConverterProvider} that provides a converter that both stores the response on disk and
 * returns the response unmarshalled as {@link RsRoot}. The file location of the sitemap is decided by
 * calling {@link PathFinder#findMetadataFilePath(URI)} for the given URI on the current PathFinder.
 */
public class FsSitemapConverterProvider implements SitemapConverterProvider {

  private static Logger logger = LoggerFactory.getLogger(FsSitemapConverterProvider.class);

  private ResourceSyncContext rsContext;
  private PathFinder currentPathFinder;

  @Override
  public LambdaUtil.BiFunction_WithExceptions<URI, HttpResponse, RsRoot, Exception> getConverter() {
    return fileSavingConverter;
  }

  private LambdaUtil.BiFunction_WithExceptions<URI, HttpResponse, RsRoot, Exception>
      fileSavingConverter = (uri, response) -> {

      HttpEntity entity = response.getEntity();
      RsRoot rsRoot = null;
      if (entity != null) {
        File file = getCurrentPathFinder().findMetadataFilePath(uri);
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

  @Override
  public FsSitemapConverterProvider withResourceSyncContext(ResourceSyncContext rsContext) {
    this.rsContext = rsContext;
    return this;
  }

  @Override
  public void setPathFinder(PathFinder pathFinder) {
    currentPathFinder = pathFinder;
  }

  private PathFinder getCurrentPathFinder() {
    if (currentPathFinder == null) {
      throw new IllegalStateException("No PathFinder set.");
    }
    return currentPathFinder;
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
}
