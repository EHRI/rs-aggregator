package nl.knaw.dans.rs.aggregator.sync;


import nl.knaw.dans.rs.aggregator.http.AbstractUriReader;
import nl.knaw.dans.rs.aggregator.http.Result;
import nl.knaw.dans.rs.aggregator.util.LambdaUtil;
import nl.knaw.dans.rs.aggregator.xml.ResourceSyncContext;
import nl.knaw.dans.rs.aggregator.xml.RsBuilder;
import nl.knaw.dans.rs.aggregator.xml.RsRoot;
import nl.knaw.dans.rs.aggregator.xml.Sitemapindex;
import nl.knaw.dans.rs.aggregator.xml.Urlset;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public class RsDocumentReader extends AbstractUriReader {

  private final ResourceSyncContext rsContext;
  private final RsBuilder rsBuilder;


  public RsDocumentReader(CloseableHttpClient httpClient, ResourceSyncContext rsContext) {
    super(httpClient);
    this.rsContext = rsContext;
    rsBuilder = new RsBuilder(rsContext);
  }

  public Result<RsRoot> read(String url) throws URISyntaxException {
    URI uri = new URI(url);
    return read(uri);
  }

  public Result<RsRoot> read(URI uri) {
    return execute(uri, rsConverter);
  }

  public Optional<Urlset> getLatestUrlset() {
    return rsBuilder.getUrlset();
  }

  public Optional<Sitemapindex> getLatestSitemapindex() {
    return rsBuilder.getSitemapindex();
  }

  private ResourceSyncContext getRsContext() {
    return rsContext;
  }

  private RsBuilder getRsBuilder() {
    return rsBuilder;
  }

  private LambdaUtil.Function_WithExceptions<HttpResponse, RsRoot, Exception> rsConverter = (response) -> {
    InputStream inStream = null;
    if (response.getEntity() != null) {
      inStream = response.getEntity().getContent();
    }
    return getRsBuilder().setInputStream(inStream).build().orElse(null);
  };

}
