package nl.knaw.dans.rs.aggregator.discover;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * List of links, keeping track of valid and invalid URI's.
 */
public class LinkList {

  private Set<URI> validUris = new TreeSet<>();
  private Set<String> invalidUris = new TreeSet<>();

  public void add(URI uri) {
    validUris.add(uri);
  }

  public void add(String uriString) {
    try {
      add(new URI(uriString));
    } catch (URISyntaxException e) {
      invalidUris.add(uriString);
    }
  }

  public void resolve(URI baseUri, String uriString) {
    try {
      add(baseUri.resolve(uriString));
    } catch (IllegalArgumentException e) {
      invalidUris.add(uriString);
    }
  }

  public void resolve(URI baseUri, Collection<String> uriStrings) {
    for (String uriString : uriStrings) {
      resolve(baseUri, uriString);
    }
  }

  public Set<URI> getValidUris() {
    return validUris;
  }

  public Set<String> getInvalidUris() {
    return invalidUris;
  }
}
