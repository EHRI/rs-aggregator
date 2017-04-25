package nl.knaw.dans.rs.aggregator.sync;

import static nl.knaw.dans.rs.aggregator.sync.VerificationStatus.*;

/**
 * Created on 2017-04-24 14:42.
 */
public class DefaultVerificationPolicy implements VerificationPolicy {

  @Override
  public boolean continueVerification(VerificationStatus stHash, VerificationStatus stLastMod,
                                      VerificationStatus stSize) {
    return true;
  }

  @Override
  public boolean repeatDownload(VerificationStatus stHash, VerificationStatus stLastMod, VerificationStatus stSize,
                                int downloadCounter, boolean resourceExists) {
    if (!resourceExists) {
      return true;
    } else if (stHash == verification_success) {
      return false;
    } else if (stLastMod == verification_success && stSize == verification_success) {
      return false;
    } else if (stHash == not_verified && stLastMod == not_verified && stSize == not_verified && downloadCounter >= 1) {
      // no verification possible: repeat download once.
      return false;
    }
    return true;
  }

  @Override
  public boolean isVerified(VerificationStatus stHash, VerificationStatus stLastMod, VerificationStatus stSize, boolean resourceExists) {
    if (stHash == verification_success || (stLastMod == verification_success && stSize == verification_success)) {
      return true;
    } else {
      return false;
    }
  }
}
