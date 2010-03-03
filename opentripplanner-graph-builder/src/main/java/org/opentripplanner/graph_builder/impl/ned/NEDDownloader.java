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

package org.opentripplanner.graph_builder.impl.ned;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;

import org.opentripplanner.routing.core.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vividsolutions.jts.geom.Envelope;

public class NEDDownloader {

    private static Logger _log = LoggerFactory.getLogger(NEDDownloader.class);

    private Graph graph;

    private File cacheDirectory;

    @Autowired
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public void setCacheDirectory(File cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    private File getPathToNEDArchive(String key) {
        try {
            cacheDirectory.mkdirs();
        } catch (Exception e) {
            throw new RuntimeException("Error creating cache directory " + cacheDirectory, e);
        }

        File path = new File(cacheDirectory, "ned-" + key + ".zip");
        return path;
    }

    private File getPathToNEDTile(String key) {
        try {
            cacheDirectory.mkdirs();
        } catch (Exception e) {
            throw new RuntimeException("Error creating cache directory " + cacheDirectory, e);
        }

        File path = new File(cacheDirectory, "ned-" + key + ".tif");
        return path;
    }

    private OMElement getValidateOMElement() {
        String dataset = "ND302XZ"; // 1/3 arcsecond data. TODO: should try ND902XZ (1/9th
        // arcsecond) and fall back to NED02XZ (1 arcsecond) if
        // necessary
        Envelope extent = graph.getExtent();
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("http://edc/usgs/gov/", "");
        OMElement method = fac.createOMElement("processAOI", omNs);
        OMElement value = fac.createOMElement("requestInfoXml", omNs);
        String xmlRequestString = "<REQUEST_SERVICE_INPUT>" + "<AOI_GEOMETRY>" + "<EXTENT>"
                + "<TOP>"
                + extent.getMaxY()
                + "</TOP>"
                + "<BOTTOM>"
                + extent.getMinY()
                + "</BOTTOM>"
                + "<LEFT>"
                + extent.getMinX()
                + "</LEFT>"
                + "<RIGHT>"
                + extent.getMaxX()
                + "</RIGHT>"
                + "</EXTENT>"
                + "<SPATIALREFERENCE_WKID/>"
                + "</AOI_GEOMETRY>"
                + "<LAYER_INFORMATION>"
                + "     <LAYER_IDS>"
                + dataset
                + "</LAYER_IDS>"
                + "</LAYER_INFORMATION>"
                + "<CHUNK_SIZE>250"
                + "</CHUNK_SIZE>"
                + "<ORIGINATOR/>"
                + "</REQUEST_SERVICE_INPUT>";

        value.addChild(fac.createOMText(value, xmlRequestString));

        method.addChild(value);

        return method;
    }

    @SuppressWarnings("unchecked")
    public List<URL> getDownloadURLs() {
        try {
            OMElement payload = getValidateOMElement();
            Options options = new Options();
            EndpointReference targetEPR = new EndpointReference(
                    "http://extract.cr.usgs.gov/requestValidationService/services/RequestValidationService");
            options.setTo(targetEPR);
            options.setAction("processAOI");

            ServiceClient sender = new ServiceClient();
            sender.setOptions(options);

            _log.debug("Getting urls from request validation service");
            // the return document contains an XML document ...
            OMElement result = sender.sendReceive(payload);

            AXIOMXPath xpathExpression = new AXIOMXPath(result, "//ns1:processAOIReturn");
            xpathExpression.addNamespace("ns1", "http://edc.usgs.gov");
            OMElement holder = (OMElement) xpathExpression.selectSingleNode(result);

            // ... that contains an XML document
            String xml = holder.getText();

            // ... which seems to get prematurely decoded
            xml = xml.replace("&", "&amp;");
            XMLStreamReader xmlStreamReader = XMLInputFactory.newInstance().createXMLStreamReader(
                    new StringReader(xml));

            StAXOMBuilder stAXOMBuilder = new StAXOMBuilder(xmlStreamReader);
            xpathExpression = new AXIOMXPath(result, "//PIECE/DOWNLOAD_URL");
            List<OMElement> nodes = xpathExpression.selectNodes(stAXOMBuilder.getDocumentElement());

            // and which finally contains a list of URLs that we can pass on to the next step.
            List<URL> urls = new ArrayList<URL>();
            for (OMElement urlElement : nodes) {
                String urlString = urlElement.getText().trim();
                _log.debug("Adding NED URL:" + urlString);
                URL url = new URL(urlString);
                urls.add(url);
            }
            return urls;
        } catch (Exception e) {
            throw new RuntimeException("Error getting data from USGS Request Validation Server", e);
        }

    }

    private String initiateDownload(URL url) {
        try {
            _log.debug("Trying to initiate download: " + url);
            OMElement doc = getXMLFromURL(url);

            AXIOMXPath xpathExpression = new AXIOMXPath(doc, "//ns:return");
            xpathExpression.addNamespace("ns1", "http://edc/usgs/gov");
            OMElement tokenElement = (OMElement) xpathExpression.selectSingleNode(doc);
            String token = tokenElement.getText();
            _log.debug("Initiated download; got token: " + token);
            return token;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error getting data from USGS Download Server while initiating downloads for url "
                            + url, e);
        }
    }

    public static OMElement getXMLFromURL(URL url) {
        return getXMLFromURL(url, false);
    }

