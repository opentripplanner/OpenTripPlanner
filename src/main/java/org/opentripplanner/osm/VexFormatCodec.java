package org.opentripplanner.osm;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Decode (or encode) a stream of VEX data into an OTP OSM data store.
 * This is a sort of data pump between an OTP OSM store and and InputStream / OutputStream.
 * Neither threadsafe nor reentrant!
 */
public class VexFormatCodec {

    public static final String HEADER = "VEXFMT";
    public static final int VEX_NODE = 0;
    public static final int VEX_WAY = 1;
    public static final int VEX_RELATION = 2;
    public static final int VEX_NONE = 3;

    /* The input stream providing decompressed VEX format. */
    private CodedInputStream vin;

    /* The output sink for uncompressed VEX format. */
    private CodedOutputStream vout;

    /* Persistent values for delta coding. */
    private long id, ref, prevId, prevRef, prevFixedLat, prevFixedLon;
    private double lat, lon;

    /* The OSM data store to stuff the elements into. */
    private OSM osm;

    /* Writers demonstrate format, readers consume it. */
    public void writeVex(OSM osm, OutputStream gzVexStream) throws IOException {
        this.vout = CodedOutputStream.newInstance(new GZIPOutputStream(gzVexStream));
        this.osm = osm;
        vout.writeRawBytes(HEADER.getBytes());
        long nEntities = 0;
        nEntities += writeNodeBlock();
        nEntities += writeWayBlock();
        nEntities += writeRelationBlock();
        // Empty block of type NONE indicates end of blocks
        beginWriteBlock(VEX_NONE);
        endWriteBlock(0);
        // Total number of blocks written as a "checksum"
        vout.writeUInt64NoTag(nEntities);
    }

    public void readVex(InputStream gzVexStream, OSM osm) throws IOException {
        this.vin = CodedInputStream.newInstance(new GZIPInputStream(gzVexStream));
        this.osm = osm;
        byte[] header = vin.readRawBytes(HEADER.length());
        if ( ! Arrays.equals(header, HEADER.getBytes())) {
            throw new IOException("Corrupt header.");
        }
        boolean done = false;
        long nBlocks = 0;
        while ( ! done) {
            done = readBlock();
            nBlocks += 1;
        }
        long expectedBlocks = vin.readUInt64();
        if (expectedBlocks != nBlocks) {
            throw new IOException("Did not read the expected number of blocks.");
        }
    }

    private void beginWriteBlock(int etype) throws IOException {
        prevId = prevRef = 0;
        prevFixedLat = prevFixedLon = 0;
        vout.writeUInt32NoTag(etype);
    }

    /* @param n - the number of entities that were written in this block. */
    private void endWriteBlock(int n) throws IOException {
        vout.writeSInt64NoTag(0L);
        vout.writeUInt32NoTag(n);
    }

    /** Note that the MapDB TreeMap is ordered, so we are writing out the nodes in ID order! */
    private int writeNodeBlock() throws IOException {
        beginWriteBlock(VEX_NODE);
        int n = 0;
        for (Map.Entry<Long, Node> entry : osm.nodes.entrySet()) {
            writeNode(entry.getKey(), entry.getValue());
            n++;
        }
        endWriteBlock(n);
        return n;
    }

    private int writeWayBlock() throws IOException {
        beginWriteBlock(VEX_WAY);
        int n = 0;
        for (Map.Entry<Long, Way> entry : osm.ways.entrySet()) {
            writeWay(entry.getKey(), entry.getValue());
            n++;
        }
        endWriteBlock(n);
        return n;
    }

    private int writeRelationBlock() throws IOException {
        beginWriteBlock(VEX_RELATION);
        int n = 0;
        for (Map.Entry<Long, Relation> entry : osm.relations.entrySet()) {
            writeRelation(entry.getKey(), entry.getValue());
            n++;
        }
        endWriteBlock(n);
        return n;
    }

    /** Write the first elements common to all OSM entities: ID and tags. */
    private void writeTagged(long id, Tagged tagged) throws IOException {
        vout.writeSInt64NoTag(id - prevId);
        prevId = id;
        writeTags(tagged);
    }

    private void writeNode(long id, Node node) throws IOException {
        writeTagged(id, node);
        // plain ints should be fine rather than longs:
        // 2**31 = 2147483648
        // 180e7 = 1800000000.0
        long fixedLat = (long) (node.fixedLat);
        long fixedLon = (long) (node.fixedLon);
        vout.writeSInt64NoTag(prevFixedLat - fixedLat);
        vout.writeSInt64NoTag(prevFixedLon - fixedLon);
        prevFixedLat = fixedLat;
        prevFixedLon = fixedLon;
    }

    private void writeWay(long id, Way way) throws IOException {
        writeTagged(id, way);
        vout.writeUInt32NoTag(way.nodes.length);
        for (long ref : way.nodes) {
            vout.writeSInt64NoTag(prevRef - ref);
            prevRef = ref;
        }
    }

    private void writeRelation(long id, Relation relation) throws IOException {
        writeTagged(id, relation);
        vout.writeUInt32NoTag(relation.members.size());
        for (Relation.Member member : relation.members) {
            vout.writeSInt64NoTag(member.id);
            vout.writeUInt32NoTag(member.type.ordinal()); // FIXME bad, assign specific numbers
            vout.writeStringNoTag(member.role);
        }
    }

