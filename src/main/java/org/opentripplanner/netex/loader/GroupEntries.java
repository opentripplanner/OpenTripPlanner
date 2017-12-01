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
