package nl.knaw.dans.rs.aggregator.sync;

import nl.knaw.dans.rs.aggregator.discover.ResultIndex;
import nl.knaw.dans.rs.aggregator.util.FileCleaner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created on 2017-05-15 16:27.
 */
public class DefaultSyncPostProcessor implements SyncPostProcessor {

  public static final String SZ = "sz.";
  // properties set by DefaultSyncPostProcessor
  public static final String PROP_SZ_DELETED_METADATA_FILE_COUNT = SZ + "deleted.metadata.file.count";
  public static final String PROP_SZ_DELETED_SYNC_PROP_FILE_COUNT = SZ + "deleted.sync.prop.file.count";

  private static Logger logger = LoggerFactory.getLogger(DefaultSyncPostProcessor.class);

  private int keepValidSyncProps = 10;

  public int getKeepValidSyncProps() {
    return keepValidSyncProps;
  }

  public void setKeepValidSyncProps(int keepValidSyncProps) {
    this.keepValidSyncProps = keepValidSyncProps;
  }

  @Override
  public void postProcess(PathFinder pathFinder, SyncProperties syncProps, ResultIndex resultIndex) throws Exception {
    int deletedMetadataFileCount = cleanUpMetadataDirectory(pathFinder, syncProps, resultIndex);
    int deletedSyncPropFileCount = cleanUpSyncProps(pathFinder, syncProps);

    syncProps.setInt(PROP_SZ_DELETED_METADATA_FILE_COUNT, deletedMetadataFileCount);
    syncProps.setInt(PROP_SZ_DELETED_SYNC_PROP_FILE_COUNT, deletedSyncPropFileCount);
    try {
      File file = pathFinder.getSyncPropXmlFile();
      String lsb = "Last saved by " + this.getClass().getName();
      syncProps.storeToXML(file, lsb);
      logger.debug("Saved SyncWorker properties to {}", file);
    } catch (IOException e) {
      logger.error("Could not save syncProps", e);
      throw new RuntimeException(e);
    }
  }

  private int cleanUpMetadataDirectory(PathFinder pathFinder, SyncProperties syncProps, ResultIndex resultIndex) {
    int deletedFiles = 0;
    if (syncProps.getBool(SyncProperties.PROP_SW_FULLY_SYNCHRONIZED)) {
      Set<File> fileSet = pathFinder.findMetadataFilePaths(resultIndex.getResultMap().keySet());
      FileCleaner fileCleaner = new FileCleaner(fileSet);
      try {
        Files.walkFileTree(pathFinder.getMetadataDirectory().toPath(), fileCleaner);
      } catch (IOException e) {
        throw new RuntimeException("Could not clean metadata directory for " + pathFinder.getCapabilityListUri(), e);
      }
      deletedFiles = fileCleaner.getDeletedFileCount();
    }
    return deletedFiles;
  }

  private int cleanUpSyncProps(PathFinder pathFinder, SyncProperties syncProps) {
    int deletedSyncProps = 0;
    File[] syncPropFiles = pathFinder.getSyncPropDirectory().listFiles(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        return pathname.getName().endsWith(".xml");
      }
    });
    if (syncPropFiles == null) return 0;
    List<File> syncPropList = Arrays.asList(syncPropFiles);
    Collections.sort(syncPropList);
    Collections.reverse(syncPropList);
    int validSyncProps = 0;
    for (File file : syncPropList) {
      SyncProperties sp = new SyncProperties();
      try {
        sp.loadFromXML(file);
        if (sp.getBool(SyncProperties.PROP_SW_FULLY_SYNCHRONIZED)) validSyncProps++;
        if (validSyncProps > keepValidSyncProps) {
          boolean deleted = file.delete();
          if (deleted) {
            deletedSyncProps++;
            logger.debug("Deleted syncProperties file {}", file);
          }
        }
      } catch (IOException e) {
        throw new RuntimeException("Could not clean synchronisation-properties directory for "
          + pathFinder.getCapabilityListUri(), e);
      }
    }
    return deletedSyncProps;
  }
}
