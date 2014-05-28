package org.opentripplanner.analyst;

import com.csvreader.CsvReader;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.opentripplanner.analyst.core.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * PointSets serve as destinations in web analyst one-to-many indicators.
 * They can also serve as origins in many-to-many indicators.
 *
 * PointSets are one of the three main web analyst resources:
 * Pointsets
 * Indicators
 * TimeSurfaces
 *
 */
public class PointSet {

    private static final Logger LOG = LoggerFactory.getLogger(PointSet.class);

    List<Category> categories = Lists.newArrayList();
    public int nFeatures = 0;
    public SampleSet samples; // connects this population to a graph

    /**
     * The geometries of the features.
     * Each Attribute must contain an array of magnitudes with the same length as this list.
     */
    protected double[] lats;
    protected double[] lons;

    /** A base class for the various levels of the Analyst GeoJSON "structured" attributes. */
    public static abstract class Structured {
        String id;
        String label;
        public Structured (String id) {
            this.id = id;
            this.label = id;
        }
        public Structured (Structured other) {
            this.id = other.id;
            this.label = other.label;
        }
    }

    public static class Category extends Structured {
        List<Attribute> attributes = Lists.newArrayList();
        public Category (String id) { super(id); }
        /** Deep copy constructor. */
        public Category(Category other) {
            super(other);
            for (Attribute attr : other.attributes) {
                attributes.add(new Attribute(attr));
            }
        }
    }

    // one array of int per leaf attribute, containing magnitudes
    public static class Attribute extends Structured {
        int[] magnitudes;
        Quantiles[] quantiles; // should maybe be an array, one per origin for these destinations
        /** Shallow copy constructor. */
        public Attribute (String id) { super(id); }
        public Attribute (Attribute other) {
            super(other);
            this.magnitudes = other.magnitudes;
            this.quantiles = other.quantiles;
        }
    }

    /**
     * Rather than trying to load anything any everything, we stick to a strict format and rely on other tools to get
     * the data into the correct format. This includes column headers in the category:subcategory:attribute format
     * and coordinates in WGS84. Comments begin with a #.
     */
    public static PointSet fromCsv(String filename) throws IOException {
        /* First, scan through the file to count lines and check for errors. */
        CsvReader reader = new CsvReader(filename, ',', Charset.forName("UTF8"));
        reader.readHeaders();
        int nCols = reader.getHeaderCount();
        while (reader.readRecord()) {
            if (reader.getColumnCount() != nCols) {
                LOG.error("CSV record {} has the wrong number of fields.", reader.getCurrentRecord());
                return null;
            }
        }
        // getCurrentRecord is zero-based and does not include headers or blank lines.
        int nRecs = (int) reader.getCurrentRecord() + 1;
        reader.close();
        /* If we reached here, the file is entirely readable. Start over. */
        reader = new CsvReader(filename, ',', Charset.forName("UTF8"));
        PointSet ret = new PointSet();
        reader.readHeaders();
        if (reader.getHeaderCount() != nCols) {
            LOG.error("Number of headers changed.");
            return null;
        }
        int latCol = -1;
        int lonCol = -1;
        Attribute[] attributes = new Attribute[nCols];
        for (int c = 0; c < nCols; c++) {
            String header = reader.getHeader(c);
            if (header.equalsIgnoreCase("lat") || header.equalsIgnoreCase("latitude")) {
                latCol = c;
            } else if (header.equalsIgnoreCase("lon") || header.equalsIgnoreCase("longitude")) {
                lonCol = c;
            } else {
                Attribute attr = ret.getAttributeForColumn(header, "default", true);
                attributes[c] = attr;
            }
        }
        if (latCol < 0 || lonCol < 0) {
            LOG.error("CSV file did not contain a latitude or longitude column.");
            throw new IOException();
        }
        for (Attribute attr : attributes) {
            if (attr != null) attr.magnitudes = new int[nRecs];
        }
        ret.lats = new double[nRecs];
        ret.lons = new double[nRecs];
        while (reader.readRecord()) {
            int rec = (int) reader.getCurrentRecord();
            for (int c = 0; c < nCols; c++) {
                Attribute attr = attributes[c];
                if (attr == null) continue; // skip lat and lon columns
                int mag = Integer.parseInt(reader.get(c));
                attr.magnitudes[rec] = mag;
            }
            ret.lats[rec] = Double.parseDouble(reader.get(latCol));
            ret.lons[rec] = Double.parseDouble(reader.get(lonCol));
        }
        ret.nFeatures = nRecs;
        return ret;
    }

