package org.opentripplanner.model.json_serialization;

import com.fasterxml.jackson.core.*;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import junit.framework.TestCase;
import org.junit.Test;
import org.opentripplanner.common.geometry.GeometryUtils;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Test that the EncodedPolylineSerializer works.
 */
public class EncodedPolylineJSONSerializerTest extends TestCase {

    /** Ensure that serializing and deserializing a line string works. We did once have a situation where on every serialization/deserialization the coordinates would be reversed. */
    @Test
    public void testJsonSerialization () throws Exception {
        LineString geom = GeometryUtils.getGeometryFactory().createLineString(new Coordinate[] {
                new Coordinate(-122.123, 37.363),
                new Coordinate(-122.125, 37.363),
                new Coordinate(-122.127, 37.365)
        });

        // use array to get around "effectively final"
        String[] encoded = new String[1];

        JsonGenerator jgen = new FakeJsonGenerator(o -> encoded[0] = (String) o);

        new EncodedPolylineJSONSerializer().serialize(geom, jgen, null);

        Geometry geom2 = new EncodedPolylineJSONDeserializer().deserialize(new FakeJsonParser(() -> encoded[0]), null);

        assertTrue(geom2 instanceof LineString);
        LineString st2 = (LineString) geom2;
        assertEquals(3, st2.getNumPoints());

        for (int i = 0; i < 3; i++) {
            assertEquals(geom.getCoordinateN(i).x, st2.getCoordinateN(i).x, 1e-4);
            assertEquals(geom.getCoordinateN(i).y, st2.getCoordinateN(i).y, 1e-4);
        }
    }

    // Fake JSON generators and parsers that implement just enough functionality for us to test encoding
    public static class FakeJsonGenerator extends JsonGenerator {
        private Consumer<Object> objectConsumer;

        public FakeJsonGenerator (Consumer<Object> objectConsumer) {
            this.objectConsumer = objectConsumer;
        }

        @Override
        public JsonGenerator setCodec(ObjectCodec objectCodec) {
            return null;
        }

        @Override
        public ObjectCodec getCodec() {
            return null;
        }

        @Override
        public Version version() {
            return null;
        }

        @Override
        public JsonGenerator enable(Feature feature) {
            return null;
        }

        @Override
        public JsonGenerator disable(Feature feature) {
            return null;
        }

        @Override
        public boolean isEnabled(Feature feature) {
            return false;
        }

        @Override
        public int getFeatureMask() {
            return 0;
        }

        @Override
        public JsonGenerator setFeatureMask(int i) {
            return null;
        }

        @Override
        public JsonGenerator useDefaultPrettyPrinter() {
            return null;
        }

        @Override
        public void writeStartArray() throws IOException {

        }

        @Override
        public void writeEndArray() throws IOException {

        }

        @Override
        public void writeStartObject() throws IOException {

        }

        @Override
        public void writeEndObject() throws IOException {

        }

        @Override
        public void writeFieldName(String s) throws IOException {

        }

        @Override
        public void writeFieldName(SerializableString serializableString) throws IOException {

        }

        @Override
        public void writeString(String s) throws IOException {

        }

        @Override
        public void writeString(char[] chars, int i, int i1) throws IOException {

        }

        @Override
        public void writeString(SerializableString serializableString) throws IOException {

        }

        @Override
        public void writeRawUTF8String(byte[] bytes, int i, int i1) throws IOException {

        }

        @Override
        public void writeUTF8String(byte[] bytes, int i, int i1) throws IOException {

        }

        @Override
        public void writeRaw(String s) throws IOException {

        }

        @Override
        public void writeRaw(String s, int i, int i1) throws IOException {

        }

        @Override
        public void writeRaw(char[] chars, int i, int i1) throws IOException {

        }

        @Override
        public void writeRaw(char c) throws IOException {

        }

        @Override
        public void writeRawValue(String s) throws IOException {

        }

        @Override
        public void writeRawValue(String s, int i, int i1) throws IOException {

        }

        @Override
        public void writeRawValue(char[] chars, int i, int i1) throws IOException {

        }

