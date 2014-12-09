package org.opentripplanner.analyst;

import com.bedatadriven.geojson.GeometryDeserializer;
import com.bedatadriven.geojson.GeometrySerializer;
import com.csvreader.CsvReader;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.referencing.CRS;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.PropertyType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.analyst.batch.Individual;
import org.opentripplanner.analyst.pointset.PropertyMetadata;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PointSets serve as destinations in web analyst one-to-many indicators. They
 * can also serve as origins in many-to-many indicators.
 * 
 * PointSets are one of the three main web analyst resources: Pointsets
 * Indicators TimeSurfaces
 */
public class PointSet implements Serializable{

    private static final long serialVersionUID = -8962916330731463238L;

    private static final Logger LOG = LoggerFactory.getLogger(PointSet.class);

    public String id;
    public String label;
    public String description;

    public Map<String, PropertyMetadata> propMetadata = new HashMap<String, PropertyMetadata>();
    public Map<String, int[]> properties = new ConcurrentHashMap<String, int[]>();
    public int capacity = 0; // The total number of features this PointSet can
    // hold.
    /*
     * Connects this population to vertices in a given Graph (map of graph ids
     * to sample sets). Keeping as a graphId->sampleSet map to prevent
     * duplication of pointset when used across multiple graphs
     */
    private Map<String, SampleSet> samples = new ConcurrentHashMap<String, SampleSet>();


    /*
     * Used to generate SampleSets on an as needed basis. 
     */
    protected GraphService graphService;

    /*
     * In a detailed Indicator, the time to reach each target, for each origin.
     * Null in non-indicator pointsets.
     */
    public int[][] times;

    /**
     * The geometries of the features. Each Attribute must contain an array of
     * magnitudes with the same length as this list.
     */

    protected String[] ids;
    protected double[] lats;
    protected double[] lons;
    protected Polygon[] polygons;

    /**
     * Rather than trying to load anything any everything, we stick to a strict
     * format and rely on other tools to get the data into the correct format.
     * This includes column headers in the category:subcategory:attribute format
     * and coordinates in WGS84. Comments begin with a #.
     */
    public static PointSet fromCsv(File filename) throws IOException {
        /* First, scan through the file to count lines and check for errors. */
        CsvReader reader = new CsvReader(filename.getAbsolutePath(), ',', Charset.forName("UTF8"));
        reader.readHeaders();
        int nCols = reader.getHeaderCount();
        while (reader.readRecord()) {
            if (reader.getColumnCount() != nCols) {
                LOG.error("CSV record {} has the wrong number of fields.", reader.getCurrentRecord());
                return null;
            }
        }
        // getCurrentRecord is zero-based and does not include headers or blank
        // lines.
        int nRecs = (int) reader.getCurrentRecord() + 1;
        reader.close();
        /* If we reached here, the file is entirely readable. Start over. */
        reader = new CsvReader(filename.getAbsolutePath(), ',', Charset.forName("UTF8"));
        PointSet ret = new PointSet(nRecs);
        reader.readHeaders();
        if (reader.getHeaderCount() != nCols) {
            LOG.error("Number of headers changed.");
            return null;
        }
        int latCol = -1;
        int lonCol = -1;

        int[][] properties = new int[nCols][ret.capacity];
        for (int c = 0; c < nCols; c++) {
            String header = reader.getHeader(c);
            if (header.equalsIgnoreCase("lat") || header.equalsIgnoreCase("latitude")) {
                latCol = c;
            } else if (header.equalsIgnoreCase("lon") || header.equalsIgnoreCase("longitude")) {
                lonCol = c;
            } else {
                ret.getOrCreatePropertyForId(header);
                properties[c] = ret.properties.get(header);
            }
        }
        if (latCol < 0 || lonCol < 0) {
            LOG.error("CSV file did not contain a latitude or longitude column.");
            throw new IOException();
        }
        ret.lats = new double[nRecs];
        ret.lons = new double[nRecs];
        while (reader.readRecord()) {
            int rec = (int) reader.getCurrentRecord();
            for (int c = 0; c < nCols; c++) {
                if(c==latCol || c==lonCol){
                    continue;
                }

                int[] prop = properties[c];
                int mag = Integer.parseInt(reader.get(c));
                prop[rec] = mag;
            }
            ret.lats[rec] = Double.parseDouble(reader.get(latCol));
            ret.lons[rec] = Double.parseDouble(reader.get(lonCol));
        }
        ret.capacity = nRecs;
        return ret;
    }