    public PointSet fromJson () {
        return new PointSet();
    }

    /**
     * @heading in the form "schools:level:primary"
     * @return null if there are too many levels or the attribute does not exist and you did not
     * ask to create it.
     */
    public Attribute getAttributeForColumn(String heading, String def, boolean create) {
        List<String> levels = Arrays.asList(heading.split(":"));
        if (levels.size() > 2) return null;
        if (levels.size() < 2) { // should be 3
            levels = Lists.newLinkedList(levels);
            while (levels.size() < 2) {
                levels.add(0, def); // pad the levels on the left with the default
            }
        }
        Category category = null;
        for (Category cat : categories) {
            if (cat.id.equals(levels.get(0))) {
                category = cat;
                break;
            }
        }
        if (category == null) {
            if (create) {
                category = new Category(levels.get(0));
                categories.add(category);
            }
            else return null;
        }
        Attribute attribute = null;
        for (Attribute attr : category.attributes) {
            if (attr.id.equals(levels.get(1))) {
                attribute = attr;
                break;
            }
        }
        if (attribute == null && create) {
            attribute = new Attribute(levels.get(1));
            category.attributes.add(attribute);
        }
        return attribute;
    }

    /**
     * Use the Jackson streaming API to output this as GeoJSON without creating another object.
     * The Indicator is a column store, and is transposed WRT the JSON representation.
     */
    public void writeJson(OutputStream out) {
        try {
            JsonFactory jsonFactory = new JsonFactory(); // ObjectMapper.getJsonFactory() is better
            JsonGenerator jgen = jsonFactory.createGenerator(out);
            jgen.setCodec(new ObjectMapper());
            jgen.writeStartObject(); {
                jgen.writeStringField("type", "FeatureCollection");
                jgen.writeArrayFieldStart("features"); {
                    for (int f = 0; f < nFeatures; f++) {
                        writeFeature(f, jgen);
                    }
                } jgen.writeEndArray();
            } jgen.writeEndObject();
            jgen.close();
        } catch (IOException ioex) {
            LOG.info("IOException, connection may have been closed while streaming JSON.");
        }
    }

    private void writeFeature(int i, JsonGenerator jgen) throws IOException {
        jgen.writeStartObject(); {
            jgen.writeStringField("type", "Feature");
            jgen.writeObjectFieldStart("geometry"); {
                jgen.writeStringField("type", "Point");
                jgen.writeArrayFieldStart("coordinates"); {
                    jgen.writeNumber(lons[i]);
                    jgen.writeNumber(lats[i]);
                } jgen.writeEndArray();
            } jgen.writeEndObject();
            jgen.writeObjectFieldStart("properties"); {
                writeStructured(i, jgen);
            } jgen.writeEndObject();
        } jgen.writeEndObject();
    }

    /**
     * This will be called once per point in an origin/destination pointset, and once per origin
     * in a one- or many-to-many indicator.
     */
    private void writeStructured(int i, JsonGenerator jgen) throws IOException {
        jgen.writeObjectFieldStart("structured");
        for (Category cat : categories) {
            jgen.writeObjectFieldStart(cat.id);
            for (Attribute attr : cat.attributes) {
                if (attr.quantiles != null) {
                    jgen.writeObjectField(attr.id, attr.quantiles[i]);
                } else if (attr.magnitudes != null) {
                    jgen.writeNumberField(attr.id, attr.magnitudes[i]);
                }
            }
            jgen.writeEndObject();
        }
        jgen.writeEndObject();
    }


}
