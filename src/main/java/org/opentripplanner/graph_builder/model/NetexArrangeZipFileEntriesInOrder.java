/* 
 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/
package org.opentripplanner.graph_builder.model;

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

class NetexArrangeZipFileEntriesInOrder {
    private static final Logger LOG = LoggerFactory
            .getLogger(NetexArrangeZipFileEntriesInOrder.class);

    private final ZipFile zipFile;

    private final NetexParameters config;

    private final List<ZipEntry> sharedEntries = new ArrayList<>();

    private final Map<String, GroupEntries> groupEntries = new TreeMap<>();

    private String currentGroup = null;

    NetexArrangeZipFileEntriesInOrder(File filename, NetexParameters netexConfig)
            throws IOException {
        this.zipFile = new ZipFile(filename, ZipFile.OPEN_READ);
        this.config = netexConfig;
    }

    List<ZipEntry> getEntriesInOrder() {

        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();

            if (isSharedFile(name)) {
                sharedEntries.add(entry);
                continue;
            }
            if (isGroupEntry(name, config.sharedGroupFilePattern)) {
                groupEntries.get(currentGroup).sharedEntries.add(entry);
                continue;
            }
            if (isGroupEntry(name, config.groupFilePattern)) {
                groupEntries.get(currentGroup).entries.add(entry);
                continue;
            }
            LOG.warn("Netex file ignored: {}. The file do not match file patterns.", name);
        }

        return mergeAllEntriesIntoOneList();
    }

    private List<ZipEntry> mergeAllEntriesIntoOneList() {
        List<ZipEntry> allEntries = new ArrayList<>(sharedEntries);

        for (GroupEntries it : groupEntries.values()) {
            allEntries.addAll(it.sharedEntries);
            allEntries.addAll(it.entries);
        }
        return allEntries;
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

    private static class GroupEntries {
        String group;

        List<ZipEntry> sharedEntries = new ArrayList<>();

        List<ZipEntry> entries = new ArrayList<>();

        GroupEntries(String group) {
            this.group = group;
        }
    }
}
