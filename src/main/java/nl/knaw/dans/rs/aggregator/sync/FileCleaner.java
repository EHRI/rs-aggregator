package nl.knaw.dans.rs.aggregator.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

/**
 * Created on 2017-04-29 10:52.
 */
public class FileCleaner implements FileVisitor<Path> {

  private static Logger logger = LoggerFactory.getLogger(FileCleaner.class);

  private final Set<File> fileSet;

  public FileCleaner(Set<File> fileSet) {
    this.fileSet = fileSet;
  }

  @Override
  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
    //System.out.println("visitFile: " + file + " -> " + attrs);
    File bFile = file.toAbsolutePath().toFile();
    if (!fileSet.contains(bFile)) {
      boolean deleted = bFile.delete();
      if (deleted) logger.debug("Deleted file {}", file);
    }
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
    logger.error("While visiting file {}", file, exc);
    throw exc;
  }

  @Override
  public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
    File directory = dir.toAbsolutePath().toFile();
    File[] children = directory.listFiles();
    if (children == null || children.length == 0) {
      boolean deleted = directory.delete();
      if (deleted) logger.debug("Deleted directory {}", dir);
    }
    return FileVisitResult.CONTINUE;
  }
}
