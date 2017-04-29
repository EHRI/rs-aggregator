package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.util.NormURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created on 2017-04-13 09:34.
 */
public class PathFinder {

  public static final String DIR_METADATA = "__MOR__";
  public static final String DIR_RESOURCES = "__SOR__";

  private static Logger logger = LoggerFactory.getLogger(PathFinder.class);

  private final ZonedDateTime operationDateTime;

  private final String host;
  private final int port;
  private final String path;

  private final URI capabilityListUri;

  private final File baseDirectory;
  private final File setDirectory;
  private final File metadataDirectory;
  private final File resourceDirectory;
  private final File capabilityListFile;

  public PathFinder(@Nonnull String baseDirectory, @Nonnull URI capabilityListUri) {
    this.capabilityListUri = capabilityListUri;
    operationDateTime = ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC);
    File baseDir = new File(baseDirectory);
    this.baseDirectory = baseDir.getAbsoluteFile();

    URI uri2 = NormURI.normalize(capabilityListUri).orElse(null);
    host = uri2.getHost();
    port = uri2.getPort();
    File filePath = new File(uri2.getPath());
    path = filePath.getParent();
    String fileName = filePath.getName();

    StringBuilder sb = new StringBuilder(this.baseDirectory.getAbsolutePath())
      .append(File.separator)
      .append(host);
    if (port > -1) {
      sb.append(File.separator).append(port);
    }
    sb.append(path);
    setDirectory = new File(sb.toString());
    metadataDirectory = new File(setDirectory, DIR_METADATA);
    resourceDirectory = new File(setDirectory, DIR_RESOURCES);
    capabilityListFile = new File(metadataDirectory, fileName);

    logger.info("Created path finder with operationDateTime {}", operationDateTime);
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getPath() {
    return path;
  }

  public URI getCapabilityListUri() {
    return capabilityListUri;
  }

  public File getBaseDirectory() {
    return baseDirectory;
  }

  public File getSetDirectory() {
    return setDirectory;
  }

  public File getMetadataDirectory() {
    return metadataDirectory;
  }

  public File getResourceDirectory() {
    return resourceDirectory;
  }

  public File getCapabilityListFile() {
    return capabilityListFile;
  }

  public File findMetadataFilePath(@Nonnull URI uri) {
    String restPath = extractPath(uri).replace(path, "");
    return new File(metadataDirectory, restPath);
  }

  public File findResourceFilePath(URI uri) {
    return new File(resourceDirectory, extractPath(uri));
  }

  private String extractPath(@Nonnull URI uri) {
    URI otherUri = NormURI.normalize(uri).orElse(null);
    String otherHost = otherUri.getHost();
    int otherPort = otherUri.getPort();
    if (!host.equals(otherHost)) {
      throw new IllegalArgumentException("Normalized host names unequal. this host:" + host + " other: " + otherHost);
    } else if (port != otherPort) {
      throw new IllegalArgumentException("Ports unequal. this port:" + port + " other: " + otherPort);
    }
    return otherUri.getPath();
  }

  public Set<File> findResourceFilePaths(Collection<URI> uris) {
    return uris.parallelStream()
      .map(this::findResourceFilePath).collect(Collectors.toSet());
  }


}
