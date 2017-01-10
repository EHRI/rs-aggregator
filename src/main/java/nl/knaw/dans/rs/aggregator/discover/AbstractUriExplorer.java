package nl.knaw.dans.rs.aggregator.discover;


import nl.knaw.dans.rs.aggregator.util.LambdaUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;

import java.net.URI;
import java.nio.charset.Charset;

/**
 * Execute a request against a server.
 */
public abstract class AbstractUriExplorer {

  static String getCharset(HttpResponse response) {
    ContentType contentType = ContentType.getOrDefault(response.getEntity());
    Charset charset = contentType.getCharset();
    return charset == null ? "UTF-8" : charset.name();
  }

  private final CloseableHttpClient httpClient;

  private URI currentUri;

  public AbstractUriExplorer(CloseableHttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public abstract Result<?> explore(URI uri, ResultIndex index);

  protected CloseableHttpClient getHttpClient() {
    return httpClient;
  }

  protected URI getCurrentUri() {
    return currentUri;
  }

  public <T> Result<T> execute(URI uri, LambdaUtil.Function_WithExceptions<HttpResponse, T, ?> func) {
    currentUri = uri;
    Result<T> result = new Result<T>(uri);
    HttpGet request = new HttpGet(uri);

    try (CloseableHttpResponse response = httpClient.execute(request)) {
      int statusCode = response.getStatusLine().getStatusCode();
      result.setStatusCode(statusCode);
      if (statusCode < 200 || statusCode > 299) {
        result.addError(new RemoteException(statusCode, response.getStatusLine().getReasonPhrase(), uri));
      } else {
        result.accept(func.apply(response));
      }
    } catch (Exception e) {
      result.addError(e);
    }
    return result;
  }

}