    public static PointSet fromShapefile( File file ) throws IOException, NoSuchAuthorityCodeException, FactoryException, EmptyPolygonException, UnsupportedGeometryException {
        if ( ! file.exists())
            throw new RuntimeException("Shapefile does not exist.");

        FileDataStore store = FileDataStoreFinder.getDataStore(file);
        SimpleFeatureSource featureSource = store.getFeatureSource();

        CoordinateReferenceSystem sourceCRS = featureSource.getInfo().getCRS();
        CoordinateReferenceSystem WGS84 = CRS.decode("EPSG:4326", true);

        Query query = new Query();
        query.setCoordinateSystem(sourceCRS);
        query.setCoordinateSystemReproject(WGS84);
        SimpleFeatureCollection featureCollection = featureSource.getFeatures(query);

        SimpleFeatureIterator it = featureCollection.features();

        PointSet ret = new PointSet(featureCollection.size());
        int i=0;
        while (it.hasNext()) {
            SimpleFeature feature = it.next();
            Geometry geom = (Geometry) feature.getDefaultGeometry();

            PointFeature ft = new PointFeature();
            ft.setGeom(geom);
            for(Property prop : feature.getProperties() ){
                Object binding = prop.getType().getBinding();

                //attempt to coerce the prop's value into an integer
                int val;
                if(binding.equals(Integer.class)){
                    val = (Integer)prop.getValue();
                } else if(binding.equals(Long.class)){
                    val = ((Long)prop.getValue()).intValue();
                } else if(binding.equals(String.class)){
                    try{
                        val = Integer.parseInt((String)prop.getValue());
                    } catch (NumberFormatException ex ){
                        continue;
                    }
                } else {
                    continue;
                }

                ft.addAttribute(prop.getName().toString(), val);
            }

            ret.addFeature(ft, i);

            i++;
        }

        return ret;
    }

    public static PointSet fromGeoJson(File filename) {
        try {
            FileInputStream fis = new FileInputStream(filename);
            int n = validateGeoJson(fis);
            if (n < 0)
                return null;
            fis.getChannel().position(0); // rewind file
            return fromValidatedGeoJson(fis, n);
        } catch (FileNotFoundException ex) {
            LOG.error("GeoJSON file not found: {}", filename);
            return null;
        } catch (IOException ex) {
            LOG.error("I/O exception while reading GeoJSON file: {}", filename);
            return null;
        }
    }

    /**
     * Examines a JSON stream to see if it matches the expected OTPA format.
     * 
     * @return the number of features in the collection if it's valid, or -1 if
     *         it doesn't fit the OTPA format.
     */
    public static int validateGeoJson(InputStream is) {
        int n = 0;
        JsonFactory f = new JsonFactory();
        try {
            JsonParser jp = f.createParser(is);
            JsonToken current = jp.nextToken();
            if (current != JsonToken.START_OBJECT) {
                LOG.error("Root of OTPA GeoJSON should be a JSON object.");
                return -1;
            }
            // Iterate over the key:value pairs in the top-level JSON object
            while (jp.nextToken() != JsonToken.END_OBJECT) {
                String key = jp.getCurrentName();
                current = jp.nextToken();
                if (key.equals("features")) {
                    if (current != JsonToken.START_ARRAY) {
                        LOG.error("Error: GeoJSON features are not in an array.");
                        return -1;
                    }
                    // Iterate over the features in the array
                    while (jp.nextToken() != JsonToken.END_ARRAY) {
                        n += 1;
                        jp.skipChildren();
                    }
                } else {
                    jp.skipChildren(); // ignore all other keys except features
                }
            }
            if (n == 0)
                return -1; // JSON has no features
            return n;
        } catch (Exception ex) {
            LOG.error("Exception while validating GeoJSON: {}", ex);
            return -1;
        }
    }

