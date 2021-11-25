package org.opentripplanner.datastore;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.datastore.configure.DataStoreFactory;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.opentripplanner.standalone.configure.OTPConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.datastore.FileType.CONFIG;
import static org.opentripplanner.datastore.FileType.DEM;
import static org.opentripplanner.datastore.FileType.GRAPH;
import static org.opentripplanner.datastore.FileType.GTFS;
import static org.opentripplanner.datastore.FileType.NETEX;
import static org.opentripplanner.datastore.FileType.OSM;
import static org.opentripplanner.datastore.FileType.REPORT;
import static org.opentripplanner.datastore.FileType.UNKNOWN;
import static org.opentripplanner.standalone.config.CommandLineParameters.createCliForTest;

public class OtpDataStoreTest {

    private static final String BUILD_CONFIG_FILENAME = "build-config.json";
    private static final String ROUTER_CONFIG_FILENAME = "router-config.json";
    private static final String OSM_FILENAME = "osm.pbf";
    private static final String DEM_FILENAME = "dem.tif";
    private static final String NETEX_FILENAME = "netex.zip";
    private static final String GTFS_FILENAME = "gtfs.zip";
    private static final String GRAPH_FILENAME = "graph.obj";
    private static final String STREET_GRAPH_FILENAME = "streetGraph.obj";
    private static final String REPORT_FILENAME = "report";
    private static final String UTF_8 = "UTF-8";
    private static final long D2000_01_01 = ZonedDateTime.parse("2000-01-01T12:00+01:00")
            .toInstant().toEpochMilli();

    private File baseDir;

    @Before
    public void setUp() throws IOException {
        // Create a base path for this test - correspond to OTP BASE_PATH
        baseDir = Files.createTempDirectory("OtpDataStoreTest-").toFile();
    }

    @After
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void tearDown() {
        baseDir.delete();
    }

    @Test
    public void readEmptyDir() {

        OtpDataStore store = new DataStoreFactory(config()).open();
        assertNoneExistingFile(store.getGraph(), GRAPH_FILENAME, GRAPH);
        assertNoneExistingFile(store.getStreetGraph(), STREET_GRAPH_FILENAME, GRAPH);
        assertNoneExistingFile(store.getBuildReportDir(), REPORT_FILENAME, REPORT);

        assertEquals("[]", store.listExistingSourcesFor(CONFIG).toString());
        assertEquals("[]", store.listExistingSourcesFor(OSM).toString());
        assertEquals("[]", store.listExistingSourcesFor(DEM).toString());
        assertEquals("[]", store.listExistingSourcesFor(GTFS).toString());
        assertEquals("[]", store.listExistingSourcesFor(NETEX).toString());
        assertEquals("[]", store.listExistingSourcesFor(GRAPH).toString());
        assertEquals("[]", store.listExistingSourcesFor(REPORT).toString());
        assertEquals("[]", store.listExistingSourcesFor(UNKNOWN).toString());
    }

    @Test
    public void readDirWithEverything() throws IOException {
        write(baseDir, BUILD_CONFIG_FILENAME, "{}");
        write(baseDir, ROUTER_CONFIG_FILENAME, "{}");
        write(baseDir, OSM_FILENAME, "Data");
        write(baseDir, DEM_FILENAME, "Data");
        writeZip(baseDir, GTFS_FILENAME);
        writeZip(baseDir, NETEX_FILENAME);
        write(baseDir, STREET_GRAPH_FILENAME, "Data");
        write(baseDir, GRAPH_FILENAME, "Data");
        writeToDir(baseDir, REPORT_FILENAME, "index.json");

        OtpDataStore store = new DataStoreFactory(config()).open();
        assertExistingSource(store.getGraph(), GRAPH_FILENAME, GRAPH);
        assertExistingSource(store.getStreetGraph(), STREET_GRAPH_FILENAME, GRAPH);
        assertReportExist(store.getBuildReportDir());

        assertExistingSources(store.listExistingSourcesFor(OSM), OSM_FILENAME);
        assertExistingSources(store.listExistingSourcesFor(DEM), DEM_FILENAME);
        assertExistingSources(store.listExistingSourcesFor(GTFS), GTFS_FILENAME);
        assertExistingSources(store.listExistingSourcesFor(NETEX), NETEX_FILENAME);
        assertExistingSources(
                store.listExistingSourcesFor(CONFIG),
                BUILD_CONFIG_FILENAME,
                ROUTER_CONFIG_FILENAME
        );
        assertExistingSources(
                store.listExistingSourcesFor(GRAPH),
                STREET_GRAPH_FILENAME,
                GRAPH_FILENAME
        );
    }

