package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.util.LambdaUtil;
import nl.knaw.dans.rs.aggregator.xml.ResourceSyncContext;
import nl.knaw.dans.rs.aggregator.xml.RsRoot;
import org.apache.http.HttpResponse;

import java.net.URI;

/**
 * Created on 2017-05-08 10:10.
 */
public interface SitemapConverterProvider {

  LambdaUtil.BiFunction_WithExceptions<URI, HttpResponse, RsRoot, Exception> getConverter();

  void setPathFinder(PathFinder pathFinder);

  SitemapConverterProvider withResourceSyncContext(ResourceSyncContext rsContext);
}
