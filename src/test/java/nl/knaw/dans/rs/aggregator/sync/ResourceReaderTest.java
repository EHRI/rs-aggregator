package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.discover.RemoteException;
import nl.knaw.dans.rs.aggregator.http.AbstractRemoteTest;
import nl.knaw.dans.rs.aggregator.http.Result;
import org.junit.Test;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.File;
import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Created on 2017-04-12 14:19.
 */
public class ResourceReaderTest extends AbstractRemoteTest {

  @Test
  public void testCreateFilePathFromUri() {
    String[] expectations = {
      "http://zandbak2.dans.knaw.nl", "base-dir/nl/knaw/dans/zandbak2",
      "http://zandbak2.dans.knaw.nl:80", "base-dir/nl/knaw/dans/zandbak2/80",
      "http://zandbak2.dans.knaw.nl/path/to/file.txt", "base-dir/nl/knaw/dans/zandbak2/path/to/file.txt",
    };

    for (int i = 0; i < expectations.length; i += 2) {
      URI uri = URI.create(expectations[i]);
      String exp = expectations[i+1];
      File file = ResourceReader.createFilePathFromUri("base-dir", uri);
      System.out.println(uri.toString() + " > " + file);
      assertThat(file.toString(), equalTo(exp));
    }
  }

  @Test
  public void testRead() throws Exception {
    String path = "/rsserv/nice.txt";
    URI uri = composeUri(path);
    byte[] body = "!@#$%^&*(".getBytes();

    getMockServer()
      .when(HttpRequest.request()
          .withMethod("GET")
          .withPath(path),
        Times.exactly(1))

      .respond(HttpResponse.response()
        .withStatusCode(200)
        .withHeader("Content-Type", "text/plain; utf-8")
        .withBody(body)
      );

    ResourceReader rsReader = new ResourceReader(getHttpclient(), "target/test-output");
    Result<File> result = rsReader.read(uri);

    assertThat(result.getErrors().isEmpty(), is(true));
    assertThat(result.getStatusCode(), is(200));
    assertThat(result.getContent().isPresent(), is(true));

    File file = result.getContent().get();
    assertThat(file.exists(), is(true));
  }

  @Test
  public void testReadAnd404() throws Exception {
    String path = "/rsserv/nice.txt";
    URI uri = composeUri(path);

    getMockServer()
      .when(HttpRequest.request()
          .withMethod("GET")
          .withPath(path),
        Times.exactly(1))

      .respond(HttpResponse.response()
        .withStatusCode(404)
        .withHeader("Content-Type", "text/plain; utf-8")
        .withBody("Not Found")
      );

    ResourceReader rsReader = new ResourceReader(getHttpclient(), "target/test-output");
    Result<File> result = rsReader.read(uri);

    assertThat(result.getErrors().isEmpty(), is(false));
    assertThat(result.getStatusCode(), is(404));
    assertThat(result.getContent().isPresent(), is(false));
    assertThat(result.getErrors().get(0), instanceOf(RemoteException.class));
  }

  @Test
  public void testReadAndNoContent() throws Exception {
    String path = "/rsserv/nice.txt";
    URI uri = composeUri(path);

    getMockServer()
      .when(HttpRequest.request()
          .withMethod("GET")
          .withPath(path),
        Times.exactly(1))

      .respond(HttpResponse.response()
        .withStatusCode(204)
      );

    ResourceReader rsReader = new ResourceReader(getHttpclient(), "target/test-output");
    Result<File> result = rsReader.read(uri);

    assertThat(result.getErrors().isEmpty(), is(true));
    assertThat(result.getStatusCode(), is(204));
    assertThat(result.getContent().isPresent(), is(false));
  }

}
