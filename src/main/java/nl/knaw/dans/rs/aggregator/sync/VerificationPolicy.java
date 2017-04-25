package nl.knaw.dans.rs.aggregator.sync;


/**
 * Created on 2017-04-24 14:06.
 */
public interface VerificationPolicy {

  boolean continueVerification(VerificationStatus stHash, VerificationStatus stLastMod, VerificationStatus stSize);

  boolean repeatDownload(VerificationStatus stHash, VerificationStatus stLastMod, VerificationStatus stSize,
                         int downloadCount, boolean resourceExists);

  boolean isVerified(VerificationStatus stHash, VerificationStatus stLastMod, VerificationStatus stSize,
                     boolean resourceExists);


}
