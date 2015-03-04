package org.opentripplanner.osm.serializer;

import org.mapdb.Serializer;
import org.opentripplanner.osm.Node;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

public class NodeSerializer implements Serializer<Node>, Serializable {

    @Override
    public void serialize(DataOutput out, Node node) throws IOException {
        out.writeInt(node.fixedLat);
        out.writeInt(node.fixedLon);
        VarInt.writeTags(out, node);
    }

    @Override
    public Node deserialize(DataInput in, int available) throws IOException {
        Node node = new Node();
        node.fixedLat = in.readInt();
        node.fixedLon = in.readInt();
        VarInt.readTags(in, node);
        return node;
    }

    @Override
    public int fixedSize() {
        return -1;
    }
    
}
