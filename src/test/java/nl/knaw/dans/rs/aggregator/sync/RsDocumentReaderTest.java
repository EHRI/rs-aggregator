package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.http.AbstractRemoteTest;
import nl.knaw.dans.rs.aggregator.http.Result;
import nl.knaw.dans.rs.aggregator.xml.Capability;
import nl.knaw.dans.rs.aggregator.xml.RsRoot;
import org.junit.Test;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * Created on 2017-04-10 11:56.
 */
public class RsDocumentReaderTest extends AbstractRemoteTest {

  @Test
  public void testReadwith404Uri() throws Exception {
    String path = "/rsserv/capabilitylist.xml";
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

    RsDocumentReader reader = new RsDocumentReader(getHttpclient(), getRsContext());
    Result<RsRoot> result = reader.read(uri);
    assertThat(result.getErrors().isEmpty(), is(false));
    assertThat(result.getStatusCode(), is(404));

    assertThat(reader.getLatestUrlset().isPresent(), is(false));
    assertThat(reader.getLatestSitemapindex().isPresent(), is(false));
  }

  @Test
  public void testReadwithCapabilityListUriUri() throws Exception {
    String path = "/rsserv/capabilitylist.xml";
    URI uri = composeUri(path);

    getMockServer()
      .when(HttpRequest.request()
          .withMethod("GET")
          .withPath(path),
        Times.exactly(1))

      .respond(HttpResponse.response()
        .withStatusCode(200)
        .withHeader("Content-Type", "text/xml; utf-8")
        .withBody(createCapabilityList())
      );

    RsDocumentReader reader = new RsDocumentReader(getHttpclient(), getRsContext());
    Result<RsRoot> result = reader.read(uri);
    assertThat(result.getErrors().isEmpty(), is(true));
    assertThat(result.getStatusCode(), is(200));
    assertThat(result.getContent().isPresent(), is(true));
    RsRoot document = result.getContent().orElse(null);
    assertThat(document.getCapability().isPresent(), is(true));
    assertThat(document.getCapability().get(), is(Capability.CAPABILITYLIST));

    assertThat(reader.getLatestUrlset().isPresent(), is(true));
    assertThat(reader.getLatestSitemapindex().isPresent(), is(false));

//    RsBuilder rsBuilder = new RsBuilder(getRsContext());
//    System.out.println(rsBuilder.toXml(document, true));
  }

  @Test
  public void testReadInvalidXml() throws Exception {
    String path = "/rsserv/capabilitylist.xml";
    URI uri = composeUri(path);

    getMockServer()
      .when(HttpRequest.request()
          .withMethod("GET")
          .withPath(path),
        Times.exactly(1))

      .respond(HttpResponse.response()
        .withStatusCode(200)
        .withHeader("Content-Type", "text/xml; utf-8")
        .withBody(createInvalidXml())
      );

    RsDocumentReader reader = new RsDocumentReader(getHttpclient(), getRsContext());
    Result<RsRoot> result = reader.read(uri);

    assertThat(result.getContent().isPresent(), is(false));
    assertThat(result.getStatusCode(), is(200));
    assertThat(result.getErrors().size(), is(1));
    assertThat(result.getErrors().get(0), instanceOf(javax.xml.bind.UnmarshalException.class));
  }

  @Test
  public void testReadNoContent() throws Exception {
    String path = "/rsserv/nothing";
    URI uri = composeUri(path);

    getMockServer()
      .when(HttpRequest.request()
          .withMethod("GET")
          .withPath(path),
        Times.exactly(1))

      .respond(HttpResponse.response()
        .withStatusCode(204)
      );

    RsDocumentReader reader = new RsDocumentReader(getHttpclient(), getRsContext());
    Result<RsRoot> result = reader.read(uri);

    assertThat(result.getContent().isPresent(), is(false));
    assertThat(result.getStatusCode(), is(204));
    assertThat(result.getErrors().size(), is(0));
  }

  private String createCapabilityList() {
    return
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"\n" +
        "        xmlns:rs=\"http://www.openarchives.org/rs/terms/\">\n" +
        "  <rs:ln rel=\"describedby\"\n" +
        "         href=\"http://example.com/info_about_set1_of_resources.xml\"/>\n" +
        "  <rs:ln rel=\"up\"\n" +
        "         href=\"http://example.com/resourcesync_description.xml\"/>\n" +
        "  <rs:md capability=\"capabilitylist\"/>\n" +
        "  <url>\n" +
        "      <loc>http://example.com/dataset1/resourcelist.xml</loc>\n" +
        "      <rs:md capability=\"resourcelist\"/>\n" +
        "  </url>\n" +
        "  <url>\n" +
        "      <loc>http://example.com/dataset1/resourcedump.xml</loc>\n" +
        "      <rs:md capability=\"resourcedump\"/>\n" +
        "  </url>\n" +
        "  <url>\n" +
        "      <loc>http://example.com/dataset1/changelist.xml</loc>\n" +
        "      <rs:md capability=\"changelist\"/>\n" +
        "  </url>\n" +
        "  <url>\n" +
        "      <loc>http://example.com/dataset1/changedump.xml</loc>\n" +
        "      <rs:md capability=\"changedump\"/>\n" +
        "  </url>\n" +
        "</urlset>\n";
  }

  private String createInvalidXml() {
    return
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"\n" +
        "        xmlns:rs=\"http://www.openarchives.org/rs/terms/\">\n" +
        "  <rs:ln rel=\"describedby\"\n" +
        "         href=\"http://example.com/info_about_set1_of_resources.xml\"/>\n" +
        "  <rs:ln rel=\"up\"\n" +
        "         href=\"http://example.com/resourcesync_description.xml\"/>\n" +
        "  <rs:md capability=\"capabilitylist\"/>\n" +
        "  <url>\n" +
        "      <loc>http://example.com/dataset1/resourcelist.xml</loc>\n" +
        "      <rs:md capability=\"resourcelist\"/>\n" +
        "  </url>\n" +
        "  <url>\n" +
        "      <loc>http://example.com/dataset1/resourcedump.xml</loc>\n" +
        "      <rs:md capability=\"resourcedump\"/>\n" +
        "      <priority>0.8</priority>" +
        "  </url>\n" +
        "  <the-spanish-inquisition>foo</the-spanish-insition>" +
        "  <url>\n" +
        "      <loc>http://example.com/dataset1/changelist.xml</loc>\n" +
        "      <rs:md capability=\"changelist\"/>\n" +
        "  </url>\n" +
        "  <url>\n" +
        "      <loc>http://example.com/dataset1/changedump.xml</loc>\n" +
        "      <rs:md capability=\"changedump\"/>\n" +
        "  </url>\n" +
        "</urlset>\n";
  }

}
