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

import org.opentripplanner.graph_builder.services.osm.OpenStreetMapContentHandler;
import org.opentripplanner.graph_builder.services.osm.OpenStreetMapProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import crosby.binary.file.BlockInputStream;

/**
 * Parser for the OpenStreetMap PBF format. Parses files in two passes:
 * First all the ways and relations are read, then the needed nodes are also loaded.
 *
 * @see http://wiki.openstreetmap.org/wiki/PBF_Format
 * @see org.opentripplanner.graph_builder.services.osm.OpenStreetMapContentHandler#biPhase
 * @since 0.4
 */
public class BinaryFileBasedOpenStreetMapProviderImpl implements OpenStreetMapProvider {

    private File _path;
    
    public void readOSM(OpenStreetMapContentHandler handler) {
        try {
            BinaryOpenStreetMapParser parser = new BinaryOpenStreetMapParser(handler);

            FileInputStream input = new FileInputStream(_path);
            parser.noNodes(true);
            (new BlockInputStream(input, parser)).process();

            handler.secondPhase();

            parser.nodesOnly(true);
            input = new FileInputStream(_path);
            (new BlockInputStream(input, parser)).process();
        } catch (Exception ex) {
            throw new IllegalStateException("error loading OSM from path " + _path, ex);
        }
    }

    public void setPath(File path) {
        _path = path;
    }
    
    public String toString() {
        return "BinaryFileBasedOpenStreetMapProviderImpl(" + _path + ")";
    }
}
