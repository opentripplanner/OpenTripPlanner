/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.model;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipFile;

import org.onebusaway.csv_entities.CsvInputSource;
import org.onebusaway.csv_entities.ZipFileCsvInputSource;
import org.opentripplanner.graph_builder.impl.DownloadableGtfsInputSource;

public class GtfsBundle {

    private File path;

    private URL url;

    private String defaultAgencyId;

    private CsvInputSource csvInputSource;

    private Boolean defaultBikesAllowed = false;

    private Map<String, String> agencyIdMappings = new HashMap<String, String>();

    public void setPath(File path) {
        this.path = path;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public void setCsvInputSource(CsvInputSource csvInputSource) {
        this.csvInputSource = csvInputSource;
    }
    
    public String getDataKey() {
        return path + ";" + url + ";" + (csvInputSource != null ? csvInputSource.hashCode() : "");
    }
    
    public CsvInputSource getCsvInputSource() throws IOException {
        if (csvInputSource == null) {
            if (path != null) {
                csvInputSource = new ZipFileCsvInputSource(new ZipFile(path));
            } else if (url != null) {
            	DownloadableGtfsInputSource isrc = new DownloadableGtfsInputSource();
            	isrc.setUrl(url);
                csvInputSource = isrc;
            }
    	}
        return csvInputSource;
    }

    public String toString () {
        String src; 
        if (path != null) {
            src = path.toString();
        } else if (url != null) {
            src = url.toString();
        } else {
            src = "(no source)";
        }
        return "GTFS bundle at " + src;
    }
    
    /**
     * So that you can load multiple gtfs feeds into the same database / system without entity id
     * collisions, everything has an agency id, including entities like stops, shapes, and service
     * ids that don't explicitly have an agency id (as opposed to routes + trips + stop times).
     * However, the spec doesn't currently have a method to specify which agency a stop
     * should be assigned to in the case of multiple agencies being specified in the same feed.  
     * Routes (and thus everything belonging to them) do have an agency id, but stops don't.
     * The defaultAgencyId allows you to define which agency will be used as the default
     * when figuring out which agency a stop should be assigned to (also applies to shapes + service
     * ids as well). If not specified, the first agency in the agency list will be used.
     */
    public String getDefaultAgencyId() {
        return defaultAgencyId;
    }

    public void setDefaultAgencyId(String defaultAgencyId) {
        this.defaultAgencyId = defaultAgencyId;
    }

    public Map<String, String> getAgencyIdMappings() {
        return agencyIdMappings;
    }

    public void setAgencyIdMappings(Map<String, String> agencyIdMappings) {
        this.agencyIdMappings = agencyIdMappings;
    }

    /**
     * When a trip doesn't contain any bicycle accessibility information, should taking a bike
     * along a transit trip be permitted?
     * A trip doesn't contain bicycle accessibility information if both route_short_name and
     * trip_short_name contain missing/0 values.
     */
    public Boolean getDefaultBikesAllowed() {
        return defaultBikesAllowed;
    }

    public void setDefaultBikesAllowed(Boolean defaultBikesAllowed) {
        this.defaultBikesAllowed = defaultBikesAllowed;
    }
}
