package org.opentripplanner.graph_builder.module.ned;

import org.locationtech.jts.geom.Envelope;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.opentripplanner.graph_builder.services.ned.NEDTileSource;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Downloads tiles from the National Elevation Dataset. 
 * @author novalis
 *
 */
public class NEDDownloader implements NEDTileSource {

    private static Logger log = LoggerFactory.getLogger(NEDDownloader.class);

    private Graph graph;

    private File cacheDirectory;

    static String dataset = "ND302XZ"; // 1/3 arcsecond data.
    
    private double _latYStep = 0.16;

    private double _lonXStep = 0.16;

    @Override
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Override
    public void setCacheDirectory(File cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    private File getPathToNEDArchive(String key) {
        if (!cacheDirectory.exists()) {
            if (!cacheDirectory.mkdirs()) {
                throw new RuntimeException("Failed to create cache directory for NED at " + cacheDirectory);
            }
        }

        File path = new File(cacheDirectory, "ned-" + key + ".zip");
        return path;
    }

    private File getPathToNEDTile(String key) {
        if (!cacheDirectory.exists()) {
            if (!cacheDirectory.mkdirs()) {
                throw new RuntimeException("Failed to create cache directory for NED at " + cacheDirectory);
            }
        }

        File path = new File(cacheDirectory, "ned-" + key + ".tif");
        return path;
    }

    public static double floor(double value, double step) {
        return step * Math.floor(value / step);
    }

    public static double ceil(double value, double step) {
        return step * Math.ceil(value / step);
    }

    private List<String> getValidateElements() {
        Envelope extent = graph.getExtent();

        List<String> elements = new ArrayList<String>();

        double minY = floor(extent.getMinY(), _latYStep);
        double maxY = ceil(extent.getMaxY(), _latYStep);
        double minX = floor(extent.getMinX(), _lonXStep);
        double maxX = ceil(extent.getMaxX(), _lonXStep);

        for (double y = minY; y < maxY; y += _latYStep) {
            for (double x = minX; x < maxX; x += _lonXStep) {
                Envelope region = new Envelope(x, x + _lonXStep, y, y + _latYStep);

                String xmlRequestString = "<REQUEST_SERVICE_INPUT>" + "<AOI_GEOMETRY>" + "<EXTENT>"
                        + "<TOP>"
                        + region.getMaxY()
                        + "</TOP>"
                        + "<BOTTOM>"
                        + region.getMinY()
                        + "</BOTTOM>"
                        + "<LEFT>"
                        + region.getMinX()
                        + "</LEFT>"
                        + "<RIGHT>"
                        + region.getMaxX()
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
                        + "<ORIGINATOR/>" + "</REQUEST_SERVICE_INPUT>";

                elements.add(xmlRequestString);
            }
        }
        return elements;
    }

    private List<URL> getDownloadURLs() {
        List<URL> urls = new ArrayList<URL>();
        List<String> payloads = getValidateElements();
        log.info("Getting urls from request validation service");
        String RTendpointURL = "http://extract.cr.usgs.gov/requestValidationService/services/RequestValidationService";

        try {
            int nd = 0;
            for (String payload : payloads) {
                // FIXME document why this sleep call exists. Rate limiting?
                sleep(2000);
                Service RTservice = new Service();
                Call RTcall = (Call) RTservice.createCall();

                RTcall.setTargetEndpointAddress(new java.net.URL(RTendpointURL));

                // Service method
                RTcall.setOperationName(new QName("edc.usgs.gov", "processAOI"));

                String response = (String) RTcall.invoke(new Object[] { payload });

                Document doc = stringToDoc(response);
                XPathExpression expr = makeXPathExpression("//ns1:processAOIReturn/text()");
                String xml = expr.evaluate(doc);
                if (!xml.equals("")) {
                    // case where response is wrapped
                    doc = stringToDoc(xml);
                }
                // ... which seems to get prematurely decoded
                // xml = xml.replace("&", "&amp;");

                expr = makeXPathExpression("//PIECE/DOWNLOAD_URL");
                NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                // and which finally contains a list of URLs that we can pass on to the next step.

                // hopefully, this will be a list of one.
                if (nodes.getLength() > 1) {
                    log.debug("One of our NED tiles requires more than one tile from the server.  This is slightly inefficient, and sort of yucky.");
                }
                for (int i = 0; i < nodes.getLength(); ++i) {
                    Node node = nodes.item(i);
                    String urlString = node.getTextContent().trim();
                    log.info("Getting URL {}/{}", ++nd, payloads.size());
                    log.debug("Adding NED URL: " + urlString);
                    // use one specific, less-broken server at usgs
                    urlString = urlString.replaceAll(" ", "+"); // urls returned are broken
                    // sometimes.
                    URL url = new URL(urlString);
                    urls.add(url);
                }
            }
            return urls;
        } catch (Exception e) {
            throw new RuntimeException("Error getting data from USGS Request Validation Server", e);
        }

    }

    private XPathExpression makeXPathExpression(String xpathStr) throws XPathExpressionException {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new EDCNamespaceContext());
        XPathExpression expr = xpath.compile(xpathStr);
        return expr;
    }

