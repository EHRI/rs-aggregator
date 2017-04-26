package nl.knaw.dans.rs.aggregator.discover;

import nl.knaw.dans.rs.aggregator.http.Result;
import nl.knaw.dans.rs.aggregator.http.NormURI;
import nl.knaw.dans.rs.aggregator.xml.Capability;
import nl.knaw.dans.rs.aggregator.xml.RsItem;
import nl.knaw.dans.rs.aggregator.xml.RsMd;
import nl.knaw.dans.rs.aggregator.xml.RsRoot;
import nl.knaw.dans.rs.aggregator.xml.UrlItem;
import nl.knaw.dans.rs.aggregator.xml.Urlset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created on 2017-04-18 14:31.
 */
public class UrlSetPivot extends ResultIndexPivot {

  private static Logger logger = LoggerFactory.getLogger(UrlSetPivot.class);

  private static int invalidUriCount = 0;

  private static URI getInvalidUri() {
    return URI.create("invalid" + invalidUriCount++);
  }

  public UrlSetPivot(ResultIndex resultIndex) {
    super(resultIndex);
  }

  public List<Result<Urlset>> listUrlsetResults() {
    return streamUrlsetResults()
      .collect(Collectors.toList());
  }

  public List<Result<Urlset>> listUrlsetResults(Capability capability) {
    return streamUrlsetResults(capability)
      .collect(Collectors.toList());
  }

  public List<Result<Urlset>> listUrlsetResultsByLevel(int capabilityLevel) {
    return streamUrlsetResults()
      .filter(result -> result.getContent()
        .map(RsRoot::getLevel).orElse(-1) == capabilityLevel)
      .collect(Collectors.toList());
  }

  public List<Result<Urlset>> listSortedUrlsetResults(Capability capability) {
    return streamUrlsetResults(capability)
      .sorted()
      .collect(Collectors.toList());
  }

  public List<UrlItem> listUrlItems(Capability capability) {
    return streamUrlItems(capability)
      .collect(Collectors.toList());
  }

  /**
   * List the values of the &lt;loc&gt; element of &lt;url&gt; elements of documents of type urlset with
   * the given capability.
   * @param capability the capability of the documents from which locations will be extracted
   * @return List of values of the &lt;loc&gt; elements
   */
  public List<String> listUrlLocations(Capability capability) {
    return streamUrlItems(capability)
      .map(UrlItem::getLoc)
      .collect(Collectors.toList());
  }

  public Map<String, UrlItem> mapUrlItemsByLoc(Capability capability) {
    return streamUrlItems(capability)
      .collect(Collectors.toConcurrentMap(RsItem::getLoc, urlItem -> urlItem,
        ((loc1, loc2) -> { return loc1; })));
  }

  public Map<URI, UrlItem> mapUrlItemsByUri(Capability capability) {
    return streamUrlItems(capability)
      .collect(Collectors.toConcurrentMap(urlItem -> NormURI.normalize(urlItem.getLoc())
          .orElseGet(UrlSetPivot::getInvalidUri), urlItem -> urlItem,
        ((urlItem, urlItem2) -> {
        logger.warn("Duplicate URI: {} and {}", urlItem.getLoc(), urlItem2.getLoc());
        return urlItem; })));
  }

  @SuppressWarnings("unchecked")
  private Stream<Result<Urlset>> streamUrlsetResults() {
    return getResultIndex().getResultMap().values().stream()
      .filter(result -> result.getContent().orElse(null) instanceof Urlset)
      .map(result -> (Result<Urlset>) result);
  }

  private Stream<Result<Urlset>> streamUrlsetResults(Capability capability) {
    return streamUrlsetResults()
      .filter(result -> result.getContent()
        .map(RsRoot::getMetadata)
        .flatMap(RsMd::getCapability).orElse("invalid").equals(capability.xmlValue));
  }

  private Stream<Result<Urlset>> streamUrlsetResults(Capability capability, ZonedDateTime zonedDateTime) {
    return streamUrlsetResults(capability)
      .filter(result -> result.getContent()
        .map(RsRoot::getMetadata)
        .flatMap(RsMd::getAt).orElse(null).isAfter(zonedDateTime) && "".equals("."));
  }

  private Stream<UrlItem> streamUrlItems(Capability capability) {
    return streamUrlsetResults(capability)
      .map(urlsetResult -> urlsetResult.getContent().orElse(null))
      .map(Urlset::getItemList)
      .flatMap(List::stream);
  }
}
