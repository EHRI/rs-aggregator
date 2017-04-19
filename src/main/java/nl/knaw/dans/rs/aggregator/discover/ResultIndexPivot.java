package nl.knaw.dans.rs.aggregator.discover;

import nl.knaw.dans.rs.aggregator.http.Result;
import nl.knaw.dans.rs.aggregator.http.UriRegulator;
import nl.knaw.dans.rs.aggregator.xml.Capability;
import nl.knaw.dans.rs.aggregator.xml.RsMd;
import nl.knaw.dans.rs.aggregator.xml.RsRoot;
import nl.knaw.dans.rs.aggregator.xml.SitemapItem;
import nl.knaw.dans.rs.aggregator.xml.Sitemapindex;
import nl.knaw.dans.rs.aggregator.xml.UrlItem;
import nl.knaw.dans.rs.aggregator.xml.Urlset;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Data summarizations on a ResultIndex.
 */
public class ResultIndexPivot {

  private final ResultIndex resultIndex;

  public ResultIndexPivot(ResultIndex resultIndex) {
    this.resultIndex = resultIndex;
  }

  public ResultIndex getResultIndex() {
    return resultIndex;
  }

  public List<Throwable> listErrors() {
    return resultIndex.getResultMap().values().stream()
      .filter(result -> !result.getErrors().isEmpty())
      .flatMap(result -> result.getErrors().stream())
      .collect(Collectors.toList());
  }

  public List<Result<?>> listErrorResults() {
    return resultIndex.getResultMap().values().stream()
      .filter(result -> !result.getErrors().isEmpty())
      .collect(Collectors.toList());
  }

  public List<Result<?>> listResultsWithContent() {
    return resultIndex.getResultMap().values().stream()
      .filter(result -> result.getContent().isPresent())
      .collect(Collectors.toList());
  }

  public List<Result<?>> listFirstResults() {
    return resultIndex.getResultMap().values().stream()
      .filter(result -> result.getOrdinal() == 0)
      .collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  private Stream<Result<Sitemapindex>> streamSitemapIndexResults() {
    return resultIndex.getResultMap().values().stream()
      .filter(result -> result.getContent().orElse(null) instanceof Sitemapindex)
      .map(result -> (Result<Sitemapindex>) result);
  }

  @SuppressWarnings("unchecked")
  private Stream<Result<RsRoot>> streamRsRootResults() {
    return resultIndex.getResultMap().values().stream()
      .filter(result -> result.getContent().orElse(null) instanceof RsRoot)
      .map(result -> (Result<RsRoot>) result);
  }

  public List<Result<Sitemapindex>> listSitemapindexResults() {
    return streamSitemapIndexResults()
      .collect(Collectors.toList());
  }

  public List<Result<Sitemapindex>> listSitemapindexResults(Capability capability) {
    return streamSitemapIndexResults()
      .filter(result -> result.getContent()
        .map(RsRoot::getMetadata)
        .flatMap(RsMd::getCapability).orElse("invalid").equals(capability.xmlValue))
      .collect(Collectors.toList());
  }

  public List<Result<Sitemapindex>> listSitemapindexResultsByLevel(int capabilityLevel) {
    return streamSitemapIndexResults()
      .filter(result -> result.getContent()
        .map(RsRoot::getLevel).orElse(-1) == capabilityLevel)
      .collect(Collectors.toList());
  }

  public List<Result<RsRoot>> listRsRootResults() {
    return streamRsRootResults()
      .collect(Collectors.toList());
  }

  public List<Result<RsRoot>> listRsRootResults(Capability capability) {
    return streamRsRootResults()
      .filter(result -> result.getContent()
        .map(RsRoot::getMetadata)
        .flatMap(RsMd::getCapability).orElse("Invalid capability").equals(capability.xmlValue))
      .collect(Collectors.toList());
  }

  public List<Result<RsRoot>> listRsRootResultsByLevel(int capabilityLevel) {
    return streamRsRootResults()
      .filter(result -> result.getContent()
        .map(RsRoot::getLevel).orElse(-1) == capabilityLevel)
      .collect(Collectors.toList());
  }

  @SuppressWarnings ("unchecked")
  public List<Result<LinkList>> listLinkListResults() {
    return resultIndex.getResultMap().values().stream()
      .filter(result -> result.getContent().isPresent() && result.getContent().orElse(null) instanceof LinkList)
      .map(result -> (Result<LinkList>) result)
      .collect(Collectors.toList());
  }

  /**
   * List the values of the &lt;loc&gt; element of &lt;sitemap&gt; elements of documents of type sitemapindex with
   * the given capability.
   * @param capability the capability of the documents from which locations will be extracted
   * @return List of values of the &lt;loc&gt; elements
   */
  @SuppressWarnings ("unchecked")
  public List<String> listSitemapLocations(Capability capability) {
    return resultIndex.getResultMap().values().stream()
      .filter(result -> result.getContent().isPresent() && result.getContent().orElse(null) instanceof Sitemapindex)
      .map(result -> (Result<Sitemapindex>) result)
      .filter(result -> result.getContent().map(RsRoot::getMetadata)
        .flatMap(RsMd::getCapability).orElse("invalid").equals(capability.xmlValue))
      .map(sitemapindexResult -> sitemapindexResult.getContent().orElse(null))
      .map(Sitemapindex::getItemList)
      .flatMap(List::stream)
      .map(SitemapItem::getLoc)
      .collect(Collectors.toList());
  }
}
