package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.http.AbstractUriReader;
import nl.knaw.dans.rs.aggregator.http.Result;
import nl.knaw.dans.rs.aggregator.util.LambdaUtil;
import org.apache.commons.io.IOUtils;
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

  private File currentFile;


  public ResourceReader(CloseableHttpClient httpClient) {
    super(httpClient);
  }

  public Result<File> read(String url, File file) throws URISyntaxException {
    URI uri = new URI(url);
    return read(uri, file);
  }

  public Result<File> read(URI uri, File file) {
    currentFile = file;
    return execute(uri, fileWriter);
  }

  public File getCurrentFile() {
    return currentFile;
  }

  private LambdaUtil.Function_WithExceptions<HttpResponse, File, Exception> fileWriter = (response) -> {
    HttpEntity entity = response.getEntity();
    if (entity != null) {
      File file = getCurrentFile();
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
