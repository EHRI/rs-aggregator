package nl.knaw.dans.rs.aggregator.sync;


/**
 * Created on 2017-04-24 14:06.
 */
public interface VerificationPolicy {

  boolean continueVerification(VerificationStatus hash, VerificationStatus lastMod, VerificationStatus file_length);

  boolean repeatDownload(VerificationStatus hash, VerificationStatus lastMod, VerificationStatus file_length, int downloadCounter);


}
