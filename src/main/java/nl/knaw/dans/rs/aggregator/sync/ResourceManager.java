package nl.knaw.dans.rs.aggregator.sync;

import javax.annotation.Nonnull;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Set;

/**
 * A ResourceManager manages resources and is capable of answering simple questions about the managed resources.
 * Resources are indicated by their normalized {@link java.net.URI}.
 *
 * For each set of resources the accompanying {@link PathFinder} will be set on the ResourceManager.
 * See {@link ResourceManager#setPathFinder(PathFinder)}
 *
 * @see nl.knaw.dans.rs.aggregator.util.NormURI#normalize(URI)
 */
public interface ResourceManager {

  /**
   * Sets the current pathFinder on this ResourceManager. The pathFinder may be useful, even if resources
   * are not managed on the file system. For instance the URI of the capabilityList that defines the
   * current set of resources can be found with {@link PathFinder#getCapabilityListUri()}.
   *
   * A ResourceManager should rely on the fact that each call to this method signifies the
   * synchronisation of a new set of resources.
   *
   * @param pathFinder the pathFinder for the current set of resources
   */
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

  /**
   * Clean up the managed store. Only resources that are identified by the given <code>normalizedURIs</code>
   * should be kept in the managed store, others should be removed or deleted.
   *
   * This call enables the ResourceManager to clean up if the Source issues a new ResourceList. The ResourceManager
   * should only remove resources that are not listed. The ResourceManager will be enabled
   * to {@link ResourceManager#create(URI)} and/or {@link ResourceManager#update(URI)} resources that are indicated
   * in the set but are not yet in the managed store.
   *
   * @param normalizedURIs identify the resources that should be kept in the managed store.
   * @return <code>true</code> if the operation succeeded, <code>false</code> otherwise
   */
  boolean keepOnly(@Nonnull Set<URI> normalizedURIs);

  /**
   * Keep the resource that is identified by the given <code>normalizedURI</code>. The resource is present
   * and verified to be up-to-date. So the ResourceManager is not expected to handle the indicated resource,
   * but may, of course, take any action it deems appropriate.
   *
   * @param normalizedURI identifies the resource
   * @return <code>true</code> if the operation succeeded, <code>false</code> otherwise
   */
  boolean keep(@Nonnull URI normalizedURI);

  /**
   * Create the resource that is identified by the given <code>normalizedURI</code>. The resource was not present.
   *
   * @param normalizedURI identifies the resource
   * @return <code>true</code> if the operation succeeded, <code>false</code> otherwise
   */
  boolean create(@Nonnull URI normalizedURI);

  /**
   * Update the resource that is identified by the given <code>normalizedURI</code>. The resource was present but
   * was not up-to-date.
   *
   * @param normalizedURI identifies the resource
   * @return <code>true</code> if the operation succeeded, <code>false</code> otherwise
   */
  boolean update(@Nonnull URI normalizedURI);

  /**
   * Delete or remove the resource that is identified by the given <code>normalizedURI</code>. The resource
   * was present but was purged from the set of resources.
   *
   * @param normalizedURI identifies the resource
   * @return <code>true</code> if the operation succeeded, <code>false</code> otherwise
   */
  boolean delete(@Nonnull URI normalizedURI);

}
