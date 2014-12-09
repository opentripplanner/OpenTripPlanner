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

package org.opentripplanner.openstreetmap.impl;

import org.opentripplanner.openstreetmap.services.OpenStreetMapContentHandler;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;

import java.io.File;
import java.io.FileInputStream;

import crosby.binary.file.BlockInputStream;

/**
 * Parser for the OpenStreetMap PBF format. Parses files in three passes:
 * First the relations, then the ways, then the nodes are also loaded.
 *
 * @see http://wiki.openstreetmap.org/wiki/PBF_Format
 * @see org.opentripplanner.openstreetmap.services.graph_builder.services.osm.OpenStreetMapContentHandler#biPhase
 * @since 0.4
 */
public class BinaryFileBasedOpenStreetMapProviderImpl implements OpenStreetMapProvider {

    private File _path;

    public void readOSM(OpenStreetMapContentHandler handler) {
        try {
            BinaryOpenStreetMapParser parser = new BinaryOpenStreetMapParser(handler);

            FileInputStream input = new FileInputStream(_path);
            parser.setParseNodes(false);
            parser.setParseWays(false);
            (new BlockInputStream(input, parser)).process();
            handler.doneFirstPhaseRelations();

            input = new FileInputStream(_path);
            parser.setParseRelations(false);
            parser.setParseWays(true);
            (new BlockInputStream(input, parser)).process();
            handler.doneSecondPhaseWays();

            input = new FileInputStream(_path);
            parser.setParseNodes(true);
            parser.setParseWays(false);
            (new BlockInputStream(input, parser)).process();
            handler.doneThirdPhaseNodes();
        } catch (Exception ex) {
            throw new IllegalStateException("error loading OSM from path " + _path, ex);        }
    }

    public void setPath(File path) {
        _path = path;
    }

    public String toString() {
        return "BinaryFileBasedOpenStreetMapProviderImpl(" + _path + ")";
    }

    @Override
    public void checkInputs() {
        if (!_path.canRead()) {
            throw new RuntimeException("Can't read OSM path: " + _path);
        }
    }
}
