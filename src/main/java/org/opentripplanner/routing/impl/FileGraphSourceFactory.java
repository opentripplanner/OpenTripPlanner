/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.opentripplanner.routing.services.GraphSource;
import org.opentripplanner.routing.services.GraphSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

/**
 * A GraphSource factory creating FileGraphSource.
 * 
 * @see FileGraphSource
 */
public class FileGraphSourceFactory implements GraphSourceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(FileGraphSourceFactory.class);

    public File basePath = new File("/var/otp/graphs");

    public LoadLevel loadLevel = LoadLevel.FULL;

    @Override
    public GraphSource createGraphSource(String routerId) {
        return InputStreamGraphSource
                .newFileGraphSource(routerId, getBasePath(routerId), loadLevel);
    }

    @Override
    public boolean save(String routerId, InputStream is) {

        File sourceFile = new File(getBasePath(routerId), InputStreamGraphSource.GRAPH_FILENAME);

        try {

            // Create directory if necessary
            File directory = new File(sourceFile.getParentFile().getPath());
            if (!directory.exists()) {
                directory.mkdir();
            }

            // Store the stream to disk, to be sure no data will be lost make a temporary backup
            // file of the original file.

            // Make backup file
            File destFile = null;
            if (sourceFile.exists()) {
                destFile = new File(sourceFile.getPath() + ".bak");
                if (destFile.exists()) {
                    destFile.delete();
                }
                sourceFile.renameTo(destFile);
            }

            // Store the stream
            FileOutputStream os = new FileOutputStream(sourceFile);
            ByteStreams.copy(is, os);

            // And delete the backup file
            sourceFile = new File(sourceFile.getPath() + ".bak");
            if (sourceFile.exists()) {
                sourceFile.delete();
            }

        } catch (Exception ex) {
            LOG.error("Exception while storing graph to {}.", sourceFile.getPath());
            ex.printStackTrace();
            return false;
        }

        return true;
    }

    private File getBasePath(String routerId) {
        return new File(basePath, routerId);
    }
}