    /**
     * Reads with a combination of streaming and tree-model to allow very large
     * GeoJSON files. The JSON should be already validated, and you must pass in
     * the maximum number of features from that validation step.
     */
    private static PointSet fromValidatedGeoJson(InputStream is, int n) {
        JsonFactory f = new MappingJsonFactory();
        PointSet ret = new PointSet(n);
        int index = 0;
        try {
            JsonParser jp = f.createParser(is);
            JsonToken current = jp.nextToken();
            // Iterate over the key:value pairs in the top-level JSON object
            while (jp.nextToken() != JsonToken.END_OBJECT) {
                String key = jp.getCurrentName();
                current = jp.nextToken();
                if (key.equals("properties")) {
                    JsonNode properties = jp.readValueAsTree();

                    if(properties.get("id") != null)
                        ret.id = properties.get("id").asText();
                    if(properties.get("label") != null)
                        ret.label = properties.get("label").asText();
                    if(properties.get("description") != null)
                        ret.label = properties.get("description").asText();

                    if(properties.get("schema") != null) {

                        Iterator<Entry<String, JsonNode>> catIter = properties.get("schema").fields();
                        while (catIter.hasNext()) {
                            Entry<String, JsonNode> catEntry = catIter.next();
                            String catName = catEntry.getKey();
                            JsonNode catNode = catEntry.getValue();

                            PropertyMetadata cat = new PropertyMetadata(catName);

                            if(catNode.get("label") != null)
                                cat.label = catNode.get("label").asText();

                            if(catNode.get("style") != null) {
                                Iterator<Entry<String, JsonNode>> styleIter = catNode.get("style").fields();
                                while (styleIter.hasNext()) {
                                    Entry<String, JsonNode> styleEntry = styleIter.next();
                                    String styleName = styleEntry.getKey();
                                    JsonNode styleValue = styleEntry.getValue();

                                    cat.addStyle(styleName, styleValue.asText());
                                }
                            }

                            ret.propMetadata.put(catName, cat);
                        }
                    }					
                }	
                if (key.equals("features")) {
                    while (jp.nextToken() != JsonToken.END_ARRAY) {
                        // Read the feature into a tree model, which moves
                        // parser to its end.
                        JsonNode feature = jp.readValueAsTree();
                        ret.addFeature(feature, index++);
                    }
                } else {
                    jp.skipChildren(); // ignore all other keys except features
                }
            }
        } catch (Exception ex) {			
            LOG.error("GeoJSON parsing failure: {}", ex.toString());
            return null;
        }
        return ret;
    }

    /**
     * Add one GeoJSON feature to this PointSet from a Jackson node tree.
     * com.bedatadriven.geojson only exposed its streaming Geometry parser as a
     * public method. I made its tree parser public as well. Geotools also has a
     * GeoJSON parser called GeometryJson (which OTP wraps in
     * GeoJsonDeserializer) but it consumes straight text, not a Jackson model
     * or streaming parser.
     */
    private void addFeature(JsonNode feature, int index) {

        PointFeature feat = null;
        try {
            feat = PointFeature.fromJsonNode(feature);
        } catch (EmptyPolygonException e) {
            LOG.warn("Empty MultiPolygon, skipping.");
            return;
        } catch (UnsupportedGeometryException e) {
            LOG.warn(e.message);
            return;
        }

        if (feat == null) {
            return;
        }

        addFeature(feat, index);
    }

    /**
     * Create a PointSet manually by defining capacity and calling
     * addFeature(geom, data) repeatedly.
     * 
     * @param capacity
     *            expected number of features to be added to this PointSet.
     */
    public PointSet(int capacity) {
        this.capacity = capacity;
        ids = new String[capacity];
        lats = new double[capacity];
        lons = new double[capacity];
        polygons = new Polygon[capacity];
    }

    /**
     * Adds a graph service to allow for auto creation of SampleSets for a given
     * graph
     * 
     * @param reference
     *            to the application graph service
     */

    public void setGraphService(GraphService graphService) {
        this.graphService = graphService;
    }

    /**
     * gets a sample set for a given graph id -- requires graphservice to be set
     * 
     * @param a valid graph id
     * @return sampleset for graph
     */

    public SampleSet getSampleSet(String routerId) {
        if(this.graphService == null) 
            return null;

        if (this.samples.containsKey(routerId))
            return this.samples.get(routerId);
        Graph g = this.graphService.getGraph(routerId);

        return getSampleSet(g);
    }

    /** 
     * gets a sample set for a graph object -- does not require graph service to be set 
     * @param g a graph objects
     * @return sampleset for graph
     */

    public SampleSet getSampleSet(Graph g) {	
        if (g == null)
            return null;
        SampleSet sampleSet = new SampleSet(this, g.getSampleFactory());
        this.samples.put(g.routerId, sampleSet);
        return sampleSet;
    }


