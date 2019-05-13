package org.opentripplanner.netex.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;

class GroupEntries {
    private String group;
    private List<ZipEntry> sharedEntries = new ArrayList<>();
    private List<ZipEntry> entries = new ArrayList<>();

    GroupEntries(String group) {
        this.group = group;
    }

    public String getGroup() {
        return group;
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
