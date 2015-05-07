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

package org.opentripplanner.graph_builder.module.ned;

import com.vividsolutions.jts.geom.Coordinate;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.services.ned.NEDTileSource;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Download one-degree-wide, 1/3 arcsecond NED tiles from S3 (or get them from a directory of files
 * organized as USGS organizes them when you ship them a hard drive).
 *
 * @author novalis
 *
 */
public class DegreeGridNEDTileSource implements NEDTileSource {
    private static Logger log = LoggerFactory.getLogger(DegreeGridNEDTileSource.class);

    private Graph graph;

    private File cacheDirectory;

    public String awsAccessKey;

    public String awsSecretKey;

    public String awsBucketName;

    @Override
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Override
    public void setCacheDirectory(File cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    @Override
    public List<File> getNEDTiles() {

        HashSet<P2<Integer>> tiles = new HashSet<P2<Integer>>();

        for (Vertex v : graph.getVertices()) {
            Coordinate coord = v.getCoordinate();
            tiles.add(new P2<Integer>((int) coord.x, (int) coord.y));
        }

        List<File> paths = new ArrayList<File>();
        for (P2<Integer> tile : tiles) {
            int x = tile.first - 1;
            int y = tile.second + 1;
            paths.add(getPathToTile(x, y));
        }
        return paths;
    }

    private String formatLatLon(int x, int y) {
        String northSouth, eastWest;
        if (y < 0) {
            northSouth = "s";
            y = -y;
        } else {
            northSouth = "n";
        }
        if (x < 0) {
            eastWest = "w";
            x = -x;
        } else {
            eastWest = "e";
        }
        return String.format("%s%d%s%03d", northSouth, y, eastWest, x);
    }

    private File getPathToTile(int x, int y) {
        File path = new File(cacheDirectory, formatLatLon(x, y) + ".tiff");
        if (path.exists()) {
            return path;
        } else {
            path.getParentFile().mkdirs();

            if (awsAccessKey == null || awsSecretKey == null) {
                throw new RuntimeException("Cannot download NED tiles from S3: awsAccessKey or awsSecretKey properties are not set");
            }
            log.info("Downloading NED degree tile " + path);
            // download the file from S3.
            AWSCredentials awsCredentials = new AWSCredentials(awsAccessKey, awsSecretKey);
            try {
                S3Service s3Service = new RestS3Service(awsCredentials);
                String key = formatLatLon(x, y) + ".tiff";
                S3Object object = s3Service.getObject(awsBucketName, key);

                InputStream istream = object.getDataInputStream();
                FileOutputStream ostream = new FileOutputStream(path);

                byte[] buffer = new byte[4096];
                while (true) {
                    int read = istream.read(buffer);
                    if (read == -1) {
                        break;
                    }
                    ostream.write(buffer, 0, read);
                }
                ostream.close();
                istream.close();
            } catch (S3ServiceException e) {
                path.deleteOnExit();
                throw new RuntimeException(e);
            } catch (ServiceException e) {
                path.deleteOnExit();
                throw new RuntimeException(e);
            } catch (FileNotFoundException e) {
                path.deleteOnExit();
                throw new RuntimeException(e);
            } catch (IOException e) {
                path.deleteOnExit();
                throw new RuntimeException(e);
            }
            return path;
        }

    }


}