    public static OMElement getXMLFromURL(URL url, boolean htmlOK) {
        String contents = null;
        try {
            URLConnection connection = url.openConnection();
            InputStream stream = connection.getInputStream();

            InputStreamReader reader = new InputStreamReader(new BufferedInputStream(stream));
            char[] buffer = new char[4096];
            StringBuffer sb = new StringBuffer();
            while (true) {
                int bytesRead = reader.read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                sb.append(buffer, 0, bytesRead);
            }
            contents = sb.toString();
            HttpURLConnection httpconnection = (HttpURLConnection) connection;
            httpconnection.disconnect();
            if (contents.startsWith("<HTML>")) {
                if (htmlOK) {
                    return null;
                }
                throw new RuntimeException(
                        "Error getting data from USGS Download Server -- they sent us HTML when we wanted XML.  Here's the HTML they sent, for what it's worth: \n"
                                + contents);
            }
            XMLStreamReader xmlStreamReader = XMLInputFactory.newInstance().createXMLStreamReader(
                    new StringReader(contents));

            StAXOMBuilder stAXOMBuilder = new StAXOMBuilder(xmlStreamReader);
            OMElement doc = stAXOMBuilder.getDocumentElement();
            return doc;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error getting data from USGS Download Server while checking download status: contents = \n"
                            + contents, e);
        }
    }

    private String getKey(URL url) {
        String lft = null;
        String rgt = null;
        String top = null;
        String bot = null;

        String query = url.getQuery();
        for (String param : query.split("&")) {
            String[] parts = param.split("=");

            if (parts[0].equals("lft")) {
                lft = parts[1];
            } else if (parts[0].equals("rgt")) {
                rgt = parts[1];
            } else if (parts[0].equals("top")) {
                top = parts[1];
            } else if (parts[0].equals("bot")) {
                bot = parts[1];
            }
        }

        return lft + "_" + rgt + "_" + top + "_" + bot;
    }

    public List<File> downloadNED() {
        _log.debug("Downloading NED");
        List<URL> urls = getDownloadURLs();
        List<File> files = new ArrayList<File>();
        for (URL url : urls) {
            String key = getKey(url);
            File tile = getPathToNEDTile(key);
            if (tile.exists()) {
                files.add(tile);
                _log.debug(url + " already exists; not downloading");
                continue;
            }
            sleep(10000);
            String token = initiateDownload(url);
            do {
                _log.debug("Waiting to query");
                sleep(30000);
            } while (!downloadReady(token));
            sleep(10000);
            try {
                downloadFile(url, token);
                files.add(unzipFile(url));
            } catch (Exception e) {
                throw new RuntimeException(
                        "Error getting data from USGS Download Server while downloading", e);
            }
        }
        return files;
    }

    private File unzipFile(URL url) {
        // Unzip Geotiff out of zip file
        String key = getKey(url);
        File path = getPathToNEDArchive(key);
        try {
            ZipFile zipFile = new ZipFile(path);
            for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements();) {
                ZipEntry entry = e.nextElement();

                if (entry.getName().endsWith(".tif")) {
                    InputStream istream = zipFile.getInputStream(entry);
                    File tile = getPathToNEDTile(key);
                    FileOutputStream ostream = new FileOutputStream(tile);
                    byte[] buffer = new byte[4096];
                    while (true) {
                        int bytesRead = istream.read(buffer);
                        if (bytesRead == -1) {
                            break;
                        }
                        ostream.write(buffer, 0, bytesRead);
                    }
                    return tile;
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error extracting geotiff from zip " + path, e);
        }

        throw new RuntimeException("Error extracting geotiff from zip: nothing ends in .tif "
                + path);
    }

    private void downloadFile(URL url, String token) {
        try {
            String key = getKey(url);
            _log.debug("Starting download " + key);
            File path = getPathToNEDArchive(key);
            URL downloadUrl = new URL(
                    "http://extract.cr.usgs.gov/axis2/services/DownloadService/getData?downloadID="
                            + token);
            URLConnection connection = downloadUrl.openConnection();
            HttpURLConnection httpconnection = (HttpURLConnection) connection;
            InputStream istream = connection.getInputStream();
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
            httpconnection.disconnect();
            _log.debug("Done download " + key);
            NEDDownloader.sleep(10000);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error getting data from USGS Download Server while downloading", e);
        }
        try {
            URL cleanupURL = new URL(
                    "http://extract.cr.usgs.gov/axis2/services/DownloadService/setDownloadComplete?downloadID="
                            + token);
            cleanupURL.openStream().close();
        } catch (Exception e) {
            _log.debug("Error getting data from USGS Download Server while cleaning up", e);
        }
    }

    /*
     * We periodically need to pause to keep from overloading the USGS's servers. This is not just a
     * matter of politeness -- they'll give weird errors if we don't.
     */
    private static void sleep(int millis) {
        long now = System.currentTimeMillis();
        while (System.currentTimeMillis() - now < millis) {
            long remaining = millis - (System.currentTimeMillis() - now);
            try {
                Thread.sleep(remaining);
            } catch (InterruptedException e) {
                // it's all good
            }
        }
    }

    private boolean downloadReady(String token) {
        try {
            String url = "http://extract.cr.usgs.gov/axis2/services/DownloadService/getDownloadStatus?downloadID="
                    + token;
            OMElement doc = getXMLFromURL(new URL(url), true);
            if (doc == null) {
                return false;
            }
            AXIOMXPath xpathExpression = new AXIOMXPath(doc, "//ns:return");
            xpathExpression.addNamespace("ns1", "http://edc/usgs/gov");
            OMElement tokenElement = (OMElement) xpathExpression.selectSingleNode(doc);
            String status = tokenElement.getText();
            int end = status.indexOf(",");
            if (end == -1) {
                _log.warn("bogus status " + status + " for token " + token);
                return false;
            }
            int statusCode = Integer.parseInt(status.substring(0, end));
            if (statusCode >= 400) {
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error getting data from USGS Download Server while downloading", e);
        }

    }
}
