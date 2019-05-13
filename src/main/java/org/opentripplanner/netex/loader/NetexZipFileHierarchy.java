package org.opentripplanner.netex.loader;

import org.opentripplanner.standalone.NetexParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class NetexZipFileHierarchy {
    private static final Logger LOG = LoggerFactory
            .getLogger(NetexZipFileHierarchy.class);

    private final ZipFile zipFile;

    private final NetexParameters config;

    private final List<ZipEntry> sharedEntries = new ArrayList<>();

    private final Map<String, GroupEntries> groupEntries = new TreeMap<>();

    private String currentGroup = null;

    NetexZipFileHierarchy(File filename, NetexParameters netexConfig)
            throws IOException {
        this.zipFile = new ZipFile(filename, ZipFile.OPEN_READ);
        this.config = netexConfig;
        distributeEntries();
    }

    Iterable<ZipEntry> sharedEntries() {
        return sharedEntries;
    }

    Iterable<GroupEntries> groups() {
        return groupEntries.values();
    }

    private void distributeEntries() {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();

            if(ignoredFile(name)) {
                LOG.debug("Netex file ignored: {}.", name);
            }
            else if (isSharedFile(name)) {
                sharedEntries.add(entry);
            }
            else if (isGroupEntry(name, config.sharedGroupFilePattern)) {
                groupEntries.get(currentGroup).addSharedEntry(entry);
            }
            else if (isGroupEntry(name, config.groupFilePattern)) {
                groupEntries.get(currentGroup).addIndependentEntries(entry);
            }
            else {
                LOG.warn("Netex file ignored: {}. The file do not match file patterns.", name);
            }
        }
    }

    private boolean ignoredFile(String name) {
        return config.ignoreFilePattern.matcher(name).matches();
    }

    private boolean isSharedFile(String name) {
        return config.sharedFilePattern.matcher(name).matches();
    }

    private boolean isGroupEntry(String name, Pattern filePattern) {
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

}
