package org.opentripplanner.netex.loader;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Simple wrapper around a zip file and file entry. */
class FileEntry {
    private final ZipFile zipFile;
    private final ZipEntry entry;

    public FileEntry(ZipFile zipFile, ZipEntry entry) {
        this.zipFile = zipFile;
        this.entry = entry;
    }

    public String filename() {
        return entry.getName();
    }

    byte[] toBytes() throws IOException {
        return IOUtils.toByteArray(zipFile.getInputStream(entry));
    }
}
