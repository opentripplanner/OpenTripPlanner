package org.opentripplanner.model;

import java.io.File;
import java.io.Serializable;

public final class GraphBundle implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private File _path;
    
    public GraphBundle() {
        
    }
    
    public GraphBundle(File path) {
        _path = path;
    }
    
    public void setPath(File path) {
        _path = path;
    }
    
    public File getPath() {
        return _path;
    }
    
    public File getGraphPath() {
        return new File(_path,"Graph.obj");
    }
}