    /**
     * Add a single feature with a variable number of free-form properties.
     * Attribute data contains id value pairs, ids are in form "cat_id:prop_id".
     * If the properties and categories do not exist, they will be created.
     * TODO: read explicit schema or infer it and validate property presence as
     * they're read
     * 
     * @param geom
     *            must be a Point, a Polygon, or a single-element MultiPolygon
     */

    public int featureCount() {
        return ids.length;
    }

    public void addFeature(PointFeature feat, int index) {
        if (index >= capacity) {
            throw new AssertionError("Number of features seems to have grown since validation.");
        }

        polygons[index] = feat.getPolygon();
        lats[index] = feat.getLat();
        lons[index] = feat.getLon();

        ids[index] = feat.getId();

        for (Entry<String,Integer> ad : feat.getProperties().entrySet()) {
            String propId = ad.getKey();
            Integer propVal = ad.getValue();

            this.getOrCreatePropertyForId(propId);
            this.properties.get(propId)[index] = propVal;


        }
    }

    public PointFeature getFeature(int index) {
        PointFeature ret = new PointFeature(ids[index]);

        if (polygons[index] != null) {
            try {
                ret.setGeom(polygons[index]);
            } catch (Exception e) {	
                // The polygon is clean; this should never happen. We
                // could pass the exception up but that'd just make the calling
                // function deal with an exception that will never pop. So
                // we'll make the compiler happy by catching it here silently.
            }
        }

        // ret.setGeom, if it was called, will already set the lat and lon
        // properties. But since every item in this pointset is guaranteed
        // to have a lat/lon coordinate, we defer to it as more authoritative.
        ret.setLat(lats[index]);
        ret.setLon(lons[index]);

        for (Entry<String, int[]> property : this.properties.entrySet()) {
            ret.addAttribute( property.getKey(), property.getValue()[index]);
        }

        return ret;
    }

    public void setLabel(String catId, String label) {
        PropertyMetadata meta = this.propMetadata.get(catId);
        if(meta!=null){
            meta.setLabel( label );
        }		
    }

    public void setStyle(String catId, String styleAttribute, String styleValue) {
        PropertyMetadata meta = propMetadata.get(catId);

        if(meta!=null){
            meta.addStyle( styleAttribute, styleValue );
        }
    }

    /**
     * Gets the Category object for the given ID, creating it if it doesn't
     * exist.
     * 
     * @param id
     *            the id for the category alone, not the fully-specified
     *            category:property.
     * @return a Category with the given ID.
     */
    public PropertyMetadata getOrCreatePropertyForId(String id) {
        PropertyMetadata property = propMetadata.get(id);
        if (property == null) {
            property = new PropertyMetadata(id);
            propMetadata.put(id, property);
        }
        if(!properties.containsKey(id))
            properties.put(id, new int[capacity]);

        return property;
    }

    public void writeJson(OutputStream out) {
        writeJson(out, false);
    }

    /**
     * Use the Jackson streaming API to output this as GeoJSON without creating
     * another object. The Indicator is a column store, and is transposed WRT
     * the JSON representation.
     */
    public void writeJson(OutputStream out, Boolean forcePoints) {
        try {
            JsonFactory jsonFactory = new JsonFactory(); // ObjectMapper.getJsonFactory()
            // is better
            JsonGenerator jgen = jsonFactory.createGenerator(out);
            jgen.setCodec(new ObjectMapper());
            jgen.writeStartObject();
            {

                jgen.writeStringField("type", "FeatureCollection");

                writeJsonProperties(jgen);

                jgen.writeArrayFieldStart("features");
                {
                    for (int f = 0; f < capacity; f++) {
                        writeFeature(f, jgen, forcePoints);
                    }
                }
                jgen.writeEndArray();
            }
            jgen.writeEndObject();
            jgen.close();
        } catch (IOException ioex) {
            LOG.info("IOException, connection may have been closed while streaming JSON.");
        }
    }

