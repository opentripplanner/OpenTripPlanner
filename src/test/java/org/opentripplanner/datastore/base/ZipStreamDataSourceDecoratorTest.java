package org.opentripplanner.datastore.base;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.datastore.CompositeDataSource;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.datastore.file.FileDataSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.datastore.FileType.GTFS;

public class ZipStreamDataSourceDecoratorTest {
    private static final long TIME = 30 * 365 * 24 * 60 * 60 * 1000L;
    private static final String FILENAME = ConstantsForTests.CALTRAIN_GTFS;

    @Test
    public void testAccessorsForNoneExistingFile() throws IOException {
        // Given:
        File target = new File(FILENAME);
        File copyTarget = new File(FILENAME);
        CompositeDataSource subject = new ZipStreamDataSourceDecorator(new FileDataSource(target, GTFS));
        CompositeDataSource copySubject = new ZipStreamDataSourceDecorator(new FileDataSource(copyTarget, GTFS));
        String expectedPath = target.getPath();

        // Verify zip file exist before we start the test
        assertTrue(target.getAbsolutePath(), target.exists());

        // Then
        assertEquals("caltrain_gtfs.zip", subject.name());
        assertEquals(expectedPath, subject.path());
        assertEquals(GTFS, subject.type());
        assertTrue("Last modified: " + subject.lastModified(), subject.lastModified() > TIME);
        assertTrue("Size: " + subject.size(), subject.size() > 100);
        assertTrue(subject.exists());
        // We do not support writing to zip files
        assertFalse(subject.isWritable());

        assertEquals(expectedPath, subject.toString());

        subject.close();
        copySubject.close();
    }

    @Test
    public void testIO() throws IOException {
        // Given:
        File target = new File(FILENAME);
        CompositeDataSource subject = new ZipStreamDataSourceDecorator(new FileDataSource(target, GTFS));

        Collection<DataSource> content = subject.content();
        Collection<String> names = content.stream().map(it -> it.name()).collect(Collectors.toList());

        System.out.println(names);
        assertTrue(
                names.toString(),
                names.containsAll(List.of(
                        "trips.txt",
                        "agency.txt",
                        "calendar.txt",
                        "calendar_dates.txt",
                        "fare_attributes.txt",
                        "fare_rules.txt",
                        "routes.txt",
                        "shapes.txt",
                        "stop_times.txt",
                        "stops.txt"
                ))
        );

        DataSource entry = subject.entry("agency.txt");

        List<String> lines = IOUtils.readLines(entry.asInputStream(), StandardCharsets.UTF_8);
        assertEquals("agency_id,agency_name,agency_url,agency_timezone", lines.get(0));

        // Close zip
        subject.close();
    }

    @Test
    public void testEntryProperties() {
        // Given:
        File target = new File(FILENAME);
        CompositeDataSource subject = new ZipStreamDataSourceDecorator(new FileDataSource(target, GTFS));
        DataSource entry = subject.entry("trips.txt");

        assertEquals("trips.txt", entry.name());
        assertEquals("trips.txt (" + subject.path() + ")", entry.path());
        assertEquals(GTFS, entry.type());
        assertTrue("Last modified: " + entry.lastModified(), entry.lastModified() > TIME);
        assertTrue("Size: " + entry.size(), entry.size() > 100);
        assertTrue(entry.exists());
        // We do not support writing to zip entries
        assertFalse(entry.isWritable());
    }
}