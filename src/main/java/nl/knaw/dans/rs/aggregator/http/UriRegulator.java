package nl.knaw.dans.rs.aggregator.http;

import javax.annotation.Nonnull;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Created on 2017-04-18 10:19.
 */
public class UriRegulator {

//  public static URI normalize(@Nonnull URI uri) {
//    URI uri2 = uri.normalize();
//    uri2 = URI.create(uri2.toString().toLowerCase());
//    return uri2;
//  }

  public static String stripWWW(@Nonnull URI uri) {
    String host = uri.getHost();
    if (host == null) {
      return null;
    } else {
      return host.replaceAll("^www.", "");
    }
  }

  public static File normalizePath(@Nonnull URI uri) {
    String path = uri.getPath();
    if (path == null) {
      return new File("");
    } else {
      return new File(path.replaceAll("[//]", "/"));
    }
  }

  public static Optional<URI> regulate(String uriString) {
    if (uriString == null) {
      return  Optional.empty();
    }
    try {
      URI uri = new URI(uriString);
      return regulate(uri);
    } catch (URISyntaxException e) {
      return Optional.empty();
    }
  }

  public static Optional<URI> regulate(@Nonnull URI uri) {
    URI n = uri.normalize();
    String host = stripWWW(n);
    host = host == null ? null : host.toLowerCase();
    String path = normalizePath(n).getPath();
    try {
      if (n.isOpaque()) {
        return Optional.of(new URI(n.getScheme(), n.getSchemeSpecificPart(), n.getFragment()));
      } else {
        return Optional.of(new URI(n.getScheme(), null, host, n.getPort(), path, null, null));
      }
    } catch (URISyntaxException e) {
      // unlikely deviation
      throw new RuntimeException(e);
    }
  }
}
