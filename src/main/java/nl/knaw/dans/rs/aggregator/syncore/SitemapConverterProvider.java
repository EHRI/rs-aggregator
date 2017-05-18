package nl.knaw.dans.rs.aggregator.syncore;

import nl.knaw.dans.rs.aggregator.util.LambdaUtil;
import nl.knaw.dans.rs.aggregator.xml.ResourceSyncContext;
import nl.knaw.dans.rs.aggregator.xml.RsRoot;
import org.apache.http.HttpResponse;

import java.net.URI;

/**
 * Implementations of this interface can provide a {@link java.util.function.BiFunction} converter that knows
 * how to handle the response on a sitemap URI.
 */
public interface SitemapConverterProvider {

  /**
   * Provide the converter that returns an {@link RsRoot} when given a URI and a HttpResponse.
   * The converter may throw an Exception.
   *
   * @return converter
   */
  LambdaUtil.BiFunction_WithExceptions<URI, HttpResponse, RsRoot, Exception> getConverter();

  /**
   * Set the {@link PathFinder} that corresponds to the URI the provided converter is about to handle.
   *
   * @param pathFinder PathFinder that corresponds to the URI the provided converter is about to handle
   */
  void setPathFinder(PathFinder pathFinder);

  /**
   * Set the {@link ResourceSyncContext} for the provided converter.
   *
   * @param rsContext ResourceSyncContext} for the provided converter
   * @return this for enabling method chaining
   */
  SitemapConverterProvider withResourceSyncContext(ResourceSyncContext rsContext);
}