    @Test
    public void testResolvingFileUris() throws IOException {
        // Given a temp data directory to dump files in, must be different from tempDir
        File tempDataDir = Files.createTempDirectory("ODST-2-").toFile();

        // Get the uri for the temp data dir to insert into config file
        String uri = tempDataDir.toURI().toString();
        // Make sure it ends with a slash '/'
        if(!uri.endsWith("/")) { uri += "/"; }

        // Insert a URI for osm, gtfs, graph and report data sources
        String buildConfigJson = String.format(
                "{"
                + "%n  storage: {"
                + "%n      osm: ['%s'],"
                + "%n      gtfs: ['%s'],"
                + "%n      graph: '%s',"
                + "%n      buildReportDir: '%s'"
                + "%n  }"
                + "%n}",
                uri + OSM_FILENAME,
                uri + GTFS_FILENAME,
                uri + GRAPH_FILENAME,
                uri + REPORT_FILENAME
        ).replace('\'', '\"');

        // Create build-config  and a unknown file in the 'baseDir'
        write(baseDir, BUILD_CONFIG_FILENAME, buildConfigJson);
        write(baseDir, "unknown.txt", "Data");

        // Save osm, gtfs, graph, and report in 'tempDataDir'- the URI location,
        write(tempDataDir, OSM_FILENAME, "Data");
        writeZip(tempDataDir, GTFS_FILENAME);
        write(tempDataDir, GRAPH_FILENAME, "Data");
        writeToDir(tempDataDir, REPORT_FILENAME, "index.json");

        // We add 2 more files, these are not configured in the build-config, and we expect
        // them to be invisible to the store; hence we won´t find them (saved to 'tempDataDir')
        write(tempDataDir, STREET_GRAPH_FILENAME, "Data");
        write(tempDataDir, "unknown-2.txt", "Data");

        // Open data store using the base-dir
        OtpDataStore store = new DataStoreFactory(
                new OTPConfiguration(createCliForTest(baseDir)).createDataStoreConfig()
        ).open();

        // Collect result and prepare it for assertion
        List<String> filenames = listFilesByRelativeName(store, baseDir, tempDataDir);
        filenames.sort(String::compareTo);
        String result = String.join(", ", filenames);

        // We expect to find all files set in the build-config (URIs) and
        // the ALL files added in the baseDir, but not the base-graph and unknown file
        // added to the same temp-data-dir.
        assertEquals(
                "CONFIG base:build-config.json, "
                + "GRAPH data:graph.obj, "
                + "GTFS data:gtfs.zip, "
                + "OSM data:osm.pbf, "
                + "REPORT data:report, "
                + "UNKNOWN base:unknown.txt",
                result
        );
    }

    /* private helper methods */

    private List<String> listFilesByRelativeName(OtpDataStore store, File baseDir, File dataDir) {
        String baseDirPath = baseDir.getPath();
        String dataDirPath = dataDir.getPath();

        List<String> files = new ArrayList<>();
        for (FileType type : FileType.values()) {
            store.listExistingSourcesFor(type).forEach(
                    s -> {
                        String p = s.path();

                        if(p.startsWith(baseDirPath)) {
                            // Add 1 to strip off path separator
                            p = "base:" + p.substring(baseDirPath.length() + 1);
                        }
                        else if(p.startsWith(dataDirPath)) {
                            p = "data:" + p.substring(dataDirPath.length() + 1);
                        }
                        files.add(type.name() + " " + p);
                    }
            );
        }
        return files;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void writeToDir(File parentDir, String dir, String oneFile) throws IOException {
        File reportDir = new File(parentDir, dir);
        reportDir.mkdirs();
        FileUtils.write(new File(reportDir, oneFile), "{}", UTF_8);
    }

    private static void write(File dir, String filename, String data) throws IOException {
        FileUtils.write(new File(dir, filename), data, OtpDataStoreTest.UTF_8);
    }

    private static void writeZip(File dir, String filename) throws IOException {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(new File(dir, filename)));
        ZipEntry e = new ZipEntry("stop.txt");
        out.putNextEntry(e);
        out.write("data".getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
        out.finish();
        out.close();
    }

    private static void assertNoneExistingFile(DataSource source, String name, FileType type) {
        assertEquals(type, source.type());
        assertEquals(name, source.name());
        assertFalse(source.exists());
    }

    private static void assertExistingSource(DataSource source, String name, FileType type) {
        assertEquals(type, source.type());
        assertEquals(name, source.name());
        assertTrue(source.exists());
        assertTrue("Last modified: " + source.lastModified(), source.lastModified() > D2000_01_01);
    }

    private static void assertExistingSources(Collection<DataSource> sources, String ... names) {
        assertEquals("Size of: " + sources, names.length, sources.size());
        List<String> nameList = Arrays.asList(names);

        for (DataSource source : sources) {
            assertTrue(source.name(), nameList.contains(source.name()));
        }
    }

    private static void assertReportExist(CompositeDataSource report) {
        assertEquals(REPORT, report.type());
        assertEquals(REPORT_FILENAME, report.name());
        assertTrue(report.exists());
        assertTrue(report.isWritable());
        assertEquals(report.content().toString(), 1, report.content().size());
    }

    private OtpDataStoreConfig config() {
        CommandLineParameters cli = CommandLineParameters.createCliForTest(baseDir);
        return new OTPConfiguration(cli).createDataStoreConfig();
    }
}