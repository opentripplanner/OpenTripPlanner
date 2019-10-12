package org.opentripplanner.netex.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Arrange zip file entries into a hierarchy:
 * <pre>
 *     1. Shared files               -- a set of shared files
 *     2. Group files                -- a set of files grouped by a naming convention
 *         2.1 Shared group files        -- Shared within the group
 *         2.2 (Individual) Group files  -- Not shared
 * </pre>
 *
 * The files is loaded in the hierarchical order. First the <em>Shared files</em>,
 * then for each group: shared group files are loaded before individual group files.
 * <p>
 * All NeTEx entities are cached in an index made available for reference linking. To save
 * memory shared group files entities are discarded after the group is loaded (and linking is
 * complete). Entities in individual group files are discarded after the file entry is loaded.
 */
public class NetexZipFileHierarchy {
    private static final Logger LOG = LoggerFactory.getLogger(NetexZipFileHierarchy.class);

    private final File file;

    private final List<FileEntry> sharedEntries = new ArrayList<>();
    private final Map<String, GroupEntries> groupEntries = new TreeMap<>();

    private final Pattern ignoreFilePattern;
    private final Pattern sharedFilePattern;
    private final Pattern sharedGroupFilePattern;
    private final Pattern groupFilePattern;

    private String currentGroup = null;

    public NetexZipFileHierarchy(
            Pattern ignoreFilePattern,
            Pattern sharedFilePattern,
            Pattern sharedGroupFilePattern,
            Pattern groupFilePattern,
            File zipFile
    ) {
        this.ignoreFilePattern = ignoreFilePattern;
        this.sharedFilePattern = sharedFilePattern;
        this.sharedGroupFilePattern = sharedGroupFilePattern;
        this.groupFilePattern = groupFilePattern;
        this.file = zipFile;
    }

    /** load the bundle, map it to the OTP transit model and return */
    void load(Consumer<NetexZipFileHierarchy> loadData) {
        try {
            try (ZipFile zipFile = new ZipFile(file, ZipFile.OPEN_READ)) {
                buildHierarchy(zipFile);
                loadData.accept(this);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    String filename() {
        return file.getPath();
    }

    private void buildHierarchy(ZipFile zipFile) {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();

            if(ignoredFile(name)) {
                LOG.debug("Netex file ignored: {}.", name);
            }
            else if (isSharedFile(name)) {
                sharedEntries.add(new FileEntry(zipFile, entry));
            }
            else if (matchesGroup(name, sharedGroupFilePattern)) {
                groupEntries.get(currentGroup).addSharedEntry(new FileEntry(zipFile, entry));
            }
            else if (matchesGroup(name, groupFilePattern)) {
                groupEntries.get(currentGroup).addIndependentEntries(new FileEntry(zipFile, entry));
            }
            else {
                LOG.warn(
                        "Netex file ignored: {}. The file do not " +
                                "match any file patterns in the config.", name
                );
            }
        }
    }

    Iterable<FileEntry> sharedEntries() {
        return sharedEntries;
    }

    Iterable<GroupEntries> groups() {
        return groupEntries.values();
    }

    private boolean ignoredFile(String name) {
        return ignoreFilePattern.matcher(name).matches();
    }

    private boolean isSharedFile(String name) {

        return sharedFilePattern.matcher(name).matches();
    }

    private boolean matchesGroup(String name, Pattern filePattern) {
        Matcher m = filePattern.matcher(name);
        if (!m.matches()) {
            return false;
        }
        try {
            currentGroup = m.group(1);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalStateException("Netex file patten '" + filePattern
                    + "' is missing a group pattern like: '(\\w+)' in '(\\w+)-.*\\.xml' ");
        }
        groupEntries.computeIfAbsent(currentGroup, GroupEntries::new);
        return true;
    }

    void checkFileExist() {
        if (file != null) {
            if (!file.exists()) {
                throw new RuntimeException("NETEX Path " + file + " does not exist.");
            }
            if (!file.canRead()) {
                throw new RuntimeException("NETEX Path " + file + " cannot be read.");
            }
        }
    }
}
