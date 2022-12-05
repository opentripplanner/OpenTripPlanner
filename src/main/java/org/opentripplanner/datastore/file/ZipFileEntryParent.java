package org.opentripplanner.datastore.file;

import java.io.InputStream;
import java.util.zip.ZipEntry;
import org.opentripplanner.datastore.api.FileType;

/**
 * This interface define the role needed by entry play by the zip-file parent.
 * This prevent a cyclic dependency between entry and patent.
 */
interface ZipFileEntryParent {
  InputStream entryStream(ZipEntry entry);
  String path();
  FileType type();
}
