package nl.knaw.dans.rs.aggregator.xml;


import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Optional;

/**
 * Utility class for building {@link RsRoot} class hierarchies. The concrete RsRoot is either a
 * {@link Urlset} or a {@link Sitemapindex}.
 */
public class RsBuilder {

  private final ResourceSyncContext rsContext;

  private File file;
  private InputSource inputSource;
  private InputStream inputStream;
  private Node node;
  private Reader reader;
  private Source source;
  private URL url;
  private XMLEventReader xmlEventReader;
  private XMLStreamReader xmlStreamReader;

  private QName latestQName;
  private Urlset urlset;
  private Sitemapindex sitemapindex;

  /**
   * Constructor.
   *
   * @param rsContext ResourceSyncContext can be reused over multiple instances of RsBuilder.
   */
  public RsBuilder(ResourceSyncContext rsContext) {
    this.rsContext = rsContext;
  }

  /**
   * Build a class hierarchy from the input previously given with one of <code>set</code>-methods.
   * <p>Example usage:</p>
   * <pre>
   *   RsBuilder rsBuilder = new RsBuilder(new ResourceSyncContext());
   *   Optional&lt;RsRoot&gt; maybeRoot = rsBuilder.setInputStream(inStream).build();
   * </pre>
   * <p>We can test what concrete class was unmarshalled by obtaining the QName:</p>
   * <pre>
   *   Optional&lt;QName&gt; maybeQName = rsBuilder.getQName();
   * </pre>
   * <p>Or directly test one of two possibilities:</p>
   * <pre>
   *   Optional&lt;Sitemapindex&gt; maybeSitemapindex = rsBuilder.getSitemapindex();
   *   Optional&lt;Urlset&gt; maybeUrlset = rsBuilder.getUrlset();
   * </pre>
   *
   *
   * @return Optional of RsRoot
   * @throws JAXBException for invalid input
   */
  @SuppressWarnings ("unchecked")
  public Optional<RsRoot> build() throws JAXBException {
    latestQName = null;
    urlset = null;
    sitemapindex = null;

    JAXBElement<RsRoot> je = null;
    RsRoot rsRoot = null;
    Unmarshaller unmarshaller = rsContext.createUnmarshaller();
    if (file != null) {
      je = (JAXBElement<RsRoot>) unmarshaller.unmarshal(file);
      file = null;
    } else if (inputSource != null) {
      je = (JAXBElement<RsRoot>) unmarshaller.unmarshal(inputSource);
      inputSource = null;
    } else if (inputStream != null) {
      je = (JAXBElement<RsRoot>) unmarshaller.unmarshal(inputStream);
      inputStream = null;
    } else if (node != null) {
      je = (JAXBElement<RsRoot>) unmarshaller.unmarshal(node);
      node = null;
    } else if (reader != null) {
      je = (JAXBElement<RsRoot>) unmarshaller.unmarshal(reader);
      reader = null;
    } else if (source != null) {
      je = (JAXBElement<RsRoot>) unmarshaller.unmarshal(source);
      source = null;
    } else if (url != null) {
      je = (JAXBElement<RsRoot>) unmarshaller.unmarshal(url);
      url = null;
    } else if (xmlEventReader != null) {
      je = (JAXBElement<RsRoot>) unmarshaller.unmarshal(xmlEventReader);
      xmlEventReader = null;
    } else if (xmlStreamReader != null) {
      je = (JAXBElement<RsRoot>) unmarshaller.unmarshal(xmlStreamReader);
      xmlStreamReader = null;
    }

    if (je != null) {
      latestQName = je.getName();
      rsRoot = je.getValue();
      if (latestQName.equals(Urlset.QNAME)) {
        urlset = (Urlset) rsRoot;
      } else if (latestQName.equals(Sitemapindex.QNAME)) {
        sitemapindex = (Sitemapindex) rsRoot;
      }
    }
    return Optional.ofNullable(rsRoot);
  }

  /**
   * Get an optional of the QName of the latest unmarshalled document. Either <code>null</code>,
   * {@link Sitemapindex#QNAME} or {@link Urlset#QNAME}.
   *
   * @return optional of QName of latest unmarshalled document
   */
  public Optional<QName> getQName() {
    return Optional.ofNullable(latestQName);
  }

  /**
   * Get an optional Urlset of the latest unmarshalled document. Either <code>null</code>, or {@link Urlset}.
   *
   * @return optional of Urlset
   */
  public Optional<Urlset> getUrlset() {
    return Optional.ofNullable(urlset);
  }

  /**
   * Get an optional Sitemapindex of the latest unmarshalled document. Either <code>null</code>,
   * or {@link Sitemapindex}.
   *
   * @return optional of Sitemapindex
   */
  public Optional<Sitemapindex> getSitemapindex() {
    return Optional.ofNullable(sitemapindex);
  }

  public RsBuilder setFile(File file) {
    this.file = file;
    return this;
  }

  public RsBuilder setInputSource(InputSource inputSource) {
    this.inputSource = inputSource;
    return this;
  }

  public RsBuilder setInputStream(InputStream inputStream) {
    this.inputStream = inputStream;
    return this;
  }

  public RsBuilder setNode(Node node) {
    this.node = node;
    return this;
  }

  public RsBuilder setReader(Reader reader) {
    this.reader = reader;
    return this;
  }

  public RsBuilder setSource(Source source) {
    this.source = source;
    return this;
  }

  public RsBuilder setUrl(URL url) {
    this.url = url;
    return this;
  }

  public RsBuilder setXmlEventReader(XMLEventReader xmlEventReader) {
    this.xmlEventReader = xmlEventReader;
    return this;
  }

  public RsBuilder setXmlStreamReader(XMLStreamReader xmlStreamReader) {
    this.xmlStreamReader = xmlStreamReader;
    return this;
  }

  /**
   * Marshal a class hierarchy to its xml-representation.
   *
   * @param rsRoot the root class of the class hierarchy to marshal
   * @param formattedOutput format the output as pretty xml
   * @return xml representation as String
   * @throws JAXBException for invalid input
   */
  public String toXml(RsRoot rsRoot, boolean formattedOutput) throws JAXBException {
    Marshaller marshaller = rsContext.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formattedOutput);
    StringWriter writer = new StringWriter();
    marshaller.marshal(rsRoot, writer);
    return writer.toString();
  }
}
