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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TimeZone;

import org.opentripplanner.api.authentication.WSSEAuthentication.WSSEAuthDetails;
import org.opentripplanner.common.model.T2;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.codec.Base64;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.Assert;

/**
 * WSSE UsernameToken Spring Authentication Provider
 * 
 * @link http://www.xml.com/pub/a/2003/12/17/dive.html
 * 
 */
public class WSSEAuthenticationProvider implements AuthenticationProvider, InitializingBean {

	private Queue<T2<String, Long>> recentlyUsedNonceList;
	private HashSet<String> recentlyUsedNonceSet;
	private UserDetailsService userDetailsService;
	private MessageDigest sha1;

	private static long NONCE_EXPIRATION_MILLIS = 1000 * 60 * 5; // Five minutes

	private SimpleDateFormat dateFormat;

	WSSEAuthenticationProvider() {
	        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		recentlyUsedNonceList = new LinkedList<T2<String, Long>>();
		recentlyUsedNonceSet = new HashSet<String>();
		try {
			sha1 = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(userDetailsService,
				"An userDetailsService must be set");
	}

	@Override
	public Authentication authenticate(Authentication auth)
			throws AuthenticationException {

		String username = auth.getName();

		UserDetails userDetails = userDetailsService.loadUserByUsername(username);
		String password = userDetails.getPassword();

		WSSEAuthentication.WSSEAuthDetails authDetails = (WSSEAuthDetails) auth
				.getDetails();

		String nonce = authDetails.getNonce();

		long now = System.currentTimeMillis();
		// clear out old nonces
		while (!recentlyUsedNonceList.isEmpty()) {
			T2<String, Long> oldNonce = recentlyUsedNonceList.peek();
			if (now - oldNonce.getSecond() > NONCE_EXPIRATION_MILLIS) {
				// expire nonce
				recentlyUsedNonceList.poll();
				recentlyUsedNonceSet.remove(oldNonce.getFirst());
			} else {
				break; // end of expired nonces
			}
		}

		// check for reused nonces
		if (recentlyUsedNonceSet.contains(nonce)) {
			throw new BadCredentialsException("reused nonce");
		}

		String created = authDetails.getCreated();

		// check date
		try {
			Date requestDate = dateFormat.parse(created);
			if (Math.abs(now - requestDate.getTime()) > NONCE_EXPIRATION_MILLIS) {
				throw new BadCredentialsException("Date out of range");
			}
		} catch (ParseException e) {
			throw new BadCredentialsException("Bad date format", e);
		}

		sha1.reset();
		byte[] digest = sha1.digest((nonce + created + password).getBytes());
		byte[] base64Digest = Base64.encode(digest);
		if (!Arrays.equals(base64Digest, authDetails.getPasswordDigest().getBytes())) {
			throw new BadCredentialsException("bad digest");
		}

		recentlyUsedNonceList.add(new T2<String, Long>(nonce, now));
		recentlyUsedNonceSet.add(nonce);
		
		auth.setAuthenticated(true);
		return auth;
	}

	@Override
	public boolean supports(Class<? extends Object> cls) {
		return WSSEAuthentication.class.isAssignableFrom(cls);
	}

	public void setUserDetailsService(UserDetailsService service) {
		this.userDetailsService = service;
	}

	public UserDetailsService getUserDetailsService() {
		return userDetailsService;
	}
}
