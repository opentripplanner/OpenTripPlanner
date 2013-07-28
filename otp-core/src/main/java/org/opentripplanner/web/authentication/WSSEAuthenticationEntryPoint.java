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

package org.opentripplanner.web.authentication;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

public class WSSEAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private String realmName;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {
		
        response.addHeader("WWW-Authenticate", "WSSE realm=\"" + realmName + "\", profile=\"UsernameToken\"");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());		
	}

	public void setRealmName(String realmName) {
		this.realmName = realmName;
	}

	public String getRealmName() {
		return realmName;
	}

}