    public void writeTags (Tagged tagged) throws IOException {
        List<Tagged.Tag> tags = tagged.getTags();
        vout.writeUInt32NoTag(tags.size());
        for (Tagged.Tag tag : tagged.getTags()) {
            if (tag.value == null) tag.value = "";
            vout.writeStringNoTag(tag.key);
            vout.writeStringNoTag(tag.value);
        }
    }

    public boolean readBlock() throws IOException {
        // Reset delta coding fields
        lat = lon = id = ref = 0;
        int blockType = vin.readUInt32();
        if (blockType == VEX_NONE) return true; // NONE block indicates end of file
        boolean blockEnd = false;
        int nRead = 0;
        while ( ! blockEnd) {
            switch (blockType) {
                case VEX_NODE:
                    blockEnd = readNode();
                    break;
                case VEX_WAY:
                    blockEnd = readWay();
                    break;
                case VEX_RELATION:
                    blockEnd = readRelation();
                    break;
            }
            nRead += 1;
        }
        if (vin.readUInt32() != nRead) {
            throw new IOException("Block length mismatch.");
        }
        return false;
    }

    public String readTags() throws IOException {
        StringBuilder sb = new StringBuilder();
        int nTags = vin.readUInt32();
        for (int i = 0; i < nTags; i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(vin.readString());
            sb.append('=');
            sb.append(vin.readString());
        }
        if (sb.length() == 0) return null;
        return sb.toString();
    }


    public boolean readNode() throws IOException {
        /* Create a new instance each time because we don't know if this is going in a MapDB or a normal Map. */
        Node node = new Node();
        long idDelta = vin.readSInt64();
        id += idDelta;
        if (idDelta == 0) return true;
        node.tags = readTags();
        lat += vin.readSInt64() * 1000000d;
        lon += vin.readSInt64() * 1000000d;
        node.setLatLon(lat, lon);
        osm.nodes.put(id, node);
        return false;
    }

    public boolean readWay() throws IOException {
        /* Create a new instance each time because we don't know if this is going in a MapDB or a normal Map. */
        Way way = new Way();
        long idDelta = vin.readSInt64();
        id += idDelta;
        if (idDelta == 0) return true;
        way.tags = readTags();
        int nNodes = vin.readUInt32();
        way.nodes = new long[nNodes];
        for (int i = 0; i < nNodes; i++) {
            ref += vin.readSInt64();
            way.nodes[i] = ref;
        }
        osm.ways.put(id, way);
        return false;
    }


    public boolean readRelation() throws IOException {
        /* Create a new instance each time because we don't know if this is going in a MapDB or a normal Map. */
        Relation relation = new Relation();
        long idDelta = vin.readSInt64();
        if (idDelta == 0) return true;
        id += idDelta;
        osm.relations.put(id, relation);
        return false;
    }


    /* This should wrap a VexFormatCodec reference rather than being an inner class of it. */
    private class Converter extends Parser {

        private int curr_etype;
        private int ecount;

        // This could be done in the constructor.
        private void streamBegin() throws IOException {
            vout = CodedOutputStream.newInstance(new GZIPOutputStream(new FileOutputStream("/home/abyrd/test.vex")));
            vout.writeRawBytes(HEADER.getBytes());
            curr_etype = VEX_NONE;
            ecount = 0;
        }

        private void streamEnd() throws IOException {
            checkBlockTransition(VEX_NONE);
            vout.flush();
        }

        private void checkBlockTransition(int toEtype) throws IOException {
            if (curr_etype != toEtype) {
                if (curr_etype != VEX_NONE) {
                    endWriteBlock(ecount);
                    if (ecount < 1000) {
                        LOG.warn("Wrote very small block of length {}", ecount);
                    }
                    ecount = 0;
                }
                beginWriteBlock(toEtype);
                curr_etype = toEtype;
            }
        }

        @Override
        public void handleNode(long id, Node node) {
            try {
                checkBlockTransition(VEX_NODE);
                writeNode(id, node);
                ecount++;
            } catch (IOException ex) {
                throw new RuntimeException();
            }
        }

        @Override
        public void handleWay(long id, Way way) {
            try {
                checkBlockTransition(VEX_WAY);
                writeWay(id, way);
                ecount++;
            } catch (IOException ex) {
                throw new RuntimeException();
            }
        }

        @Override
        public void handleRelation(long id, Relation relation) {
            try {
                checkBlockTransition(VEX_RELATION);
                writeRelation(id, relation);
                ecount++;
            } catch (IOException ex) {
                throw new RuntimeException();
            }
        }
    }

    // TODO Need Etype for class, int for etype, etc.

    /** This main method will convert a PBF file to VEX in a streaming manner, without an intermediate datastore. */
    public static void main (String[] args) {
        // final String INPUT = "/var/otp/graphs/ny/new-york-latest.osm.pbf";
        final String INPUT = "/var/otp/graphs/nl/netherlands-latest.osm.pbf";
        // final String INPUT = "/var/otp/graphs/trimet/portland.osm.pbf";
        Converter converter = new VexFormatCodec().new Converter();
        try {
            converter.streamBegin();
            converter.parse(INPUT);
            converter.streamEnd();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
