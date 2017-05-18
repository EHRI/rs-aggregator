package nl.knaw.dans.rs.aggregator.util;

import nl.knaw.dans.rs.aggregator.util.FileCleaner;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * Created on 2017-04-29 11:00.
 */
public class FileCleanerTest {

  @Test
  @Ignore("Assumes non-existing directory")
  public void testCleaner() throws Exception {
    Set<File> fileSet = new HashSet<>();
    fileSet.add(new File("/Users/ecco/git/rs-aggregator/target/test-output/synchronizer/zandbak11.dans.knaw.nl/ehri2/mdx/__SOR__/ehri2/tmp/rs/collection2/folder1/doc1.txt"));
    fileSet.add(new File("/Users/ecco/git/rs-aggregator/target/test-output/synchronizer/zandbak11.dans.knaw.nl/ehri2/mdx/__SOR__/ehri2/tmp/rs/collection2/folder1/doc2.txt"));
    FileCleaner cleaner = new FileCleaner(fileSet);
    Path startingDir = Paths.get("target/test-output/synchronizer/zandbak11.dans.knaw.nl/ehri2/mdx/__SOR__");
    Files.walkFileTree(startingDir, cleaner);
  }
}
