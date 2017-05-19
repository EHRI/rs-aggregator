package nl.knaw.dans.rs.aggregator.xml;


import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * JAXBContext for marshalling and unmarshalling of ResourceSync documents.
 * This class uses the {@link ObjectFactory} class.
 */
public class ResourceSyncContext {

  private final JAXBContext jaxbContext;

  public ResourceSyncContext() throws JAXBException {
    jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
  }

  public Marshaller createMarshaller() throws JAXBException {
    return jaxbContext.createMarshaller();
  }

  public Unmarshaller createUnmarshaller() throws JAXBException {
    return jaxbContext.createUnmarshaller();
  }


}
