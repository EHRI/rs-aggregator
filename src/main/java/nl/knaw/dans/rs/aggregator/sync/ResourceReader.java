package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.http.AbstractUriReader;
import nl.knaw.dans.rs.aggregator.http.Result;
import nl.knaw.dans.rs.aggregator.util.LambdaUtil;
import nl.knaw.dans.rs.aggregator.xml.RsRoot;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created on 2017-04-11 16:15.
 */
public class ResourceReader extends AbstractUriReader {

  public static File createFilePathFromUri(String baseDir, URI uri) {
    StringBuffer sb = new StringBuffer(baseDir);
    sb.append(File.separator);
    String[] hostParts = uri.getHost().split("\\.");
    for (int i = hostParts.length - 1; i >= 0; i--) {
      sb.append(hostParts[i]).append(File.separator);
    }
    if (uri.getPort() > 0) {
      sb.append(uri.getPort()).append(File.separator);
    }
    sb.append(uri.getPath());
    return new File(sb.toString());
  }

  private final String baseDirectory;

  public ResourceReader(CloseableHttpClient httpClient, String baseDirectory) {
    super(httpClient);
    this.baseDirectory = baseDirectory;
  }

  public String getBaseDirectory() {
    return baseDirectory;
  }

  public Result<File> read(String url) throws URISyntaxException {
    URI uri = new URI(url);
    return read(uri);
  }

  public Result<File> read(URI uri) {
    return execute(uri, fileWriter);
  }

  private LambdaUtil.Function_WithExceptions<HttpResponse, File, Exception> fileWriter = (response) -> {
    HttpEntity entity = response.getEntity();
    if (entity != null) {
      File file = createFilePathFromUri(getBaseDirectory(), getCurrentUri());
      file.getParentFile().mkdirs();
      InputStream instream = entity.getContent();
      OutputStream outstream = new FileOutputStream(file);
      byte[] buffer = new byte[8 * 1024];
      int bytesRead;
      try {
        while ((bytesRead = instream.read(buffer)) != -1) {
          outstream.write(buffer, 0, bytesRead);
        }
      } finally {
        IOUtils.closeQuietly(instream);
        IOUtils.closeQuietly(outstream);
      }
      return file;
    } else {
      return null;
    }
  };


}
