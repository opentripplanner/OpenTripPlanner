package org.opentripplanner.serializer;

import java.io.InputStream;
import java.io.OutputStream;

public interface GraphSerializer {

    GraphWrapper deserialize(InputStream inputStream);

    void serialize(GraphWrapper graphWrapper, OutputStream outputStream);
}
