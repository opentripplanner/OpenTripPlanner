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

package org.opentripplanner.graph_builder.impl.osm;

import java.io.File;

import org.opentripplanner.graph_builder.services.osm.OpenStreetMapContentHandler;
import org.opentripplanner.graph_builder.services.osm.OpenStreetMapProvider;

public class FileBasedOpenStreetMapProviderImpl implements OpenStreetMapProvider {

    private File _path;

    public void setPath(File path) {
        _path = path;
    }

    @Override
    public void readOSM(OpenStreetMapContentHandler handler) {
        try {
            OpenStreetMapParser parser = new OpenStreetMapParser();
            parser.parseMap(_path, handler);
        } catch (Exception ex) {
            throw new IllegalStateException("error loading OSM from path " + _path, ex);
        }
    }
}