    private static Document stringToDoc(String str) throws ParserConfigurationException,
            SAXException, IOException {
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        documentFactory.setNamespaceAware(true);
        DocumentBuilder builder = documentFactory.newDocumentBuilder();
        str = str.replaceAll("&", "&amp;");

        Document doc = builder.parse(new ByteArrayInputStream(str.getBytes("UTF-8")));
        return doc;
    }

    private String initiateDownload(URL url) {
        try {
            log.debug("Trying to initiate download: " + url);
            Document doc = getXMLFromURL(url);
            XPathExpression xPathExpression = makeXPathExpression("//ns:return/text()");
            String token = xPathExpression.evaluate(doc);
            log.debug("Initiated download; got token: " + token);
            return token;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error getting data from USGS Download Server while initiating downloads for url "
                            + url, e);
        }
    }

    private static Document getXMLFromURL(URL url) {
        return getXMLFromURL(url, false);
    }

    private static Document getXMLFromURL(URL url, boolean htmlOK) {
        String contents = null;
        while (true) {
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
                reader.close();
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

                return stringToDoc(contents);
            } catch (IOException e) {
                log.warn("IO error, retrying: " + e);
                sleep(3000);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Error getting data from USGS Download Server while checking download status: contents = \n"
                                + contents, e);
            }
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
                lft = String.format("%.5g", Double.parseDouble(parts[1]));
            } else if (parts[0].equals("rgt")) {
                rgt = String.format("%.5g", Double.parseDouble(parts[1]));
            } else if (parts[0].equals("top")) {
                top = String.format("%.5g", Double.parseDouble(parts[1]));
            } else if (parts[0].equals("bot")) {
                bot = String.format("%.5g", Double.parseDouble(parts[1]));
            }
        }

        return lft + "_" + rgt + "_" + top + "_" + bot;
    }

    @Override
    public List<File> getNEDTiles() {
        log.info("Downloading NED elevation data (or fetching it from local cache).");
        List<URL> urls = getDownloadURLsCached();
        List<File> files = new ArrayList<File>();
        int tileCount = 0;
        TILE: for (URL url : urls) {
            String tileProgress = String.format("Tile %d/%d", ++tileCount, urls.size());
            String key = getKey(url);
            File tile = getPathToNEDTile(key);
            if (tile.exists()) {
                files.add(tile);
                log.debug("{} found in NED cache, not downloading: {}", tileProgress, tile);
                continue;
            }
            REQUEST: for (int req_attempt = 0; req_attempt < 5; ++req_attempt) {
                log.info("{} not in NED cache, requesting download: {}", tileProgress, tile);
                try {
                    sleep(3000);
                    String token = initiateDownload(url);
                    DOWNLOAD: for (int dl_attempt = 0; dl_attempt < 20; ++dl_attempt) {
                        log.info("Waiting to check if tile is ready for download (try {}).", dl_attempt + 1);
                        sleep(30000);
                        if (downloadReady(token)) {
                            downloadFile(url, token);
                            try {
                                files.add(unzipFile(url));
                                continue TILE;
                            } catch (NotAZipFileException e) {
                                log.error("Received corrupted zip file. Retrying with same token.");
                                continue DOWNLOAD;
                            }
                        }
                    }
                    log.info("Giving up on slow download attempt {} and retrying.", req_attempt);
                } catch (NoDownloadIDException e) {
                    log.debug("Download ID unrecognized by server, retrying.");
                    continue REQUEST;
                } catch (Exception e) {
                    throw new RuntimeException("Error getting data from USGS Download Server while downloading", e);
                }
            }
            log.error("Unable to download a NED tile after 5 requests.");
        }
        return files;
    }

    private List<URL> getDownloadURLsCached() {
        Envelope extent = graph.getExtent();
        Formatter formatter = new Formatter();
        String filename = formatter.format("%f,%f-%f,%f.urls", extent.getMinX(), extent.getMinY(),
                extent.getMaxX(), extent.getMaxY()).toString();
        formatter.close();
        try {
            File file = new File(cacheDirectory, filename);
            List<URL> urls;
            if (!file.exists()) {
                return getAndCacheUrls(file);
            }
            // read cached urls
            FileInputStream is = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            urls = new ArrayList<URL>();
            while (true) {
                String line = reader.readLine();
                if (line == null || line.length() == 0) {
                    break;
                }
                urls.add(new URL(line));
            }
            reader.close();
            is.close();
            if (urls.size() == 0) {
                return getAndCacheUrls(file);
            }
            return urls;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<URL> getAndCacheUrls(File file) throws IOException {
        // get urls from validation server and write them to the cache
        List<URL> urls = getDownloadURLs();
        FileOutputStream os = new FileOutputStream(file);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
        for (URL url : urls) {
            writer.write(url.toString());
            writer.write('\n');
        }
        writer.flush();
        writer.close();
        os.close();
        return urls;
    }

    private File unzipFile(URL url) {
        // Unzip Geotiff out of zip file
        String key = getKey(url);
        File path = getPathToNEDArchive(key);
        try {
            FileInputStream inputStream = new FileInputStream(path);
            byte[] header = new byte[2];
            int bytesRead = inputStream.read(header, 0, 2);
            inputStream.close();
            if (bytesRead != 2 || header[0] != 'P' || header[1] != 'K') {
                // not a zip file
                log.warn("not a zip file.");
                if (!path.delete()) {
                    log.error("Failed to delete incomplete file " + path);
                }
                throw new NotAZipFileException();
            }
            ZipFile zipFile = new ZipFile(path);
            for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements();) {
                ZipEntry entry = e.nextElement();

                if (entry.getName().endsWith(".tif")) {
                    InputStream istream = zipFile.getInputStream(entry);
                    File tile = getPathToNEDTile(key);
                    FileOutputStream ostream = new FileOutputStream(tile);
                    byte[] buffer = new byte[4096];
                    while (true) {
                        bytesRead = istream.read(buffer);
                        if (bytesRead == -1) {
                            break;
                        }
                        ostream.write(buffer, 0, bytesRead);
                    }
                    ostream.close();
                    return tile;
                }
            }
        } catch (NotAZipFileException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error extracting geotiff from zip " + path, e);
        }
        throw new RuntimeException("Error extracting geotiff from zip: nothing ends in .tif "
                + path);
    }

    private void downloadFile(URL url, String token) {
        try {
            String key = getKey(url);
            log.info("Starting download " + key);
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
            log.info("Finished download " + key);
            NEDDownloader.sleep(3000);
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
            log.debug("Error getting data from USGS Download Server while cleaning up", e);
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
            Document doc = getXMLFromURL(new URL(url), true);
            if (doc == null) {
                return false;
            }
            XPathExpression xPathExpression = makeXPathExpression("//ns2:return/text()");
            String status = xPathExpression.evaluate(doc);
            int end = status.indexOf(",");
            if (end == -1) {
                if (status.contains("downloadID not found")) {
                    throw new NoDownloadIDException();
                }
                log.warn("bogus status " + status + " for token " + token);
                return false;
            }
            int statusCode = Integer.parseInt(status.substring(0, end));
            if (statusCode >= 400 && statusCode < 500) {
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error getting data from USGS Download Server while downloading", e);
        }

    }
}

/**
 * Some shit that apparently Java can't be arsed to provide for you.
 * 
 * @author novalis
 * 
 */
class EDCNamespaceContext implements NamespaceContext {
    public String getNamespaceURI(String prefix) {
        if (prefix.equals("ns1")) {
            return "http://edc.usgs.gov";
        } else {
            return "http://edc/usgs/gov/xsd";
        }

    }

    public String getPrefix(String namespace) {
        if (namespace.equals("http://edc.usgs.gov")) {
            return "ns1";
        } else {
            return "ns";
        }
    }

    public Iterator<String> getPrefixes(String namespace) {
        return null;
    }
}

class NotAZipFileException extends RuntimeException {
    private static final long serialVersionUID = -3724250760182397153L;
}

class NoDownloadIDException extends RuntimeException {
    private static final long serialVersionUID = -4749381647025119431L;
}