    public void writeJsonProperties(JsonGenerator jgen) throws JsonGenerationException, IOException {
        jgen.writeObjectFieldStart("properties");
        {

            if (id != null)
                jgen.writeStringField("id", id);
            if (label != null)
                jgen.writeStringField("label", label);
            if (description != null)
                jgen.writeStringField("description", description);

            // writes schema as a flat namespace with cat_id and
            // cat_id:prop_id interleaved

            jgen.writeObjectFieldStart("schema");
            {

                for (PropertyMetadata cat : this.propMetadata.values()) {

                    jgen.writeObjectFieldStart(cat.id);
                    {
                        if (cat.label != null)
                            jgen.writeStringField("label", cat.label);
                        jgen.writeStringField("type", "Category");

                        if (cat.style != null && cat.style.attributes != null) {

                            jgen.writeObjectFieldStart("style");
                            {

                                for (String styleKey : cat.style.attributes.keySet()) {
                                    jgen.writeStringField(styleKey, cat.style.attributes.get(styleKey));
                                }
                            }
                            jgen.writeEndObject();

                        }

                    }
                    jgen.writeEndObject();

                    // two-level hierarchy for now... could be extended
                    // to recursively map
                    // categories,sub-categories,attributes
                }

            }
            jgen.writeEndObject();
        }
        jgen.writeEndObject();

    }

    /**
     * Pairs an array of times with the array of features in this pointset,
     * writing out the resulting (ID,time) pairs to a JSON object.
     */
    protected void writeTimes(JsonGenerator jgen, int[] times) throws IOException {
        jgen.writeObjectFieldStart("times");
        for (int i = 0; i < times.length; i++) { // capacity is now 1 if this is
            // a one-to-many indicator
            int t = times[i];
            if (t != Integer.MAX_VALUE)
                jgen.writeNumberField(ids[i], t);
        }
        jgen.writeEndObject();
    }

    /**
     * This writes either a polygon or lat/lon point defining the feature. In
     * the case of polygons, we convert these back to centroids on import, as
     * OTPA depends on the actual point. The polygons are kept for derivative
     * uses (e.g. visualization)
     * 
     * @param i
     *            the feature index
     * @param jgen
     *            the Jackson streaming JSON generator to which the geometry
     *            will be written
     * @throws IOException
     */
    private void writeFeature(int i, JsonGenerator jgen, Boolean forcePoints) throws IOException {

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel());

        GeometrySerializer geomSerializer = new GeometrySerializer();

        jgen.writeStartObject();
        {
            jgen.writeStringField("id", ids[i]);
            jgen.writeStringField("type", "Feature");
            jgen.writeFieldName("geometry");
            {

                if (!forcePoints && polygons != null && polygons.length >= i && polygons[i] != null) {
                    geomSerializer.writeGeometry(jgen, polygons[i]);
                } else {

                    Point p = geometryFactory.createPoint(new Coordinate(lons[i], lats[i]));
                    geomSerializer.writeGeometry(jgen, p);
                }

            }
            jgen.writeObjectFieldStart("properties");
            {
                writeStructured(i, jgen);
            }
            jgen.writeEndObject();
        }
        jgen.writeEndObject();
    }

    /**
     * This will be called once per point in an origin/destination pointset, and
     * once per origin in a one- or many-to-many indicator.
     */
    protected void writeStructured(int i, JsonGenerator jgen) throws IOException {
        jgen.writeObjectFieldStart("structured");
        for (Entry<String,int[]> entry : properties.entrySet()) {
            jgen.writeNumberField( entry.getKey(), entry.getValue()[i] );
        }
        jgen.writeEndObject();
    }

    /**
     * Get a subset of this point set containing only the specified point IDs.
     */
    public PointSet slice(List<String> ids) {

        PointSet ret = new PointSet(ids.size());

        HashSet<String> idsHashSet = new HashSet<String>(ids);

        ret.id = id;
        ret.label = label;
        ret.description = description;

        int n = 0;

        for (int i = 0; i < this.ids.length; i++) {
            if(idsHashSet.contains(this.ids[i])) {	
                ret.lats[n] = this.lats[i];
                ret.lons[n] = this.lons[i];
                ret.ids[n] = this.ids[i];
                ret.polygons[n] = this.polygons[i];
                n++;
            }
        }

        return ret;
    }

    public PointSet slice(int start, int end) {
        PointSet ret = new PointSet(end - start);

        ret.id = id;
        ret.label = label;
        ret.description = description;

        int n = 0;
        for (int i = start; i < end; i++) {
            ret.lats[n] = this.lats[i];
            ret.lons[n] = this.lons[i];
            ret.ids[n] = this.ids[i];
            ret.polygons[n] = this.polygons[i];
            n++;
        }

        for(Entry<String, int[]> property : this.properties.entrySet()) {
            int[] data = property.getValue();

            int[] magSlice = new int[end-start];
            n=0;
            for(int i=start; i<end; i++){
                magSlice[n] = data[i];
                n++;
            }

            ret.properties.put( property.getKey(), magSlice );
        }

        return ret;
    }

}
