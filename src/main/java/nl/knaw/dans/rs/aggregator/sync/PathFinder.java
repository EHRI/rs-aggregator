package nl.knaw.dans.rs.aggregator.sync;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created on 2017-04-13 09:34.
 */
public class PathFinder {

  public static final String DIR_METADATA = "metadata";
  public static final String DIR_RESOURCES = "resources";

  private final String host;
  private final int port;
  private final String path;

  private final File baseDirectory;
  private final File setDirectory;
  private final File metadataDirectory;
  private final File resourceDirectory;
  private final File capabilityListFile;

  public PathFinder(String baseDirectory, URI capabilityListUri) {
    File baseDir = new File(baseDirectory);
    this.baseDirectory = baseDir.getAbsoluteFile();

    URI uri2 = normalize(capabilityListUri);
    host = stripWWW(uri2);
    port = uri2.getPort();
    path = normalizePath(uri2).getParent();
    String fileName = normalizePath(uri2).getName();

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

  public File getMetadataFilePath(URI uri) {
    String restPath = extractPath(uri).replace(path, "");
    return new File(metadataDirectory, restPath);
  }

  public File getResourceFilePath(URI uri) {
    return new File(resourceDirectory, extractPath(uri));
  }

  private String extractPath(URI uri) {
    URI otherUri = normalize(uri);
    String otherHost = stripWWW(otherUri);
    int otherPort = otherUri.getPort();
    if (!host.equals(otherHost)) {
      throw new IllegalArgumentException("Normalized host names unequal. this host:" + host + " other: " + otherHost);
    } else if (port != otherPort) {
      throw new IllegalArgumentException("Ports unequal. this port:" + port + " other: " + otherPort);
    }
    return normalizePath(otherUri).getPath();
  }

  public static URI normalize(URI uri) {
    URI uri2 = uri.normalize();
    uri2 = URI.create(uri2.toString().toLowerCase());
    return uri2;
  }

  public static String stripWWW(URI uri) {
    return uri.getHost().replaceAll("^www.", "");
  }

  public static File normalizePath(URI uri) {
    return new File(uri.getPath().replaceAll("[//]", "/"));
  }

}
