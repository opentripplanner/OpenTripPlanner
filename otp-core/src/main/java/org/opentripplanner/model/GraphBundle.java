/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.model;

import java.io.File;
import java.io.Serializable;

/** 
 * This is a bean that just holds the path of a serialized graph 
 */
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

    public File getExtraClassPath() {
        return new File(_path, "extraClasses");
    }
}
