/* 
 Copyright 2010, Patrick Grimaud.    
 
 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 
 (Patrick gave me explicit permission to use this code under the LGPL via email. -- novalis)
 
*/

package org.opentripplanner.jsonp;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
 
import javax.servlet.ServletOutputStream;
 
public class FilterServletOutputStream extends ServletOutputStream {
 
    private DataOutputStream stream;
 
    public FilterServletOutputStream(OutputStream output) {
        stream = new DataOutputStream(output);
    }
 
    public void write(int b) throws IOException {
        stream.write(b);
    }
 
    public void write(byte[] b) throws IOException {
        stream.write(b);
    }
 
    public void write(byte[] b, int off, int len) throws IOException {
        stream.write(b, off, len);
    }
 
}
