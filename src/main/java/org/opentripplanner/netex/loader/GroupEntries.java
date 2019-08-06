package org.opentripplanner.netex.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;

/**
 * A named collection of NeTEx files grouped together with
 * a set of shared group entries/files and a set of _individual_
 * entries/files.
 */
class GroupEntries {
    private final String name;
    private final List<ZipEntry> sharedEntries = new ArrayList<>();
    private final List<ZipEntry> entries = new ArrayList<>();

    GroupEntries(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    void addSharedEntry(ZipEntry entry) {
        sharedEntries.add(entry);
    }

    Collection<ZipEntry> sharedEntries() {
        return sharedEntries;
    }

    void addIndependentEntries(ZipEntry entry) {
        entries.add(entry);
    }

    Collection<ZipEntry> independentEntries() {
        return entries;
    }
}
