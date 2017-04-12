package nl.knaw.dans.rs.aggregator.discover;


import nl.knaw.dans.rs.aggregator.http.AbstractUriReader;
import nl.knaw.dans.rs.aggregator.http.Result;
import nl.knaw.dans.rs.aggregator.util.LambdaUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;

import java.net.URI;
import java.nio.charset.Charset;

/**
 * Explore URI's and store {@link Result}s in a {@link ResultIndex}.
 */
public abstract class AbstractUriExplorer extends AbstractUriReader {

  public AbstractUriExplorer(CloseableHttpClient httpClient) {
    super(httpClient);
  }

  public abstract Result<?> explore(URI uri, ResultIndex index);

}
