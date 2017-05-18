package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.syncore.VerificationPolicy;
import nl.knaw.dans.rs.aggregator.syncore.VerificationStatus;

import static nl.knaw.dans.rs.aggregator.syncore.VerificationStatus.*;

/**
 * A default {@link VerificationPolicy} that should be a valid policy under most circumstances where
 * at least the hash and last modification date of a resource are given by Sources and these attributes
 * are verifiable by the Destination.
 */
public class DefaultVerificationPolicy implements VerificationPolicy {

  @Override
  public boolean continueVerification(VerificationStatus stHash, VerificationStatus stLastMod,
                                      VerificationStatus stSize) {
    return true;
  }

  @Override
  public boolean repeatDownload(VerificationStatus stHash, VerificationStatus stLastMod, VerificationStatus stSize) {
    if (stHash == verification_failure || stLastMod == verification_failure) {
      return true; // keep strict policy on last modification date.
    } else if (stHash == verification_success) {
      return false; // perfect.
    } else if ((stLastMod == verification_success && stSize == verification_success)) {
      return false; // will do if no hash is available.
    } else if (stHash == not_verified && stLastMod == not_verified && stSize == not_verified) {
      return true; // no verification possible: repeat download.
    }
    return true; // under all other conditions.
  }

  @Override
  public boolean isVerified(VerificationStatus stHash, VerificationStatus stLastMod, VerificationStatus stSize) {
    return stHash == verification_success || (stLastMod == verification_success && stSize == verification_success);
  }
}
