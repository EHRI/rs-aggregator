package nl.knaw.dans.rs.aggregator.http;

import nl.knaw.dans.rs.aggregator.discover.RemoteException;
import nl.knaw.dans.rs.aggregator.util.LambdaUtil;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;

/**
 * Execute requests and store response in a {@link Result}.
 */
public class AbstractUriReader {
  private final CloseableHttpClient httpClient;
  private boolean keepingHeaders = false;
  private URI currentUri;

  final static Logger logger = LoggerFactory.getLogger(AbstractUriReader.class);

  public AbstractUriReader(CloseableHttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public static String getCharset(HttpResponse response) {
    ContentType contentType = ContentType.getOrDefault(response.getEntity());
    Charset charset = contentType.getCharset();
    return charset == null ? "UTF-8" : charset.name();
  }

  public boolean isKeepingHeaders() {
    return keepingHeaders;
  }

  public void setKeepingHeaders(boolean keepingHeaders) {
    this.keepingHeaders = keepingHeaders;
  }

  protected CloseableHttpClient getHttpClient() {
    return httpClient;
  }

  protected URI getCurrentUri() {
    return currentUri;
  }

  public <T> Result<T> execute(URI uri, LambdaUtil.Function_WithExceptions<HttpResponse, T, ?> func) {
    logger.debug("Executing GET on uri {}", uri);
    currentUri = uri;
    Result<T> result = new Result<T>(uri);
    HttpGet request = new HttpGet(uri);
    CloseableHttpResponse response = null;
    try  {
      response = httpClient.execute(request);
      int statusCode = response.getStatusLine().getStatusCode();
      result.setStatusLine(response.getStatusLine().toString());
      logger.debug("Received {} from {}", response.getStatusLine(), uri);
      result.setStatusCode(statusCode);
      if (keepingHeaders) {
        for (Header header : response.getAllHeaders()) {
          result.getHeaders().put(header.getName(), header.getValue());
        }
      }
      if (statusCode < 200 || statusCode > 299) {
        result.addError(new RemoteException(statusCode, response.getStatusLine().getReasonPhrase(), uri));
      } else {
        result.accept(func.apply(response));
      }
    } catch (Exception e) {
      logger.error("Error executing GET on uri {}", uri, e);
      result.addError(e);
    } finally {
      closeResponse(response);
    }
    return result;
  }

  private void closeResponse(CloseableHttpResponse response) {
    if (response != null) try {
      response.close();
    } catch (IOException e) {
      logger.error("Error while closing HttpResponse: ", e);
    }
  }
}
