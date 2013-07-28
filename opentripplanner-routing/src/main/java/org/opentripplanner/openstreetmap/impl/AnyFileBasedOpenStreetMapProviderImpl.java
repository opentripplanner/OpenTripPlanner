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

import java.io.File;

import org.opentripplanner.openstreetmap.services.OpenStreetMapContentHandler;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;

public class AnyFileBasedOpenStreetMapProviderImpl implements OpenStreetMapProvider {

    private File _path;

    public void setPath(File path) {
        _path = path;
    }

    public AnyFileBasedOpenStreetMapProviderImpl (File file) {
        this.setPath(file);
    }
    
    public AnyFileBasedOpenStreetMapProviderImpl() { };

    @Override
    public void readOSM(OpenStreetMapContentHandler handler) {
        try {
            if (_path.getName().endsWith(".pbf")) {
                BinaryFileBasedOpenStreetMapProviderImpl p = new BinaryFileBasedOpenStreetMapProviderImpl();
                p.setPath(_path);
                p.readOSM(handler);
            } else {
                StreamedFileBasedOpenStreetMapProviderImpl p = new StreamedFileBasedOpenStreetMapProviderImpl();
                p.setPath(_path);
                p.readOSM(handler);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("error loading OSM from path " + _path, ex);
        }
    }

    public String toString() {
        return "AnyFileBasedOpenStreetMapProviderImpl(" + _path + ")";
    }

    @Override
    public void checkInputs() {
        if (!_path.canRead()) {
            throw new RuntimeException("Can't read OSM path: " + _path);
        }
    }
}
