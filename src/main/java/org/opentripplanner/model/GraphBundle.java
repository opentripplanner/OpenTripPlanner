package org.opentripplanner.model;

import java.io.File;
import java.io.Serializable;

/** 
 * This is a bean that just holds the path of a serialized graph 
 */
public final class GraphBundle implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private File path;
    
    public GraphBundle() {
        
    }
    
    public GraphBundle(File path) {
        this.path = path;
    }
    
    public void setPath(File path) {
        this.path = path;
    }
    
    public File getPath() {
        return path;
    }
    
    public File getGraphPath() {
        return new File(path,"Graph.obj");
    }

    public File getExtraClassPath() {
        return new File(path, "extraClasses");
    }
}
