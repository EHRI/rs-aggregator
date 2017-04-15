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

    ResourceReader rsReader = new ResourceReader(getHttpclient());
    File file = new File("target/test-output/rsreader/nice.txt");
    Result<File> result = rsReader.read(uri, file);

    assertThat(result.getErrors().isEmpty(), is(true));
    assertThat(result.getStatusCode(), is(200));
    assertThat(result.getContent().isPresent(), is(true));

    File file2 = result.getContent().get();
    assertThat(file.exists(), is(true));
    assertThat(file, equalTo(file2));
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

    ResourceReader rsReader = new ResourceReader(getHttpclient());
    File file = new File("target/test-output/rsreader/not_found.txt");
    Result<File> result = rsReader.read(uri, file);

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

    ResourceReader rsReader = new ResourceReader(getHttpclient());
    File file = new File("target/test-output/rsreader/no_content.txt");
    Result<File> result = rsReader.read(uri, file);

    assertThat(result.getErrors().isEmpty(), is(true));
    assertThat(result.getStatusCode(), is(204));
    assertThat(result.getContent().isPresent(), is(false));
  }

}
