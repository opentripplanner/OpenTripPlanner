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

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;

public class WSSEAuthentication implements Authentication {

	private static final long serialVersionUID = -5713751801681961661L;

	public class WSSEAuthDetails {
		public String getNonce() {
			return nonce;
		}
		public String getCreated() {
			return created;
		}
		public String getPasswordDigest() {
			return passwordDigest;
		}
	}

	private String username;
	private String passwordDigest;
	private String nonce;
	private String created;
	private boolean authenticated;

	public WSSEAuthentication(String username, String passwordDigest, String nonce,
			String created) {
		this.username = username;
		this.passwordDigest = passwordDigest;
		this.nonce = nonce;
		this.created = created;
	}

	@Override
	public Collection<GrantedAuthority> getAuthorities() {
		ArrayList<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		authorities.add(new GrantedAuthorityImpl("user"));
		return authorities;
	}

	@Override
	public Object getCredentials() {
		return null;
	}

	@Override
	public Object getDetails() {
		return new WSSEAuthDetails();
	}

	@Override
	public Object getPrincipal() {
		return null;
	}

	@Override
	public boolean isAuthenticated() {
		return authenticated;
	}

	@Override
	public void setAuthenticated(boolean authenticated) throws IllegalArgumentException {
		this.authenticated = authenticated;
	}

	@Override
	public String getName() {
		return username;
	}
	

}
