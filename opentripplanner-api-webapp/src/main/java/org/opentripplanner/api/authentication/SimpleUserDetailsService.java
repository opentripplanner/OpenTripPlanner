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

package org.opentripplanner.api.authentication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class SimpleUserDetailsService implements UserDetailsService {

	private HashMap<String, String> users;

	private static final List<GrantedAuthority> authorities;
	static {
		authorities = new ArrayList<GrantedAuthority>();
		authorities.add(new GrantedAuthorityImpl("user"));
	}

	public class SimpleUserDetails implements UserDetails {
		private static final long serialVersionUID = -6281467878234893438L;
		
		private String username;
		private String password;

		public SimpleUserDetails(String username, String password) {
			this.username = username;
			this.password = password;
		}

		@Override
		public boolean isEnabled() {
			return true;
		}

		@Override
		public boolean isCredentialsNonExpired() {
			return true;
		}

		@Override
		public boolean isAccountNonLocked() {
			return true;
		}

		@Override
		public boolean isAccountNonExpired() {
			return true;
		}

		@Override
		public String getUsername() {
			return username;
		}

		@Override
		public String getPassword() {
			return password;
		}

		@Override
		public Collection<GrantedAuthority> getAuthorities() {
			return authorities;
		}
	};

	@Override
	public UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException, DataAccessException {
		String password = users.get(username);
		return new SimpleUserDetails(username, password);
	}

	public void setUsers(List<String> userStrings) {
		users = new HashMap<String, String>();
		for (String userSpec : userStrings) {
			String[] parts = userSpec.split("=");
			users.put(parts[0], parts[1]);
		}

	}
}
