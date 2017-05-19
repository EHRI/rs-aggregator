/**
 * JAXB implementation for marshalling and unmarshalling sitemap documents.
 * <p>
 *   The class {@link nl.knaw.dans.rs.aggregator.xml.RsBuilder} can be used to marshal and unmarshal
 *   ResourceSync sitemap documents to and from a class hierarchy.
 * </p>
 * <pre>
 *   RsBuilder rsBuilder = new RsBuilder(new ResourceSyncContext());
 *   Optional&lt;RsRoot&gt; maybeRoot = rsBuilder.setInputStream(inStream).build();
 * </pre>
 * <p>
 *   {@link nl.knaw.dans.rs.aggregator.xml.RsRoot} is the superclass of two classes that represent
 * the two possible root elements of a sitemap: &lt;sitemapindex&gt; and &lt;urlset&gt;: the classes
 * {@link nl.knaw.dans.rs.aggregator.xml.Sitemapindex} and {@link nl.knaw.dans.rs.aggregator.xml.Urlset}.
 * </p>
 * <p>
 *   {@link nl.knaw.dans.rs.aggregator.xml.RsBuilder} can also be used to marshal a class hierarchy to its
 *   xml-representation. Continuing from the previous example (and knowing the unmarshalled thing was a Urlset):
 * </p>
 * <pre>
 *   Urlset urlSet = rsBuilder.getUrlset().orElseThrow(RuntimeException::new);
 *   String xml = rsBuilder.toXml(urlSet, true);
 * </pre>
 */

@XmlSchema(
  namespace = "http://www.sitemaps.org/schemas/sitemap/0.9",
  xmlns = {
    @XmlNs(prefix = "", namespaceURI = "http://www.sitemaps.org/schemas/sitemap/0.9"),
    @XmlNs(prefix = "rs", namespaceURI = "http://www.openarchives.org/rs/terms/")
  },
  elementFormDefault = XmlNsForm.QUALIFIED,
  attributeFormDefault = XmlNsForm.UNQUALIFIED)

@XmlJavaTypeAdapters({
  @XmlJavaTypeAdapter(type = ZonedDateTime.class, value = ZonedDateTimeAdapter.class)
  })

package nl.knaw.dans.rs.aggregator.xml;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import java.time.ZonedDateTime;

