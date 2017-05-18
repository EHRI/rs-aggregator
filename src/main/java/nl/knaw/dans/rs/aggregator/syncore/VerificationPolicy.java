package nl.knaw.dans.rs.aggregator.syncore;


/**
 * Policies for verification of resources.
 * <p>
 * The ResourceSync Specification is a framework. Depending on community-specific agreements, the hash,
 * last modification date and size of a resource may or may not be given as attributes on an item. Depending on the
 * specific implementation of the {@link ResourceManager} these attributes, if given, may or may not be
 * verifiable. A VerificationPolicy should adjust for these Source community- and Destination-specific
 * circumstances.
 * </p>
 */
public interface VerificationPolicy {

  /**
   * Should successive verifications go on, given the status of verifications.
   *
   * @param stHash {@link VerificationStatus} of hash of resource
   * @param stLastMod {@link VerificationStatus} of last modified date of resource
   * @param stSize {@link VerificationStatus} of the size of the resource
   * @return <code>true</code> if verification should continue, <code>false</code> otherwise
   */
  boolean continueVerification(VerificationStatus stHash, VerificationStatus stLastMod, VerificationStatus stSize);

  /**
   * Should we repeat the download of a resource, given the status of verifications.
   *
   * @param stHash {@link VerificationStatus} of hash of resource
   * @param stLastMod {@link VerificationStatus} of last modified date of resource
   * @param stSize {@link VerificationStatus} of the size of the resource
   * @return <code>true</code> if download should be repeated, <code>false</code> otherwise
   */
  boolean repeatDownload(VerificationStatus stHash, VerificationStatus stLastMod, VerificationStatus stSize);

  /**
   * Can the resource be marked as verified, given the status of verifications.
   *
   * @param stHash {@link VerificationStatus} of hash of resource
   * @param stLastMod {@link VerificationStatus} of last modified date of resource
   * @param stSize {@link VerificationStatus} of the size of the resource
   * @return <code>true</code> if resource should be marked as verified, <code>false</code> otherwise
   */
  boolean isVerified(VerificationStatus stHash, VerificationStatus stLastMod, VerificationStatus stSize);


}
