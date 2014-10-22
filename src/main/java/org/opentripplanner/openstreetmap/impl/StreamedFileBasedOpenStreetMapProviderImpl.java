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
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * @author Vincent Privat
 * @since 1.0
 */
public class StreamedFileBasedOpenStreetMapProviderImpl implements OpenStreetMapProvider {

    private File _path;

    /* (non-Javadoc)
     * @see org.opentripplanner.graph_builder.services.osm.OpenStreetMapProvider#readOSM(org.opentripplanner.graph_builder.services.osm.OpenStreetMapContentHandler)
     */
    @Override
    public void readOSM(OpenStreetMapContentHandler handler) {
        try {
            if (_path.getName().endsWith(".gz")) {
                InputStream in = new GZIPInputStream(new FileInputStream(_path));
                StreamedOpenStreetMapParser.parseMap(in, handler, 1);
                handler.doneFirstPhaseRelations();

                in = new GZIPInputStream(new FileInputStream(_path));
                StreamedOpenStreetMapParser.parseMap(in, handler, 2);
                handler.doneSecondPhaseWays();

                in = new GZIPInputStream(new FileInputStream(_path));
                StreamedOpenStreetMapParser.parseMap(in, handler, 3);
                handler.doneThirdPhaseNodes();

            } else if (_path.getName().endsWith(".bz2")) {
                InputStream in = new BZip2CompressorInputStream(new FileInputStream(_path));
                StreamedOpenStreetMapParser.parseMap(in, handler, 1);
                handler.doneFirstPhaseRelations();

                in = new BZip2CompressorInputStream(new FileInputStream(_path));
                StreamedOpenStreetMapParser.parseMap(in, handler, 2);
                handler.doneSecondPhaseWays();

                in = new BZip2CompressorInputStream(new FileInputStream(_path));
                StreamedOpenStreetMapParser.parseMap(in, handler, 3);
                handler.doneThirdPhaseNodes();

            } else {
                StreamedOpenStreetMapParser.parseMap(_path, handler);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("error loading OSM from path " + _path, ex);
        }
    }

    public void setPath(File path) {
        _path = path;
    }

    public String toString() {
        return "StreamedFileBasedOpenStreetMapProviderImpl(" + _path + ")";
    }

    @Override
    public void checkInputs() {
        if (!_path.canRead()) {
            throw new RuntimeException("Can't read OSM path: " + _path);
        }
    }
}
