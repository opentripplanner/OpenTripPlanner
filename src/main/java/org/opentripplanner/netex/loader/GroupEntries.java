package org.opentripplanner.netex.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A named group of NeTEx file entries. The entries are grouped together
 * with a set of shared group entries and a set of _individual_
 * entries.
 */
class GroupEntries {
    private final String name;
    private final List<FileEntry> sharedEntries = new ArrayList<>();
    private final List<FileEntry> independentEntries = new ArrayList<>();

    GroupEntries(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    void addSharedEntry(FileEntry entry) {
        sharedEntries.add(entry);
    }

    Collection<FileEntry> getSharedEntries() {
        return sharedEntries;
    }

    void addIndependentEntries(FileEntry entry) {
        independentEntries.add(entry);
    }

    Collection<FileEntry> getIndependentEntries() {
        return independentEntries;
    }
}
