package org.opentripplanner.graph_builder;

import java.io.File;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Represents the different types of input files for a graph build.
 */
public enum InputFileType {
    GTFS, OSM, CONFIG, OTHER;
    public static InputFileType forFile(File file) {
        String name = file.getName();
        if (name.endsWith(".zip")) {
            try {
                ZipFile zip = new ZipFile(file);
                ZipEntry stopTimesEntry = zip.getEntry("stop_times.txt");
                zip.close();
                if (stopTimesEntry != null) return GTFS;
            } catch (Exception e) { /* fall through */ }
        }
        if (name.endsWith(".pbf")) return OSM;
        if (name.endsWith(".osm")) return OSM;
        if (name.endsWith(".osm.xml")) return OSM;
        if (name.equals("Embed.properties")) return CONFIG;
        return OTHER;
    }
}