        @Override
        public void writeBinary(Base64Variant base64Variant, byte[] bytes, int i, int i1) throws IOException {

        }

        @Override
        public int writeBinary(Base64Variant base64Variant, InputStream inputStream, int i) throws IOException {
            return 0;
        }

        @Override
        public void writeNumber(int i) throws IOException {

        }

        @Override
        public void writeNumber(long l) throws IOException {

        }

        @Override
        public void writeNumber(BigInteger bigInteger) throws IOException {

        }

        @Override
        public void writeNumber(double v) throws IOException {

        }

        @Override
        public void writeNumber(float v) throws IOException {

        }

        @Override
        public void writeNumber(BigDecimal bigDecimal) throws IOException {

        }

        @Override
        public void writeNumber(String s) throws IOException {

        }

        @Override
        public void writeBoolean(boolean b) throws IOException {

        }

        @Override
        public void writeNull() throws IOException {

        }

        @Override
        public void writeObject(Object o) throws IOException {
            objectConsumer.accept(o);
        }

        @Override
        public void writeTree(TreeNode treeNode) throws IOException {

        }

        @Override
        public JsonStreamContext getOutputContext() {
            return null;
        }

        @Override
        public void flush() throws IOException {

        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public void close() throws IOException {

        }
    }

    public static class FakeJsonParser extends JsonParser {

        private Supplier<String> stringProducer;

        public FakeJsonParser (Supplier<String> stringProducer) {
            this.stringProducer = stringProducer;
        }

        @Override
        public ObjectCodec getCodec() {
            return null;
        }

        @Override
        public void setCodec(ObjectCodec objectCodec) {

        }

        @Override
        public Version version() {
            return null;
        }

        @Override
        public void close() throws IOException {

        }

        @Override
        public JsonToken nextToken() throws IOException, JsonParseException {
            return null;
        }

        @Override
        public JsonToken nextValue() throws IOException, JsonParseException {
            return null;
        }

        @Override
        public JsonParser skipChildren() throws IOException, JsonParseException {
            return null;
        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public JsonToken getCurrentToken() {
            return null;
        }

        @Override
        public int getCurrentTokenId() {
            return 0;
        }

        @Override
        public boolean hasCurrentToken() {
            return false;
        }

        @Override
        public boolean hasTokenId(int i) {
            return false;
        }

        @Override
        public String getCurrentName() throws IOException {
            return null;
        }

        @Override
        public JsonStreamContext getParsingContext() {
            return null;
        }

        @Override
        public JsonLocation getTokenLocation() {
            return null;
        }

        @Override
        public JsonLocation getCurrentLocation() {
            return null;
        }

        @Override
        public void clearCurrentToken() {

        }

        @Override
        public JsonToken getLastClearedToken() {
            return null;
        }

        @Override
        public void overrideCurrentName(String s) {

        }

        @Override
        public String getText() throws IOException {
            return null;
        }

        @Override
        public char[] getTextCharacters() throws IOException {
            return new char[0];
        }

        @Override
        public int getTextLength() throws IOException {
            return 0;
        }

        @Override
        public int getTextOffset() throws IOException {
            return 0;
        }

        @Override
        public boolean hasTextCharacters() {
            return false;
        }

        @Override
        public Number getNumberValue() throws IOException {
            return null;
        }

        @Override
        public NumberType getNumberType() throws IOException {
            return null;
        }

        @Override
        public int getIntValue() throws IOException {
            return 0;
        }

        @Override
        public long getLongValue() throws IOException {
            return 0;
        }

        @Override
        public BigInteger getBigIntegerValue() throws IOException {
            return null;
        }

        @Override
        public float getFloatValue() throws IOException {
            return 0;
        }

        @Override
        public double getDoubleValue() throws IOException {
            return 0;
        }

        @Override
        public BigDecimal getDecimalValue() throws IOException {
            return null;
        }

        @Override
        public Object getEmbeddedObject() throws IOException {
            return null;
        }

        @Override
        public byte[] getBinaryValue(Base64Variant base64Variant) throws IOException {
            return new byte[0];
        }

        @Override
        public String getValueAsString(String s) throws IOException {
            return stringProducer.get();
        }
    }
}