package org.opentripplanner.ext.datastore.gs;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opentripplanner.datastore.CompositeDataSource;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.datastore.FileType;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.ext.datastore.gs.GsHelper.toUri;


/**
 * This is a manual integration test to test the Google Cloud Storage integration. To set up the
 * test you need to provide a service credential file and a bucket name. For all test to pass there
 * also need to be a gtfs.zip file in your store. Edit the {@link #CREDENTIALS_FILE},
 * {@link #BUCKET_NAME} and {@link #GTFS_URI} to make the test run.
 *
 * This test comed in handy, not only to verify the implementation, but also to verify that your
 * credentials have the proper rights. If the test run, then OTP should also run with the same
 * credentials.
 */
@Ignore("This test is a manual integration test, because it require an Google Cloud Store to run.")
public class GsIntegrationTest {
    private static final String CREDENTIALS_FILE = "<Insert path to local Google Service Credential file here>";
    private static final String BUCKET_NAME = "<Insert bucket name here>";
    private static final URI GTFS_URI = toUri(BUCKET_NAME, "gtfs.zip");
    private static final String DATA = "{ \"key\" : \"data\" }";

    private GsDataSourceRepository repo;

    @Before
    public void setUp() {
        // Open a repository
        repo = new GsDataSourceRepository(CREDENTIALS_FILE);
        repo.open();
    }

    @Test
    public void testGsDirectory() throws Exception {
        // Get a virtual directory
        URI dirUri = toUri(BUCKET_NAME, "my-test-dir");
        CompositeDataSource dir = repo.findCompositeSource(dirUri, FileType.REPORT);

        assertEquals(dirUri.toString(), dir.path());
        assertEquals(FileType.REPORT, dir.type());
        assertEquals("my-test-dir", dir.name());
        assertTrue(dir.isWritable());

        Collection<DataSource> content;

        // Add at least one file
        writeDataToDataSource(dir.entry("a.txt"));

        // Assert content have at least one file
        content = dir.content();
        assertTrue(dir.exists());
        assertTrue(content.toString(), content.size() > 0);
        // And the new file have the expected content
        assertEquals(DATA, IOUtils.toString(dir.entry("a.txt").asInputStream(), UTF_8));


        // Delete all files in dir
        dir.delete();

        // Verify there is no data
        assertFalse(dir.exists());
        content = dir.content();
        assertEquals(content.toString(), 0, content.size());

        // Write a file
        writeDataToDataSource(dir.entry("b.txt"));

        content = dir.content();
        // Assert file is moved
        assertEquals(content.toString(), 1, content.size());
        assertTrue(content.toString(), content.toString().contains("my-test-dir/b.txt"));

        // Cleanup
        dir.delete();
        dir.close();
    }

    @Test
    public void testGsDataSource() throws IOException {
        String tempDir = "temp-dir";

        // Make sure we start with an empty folder
        cleanUpDir(tempDir);

        // Create on new file
        URI dsUri = toUri(BUCKET_NAME, tempDir + "/ds.txt");
        DataSource ds = repo.findSource(dsUri, FileType.UNKNOWN);

        //assertFalse(ds.exists());
        assertEquals(tempDir + "/ds.txt",  ds.name());
        assertEquals(GsHelper.toUriString(BUCKET_NAME, tempDir + "/ds.txt"),  ds.path());
        assertEquals(FileType.UNKNOWN,  ds.type());
        assertTrue( ds.isWritable());
        assertFalse(ds.exists());

        // Create a new file
        writeDataToDataSource(ds);

        // Retrieve the new created file
        ds = repo.findSource(dsUri, FileType.UNKNOWN);

        // Validate the file
        assertTrue(ds.exists());
        assertEquals("temp-dir/ds.txt",  ds.name());
        assertEquals("gs://" + BUCKET_NAME + "/temp-dir/ds.txt",  ds.path());
        assertEquals(FileType.UNKNOWN,  ds.type());
        assertTrue("LastModified: " + ds.lastModified(),  ds.lastModified() > 0);
        assertTrue("Size: " + ds.size(),  ds.size() > 0);
        assertTrue( ds.isWritable());
        assertEquals(DATA,  IOUtils.toString(ds.asInputStream(), UTF_8));

        // Cleanup
        cleanUpDir(tempDir);
    }

    @Test
    //@Ignore("This test is a manual test, because it require an Google Cloud Store to run.")
    public void testReadingZipFile() throws Exception {
        CompositeDataSource ds = repo.findCompositeSource(GTFS_URI, FileType.GTFS);

        DataSource stops = ds.entry("stops.txt");
        String text = IOUtils.toString(stops.asInputStream(), UTF_8);
        assertTrue(text, text.contains("stop"));
    }

    private void cleanUpDir(String dir) {
        CompositeDataSource tempdir = repo.findCompositeSource(toUri(BUCKET_NAME, dir), FileType.REPORT);
        tempdir.delete();
    }

    private void writeDataToDataSource(DataSource entry) throws IOException {
        try(OutputStream output = entry.asOutputStream()) {
            IOUtils.write(DATA, output, UTF_8);
            output.flush();
        }
    }
}