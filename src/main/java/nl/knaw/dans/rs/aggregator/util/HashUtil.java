package nl.knaw.dans.rs.aggregator.util;

import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created on 2017-04-28 13:05.
 */
public class HashUtil {

  public static String computeHash(String algorithm, @Nonnull InputStream inputStream)
    throws NoSuchAlgorithmException, IOException {

    StringBuilder sb;
    try {
      MessageDigest digest = MessageDigest.getInstance(algorithm);
      byte[] byteArray = new byte[1024];
      int bytesCount = 0;
      while ((bytesCount = inputStream.read(byteArray)) != -1) {
        digest.update(byteArray, 0, bytesCount);
      }
      byte[] bytes = digest.digest();
      sb = new StringBuilder();
      for (byte aByte : bytes) {
        sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
      }
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
    return sb.toString();
  }
}
