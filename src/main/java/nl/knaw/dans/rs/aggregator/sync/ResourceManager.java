package nl.knaw.dans.rs.aggregator.sync;

import javax.annotation.Nonnull;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Set;

/**
 * A ResourceManager manages resources and is capable of answering simple questions about the managed resources.
 * Resources are indicated by their normalized {@link java.net.URI}.
 *
 * @see nl.knaw.dans.rs.aggregator.util.NormURI#normalize(URI)
 */
public interface ResourceManager {

  void setPathFinder(PathFinder pathFinder);

  /**
   * Does the resource that is identified by the given <code>normalizedURI</code> exist in the resource store.
   *
   * @param normalizedURI identifies the resource
   * @return <code>true</code> if the indicated resource exists, <code>false</code> otherwise
   */
  boolean exists(@Nonnull URI normalizedURI);

  /**
   * Verify the <code>hash</code> of the resource that is identified by the given <code>normalizedURI</code>,
   * computed with the indicated <code>algorithm</code>.
   *
   * @param normalizedURI identifies the resource
   * @param algorithm indicates algorithm for hash computation
   * @param hash hash to verify
   * @return verification status
   */
  VerificationStatus verifyHash(@Nonnull URI normalizedURI, @Nonnull String algorithm, @Nonnull String hash);

  /**
   * Verify the last modified date of the resource that is identified by the given <code>normalizedURI</code>.
   *
   * @param normalizedURI identifies the resource
   * @param lastModified dateTime to verify
   * @return verification status
   */
  VerificationStatus verifyLastModified(@Nonnull URI normalizedURI, @Nonnull ZonedDateTime lastModified);

  /**
   * Verify the size in bytes of the resource that is identified by the given <code>normalizedURI</code>.
   *
   * @param normalizedURI identifies the resource
   * @param size size in bytes of the remote resource
   * @return verification status
   */
  VerificationStatus verifySize(@Nonnull URI normalizedURI, long size);

  boolean keepOnly(@Nonnull Set<URI> normalizedURIs);

  boolean keep(@Nonnull URI normalizedURI);

  boolean create(@Nonnull URI normalizedURI);

  boolean update(@Nonnull URI normalizedURI);

  boolean delete(@Nonnull URI normalizedURI);

}
