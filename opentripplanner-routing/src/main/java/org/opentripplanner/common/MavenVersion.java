package org.opentripplanner.common;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenVersion {

    private static final Logger LOG = LoggerFactory.getLogger(MavenVersion.class);

    private static final Properties PROPS;
    public static final String VERSION; 
    public static final int MAJOR;
    public static final int MINOR;
    public static final int INCREMENTAL;
    public static final String QUALIFIER;
    public static final long UID;

    static {
        final String FILE = "maven-version.properties";
        PROPS = new java.util.Properties();
        try {
            InputStream in = MavenVersion.class.getClassLoader().getResourceAsStream(FILE);
            PROPS.load(in);
        } catch (Exception e) {
            throw new IllegalStateException("Error loading Maven artifact version from properties file.");
        }
        String v = VERSION = PROPS.getProperty("version");
        String [] fields = v.split("\\-");
        if (fields.length > 1)
            QUALIFIER = fields[1];
        else
            QUALIFIER = "";
        fields = fields[0].split("\\.");
        if (fields.length > 0)
            MAJOR = Integer.parseInt(fields[0]);
        else
            MAJOR = 0;
        if (fields.length > 1)
            MINOR = Integer.parseInt(fields[1]);
        else
            MINOR = 0;
        if (fields.length > 2)
            INCREMENTAL = Integer.parseInt(fields[2]);
        else
            INCREMENTAL = 0;
        UID = (QUALIFIER.equals("SNAPSHOT") ? -1L : +1L) *
              (1000000L * MAJOR + 1000L * MINOR + INCREMENTAL);
        LOG.info("Maven artifact version read: {}", getVersion());
    }
    
    public static String getVersion() {
        return String.format("%s => (%d, %d, %d, %s) UID=%d", 
                VERSION, MAJOR, MINOR, INCREMENTAL, QUALIFIER, UID);
    }
}
