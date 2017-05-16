package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.http.Result;
import nl.knaw.dans.rs.aggregator.util.FileCleaner;
import nl.knaw.dans.rs.aggregator.util.HashUtil;
import nl.knaw.dans.rs.aggregator.util.ZonedDateTimeUtil;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Set;

/**
 * Created on 2017-04-28 12:56.
 */
public class FsResourceManager implements ResourceManager {

  private static Logger logger = LoggerFactory.getLogger(FsResourceManager.class);

  private PathFinder pathFinder;
  private ResourceReader resourceReader;
  private CloseableHttpClient httpClient;

  private PathFinder getPathFinder() {
    if (pathFinder == null) throw new IllegalStateException("Missing PathFinder. No PathFinder was set.");
    return pathFinder;
  }

  public void setPathFinder(PathFinder pathFinder) {
    this.pathFinder = pathFinder;
  }

  private CloseableHttpClient getHttpClient() {
    if (httpClient == null) {
      httpClient = HttpClients.createDefault();
    }
    return httpClient;
  }

  public FsResourceManager withHttpClient(CloseableHttpClient httpClient) {
    this.httpClient = httpClient;
    return this;
  }

  private ResourceReader getResourceReader() {
    if (resourceReader == null) {
      resourceReader = new ResourceReader(getHttpClient());
    }
    return resourceReader;
  }

  public FsResourceManager withResourceReader(ResourceReader resourceReader) {
    this.resourceReader = resourceReader;
    return this;
  }

  @Override
  public boolean exists(@Nonnull URI normalizedURI) {
    return getPathFinder().findResourceFilePath(normalizedURI).exists();
  }

  @Override
  public VerificationStatus verifyHash(@Nonnull URI normalizedURI, @Nonnull String algorithm, @Nonnull String hash) {
    VerificationStatus status = VerificationStatus.not_verified;
    try {
      String localHash = HashUtil.computeHash(algorithm,
        new FileInputStream(getPathFinder().findResourceFilePath(normalizedURI)));
      if (hash.equalsIgnoreCase(localHash)) {
        status = VerificationStatus.verification_success;
        //logger.debug("Verified {} hash of {}.", algorithm, normalizedURI);
      } else {
        status = VerificationStatus.verification_failure;
        logger.info("Hash failure. algorithm={}, remote={}, local={}, uri={}",
          algorithm, hash, localHash, normalizedURI);
      }
    } catch (NoSuchAlgorithmException e) {
      status = VerificationStatus.verification_error;
      logger.error("Unknown hash algorithm: '{}': {}", algorithm, normalizedURI, e);
    } catch (IOException e) {
      status = VerificationStatus.verification_error;
      logger.error("Exception while computing hash: {}", normalizedURI, e);
    }
    return status;
  }

  @Override
  public VerificationStatus verifyLastModified(@Nonnull URI normalizedURI, @Nonnull ZonedDateTime lastModified) {
    VerificationStatus status = VerificationStatus.not_verified;
    long localLm = getPathFinder().findResourceFilePath(normalizedURI).lastModified();
    long remoteLm = ZonedDateTimeUtil.toLong(lastModified);
    if (remoteLm == localLm) {
      status = VerificationStatus.verification_success;
      //logger.debug("Verified Last-modified of {}", normalizedURI);
    } else {
      status = VerificationStatus.verification_failure;
      logger.info("Last-modified not equal. remote={} ({}), local={} ({}), uri={}",
        remoteLm, new Date(remoteLm),
        localLm, new Date(localLm), normalizedURI);
    }
    return status;
  }

  @Override
  public VerificationStatus verifySize(@Nonnull URI normalizedURI, long size) {
    VerificationStatus status = VerificationStatus.not_verified;
    long localSize = getPathFinder().findResourceFilePath(normalizedURI).length();
    if (size == localSize) {
      status = VerificationStatus.verification_success;
      //logger.debug("Verified Length of {}", normalizedURI);
    } else {
      status = VerificationStatus.verification_failure;
      logger.info("Length not equal. remote={}, local={}, uri={}", size, localSize, normalizedURI);
    }
    return status;
  }

  @Override
  public boolean keepOnly(@Nonnull Set<URI> normalizedURIs) {
    Set<File> fileSet = getPathFinder().findResourceFilePaths(normalizedURIs);
    FileCleaner fileCleaner = new FileCleaner(fileSet);
    File resourceDirectory = getPathFinder().getResourceDirectory();
    if (!resourceDirectory.exists()) return true;
    try {
      Files.walkFileTree(getPathFinder().getResourceDirectory().toPath(), fileCleaner);
    } catch (IOException e) {
      throw new RuntimeException("Could not clear resource directory for " + getPathFinder().getCapabilityListUri(), e);
    }
    return true;
  }

  @Override
  public boolean keep(@Nonnull URI normalizedURI) {
    File resourcePath = getPathFinder().findResourceFilePath(normalizedURI);
    logger.debug("Remain {} --> {}", normalizedURI, resourcePath);
    return true;
  }

  @Override
  public boolean create(@Nonnull URI normalizedURI) {
    return download(normalizedURI);
  }

  @Override
  public boolean update(@Nonnull URI normalizedURI) {
    return download(normalizedURI);
  }

  @Override
  public boolean delete(@Nonnull URI normalizedURI) {
    File resourcePath = getPathFinder().findResourceFilePath(normalizedURI);
    boolean deleted = resourcePath.delete();
    if (deleted) logger.debug("Deleted {} --> {}", normalizedURI, resourcePath);
    return true;
  }

  private boolean download(@Nonnull URI normalizedURI) {
    boolean downloaded = false;
    File resourcePath = getPathFinder().findResourceFilePath(normalizedURI);
    Result<File> result = getResourceReader().read(normalizedURI, resourcePath);
    if (result.getContent().isPresent()) {
      downloaded = true;
      logger.debug("Downloaded {} --> {}", normalizedURI, resourcePath);
    } else {
      logger.warn("Failed download of {}: ", normalizedURI, result.lastError());
    }
    return downloaded;
  }
}
