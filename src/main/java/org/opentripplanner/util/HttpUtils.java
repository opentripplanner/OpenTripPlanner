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

package org.opentripplanner.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class HttpUtils {

    private static final int TIMEOUT_CONNECTION = 5000;
    private static final int TIMEOUT_CONNECTION_REQUEST = 5000;
    private static final int TIMEOUT_SOCKET = 5000;

    public static InputStream getData(String url) throws IOException {
        return getData(url, null, null, TIMEOUT_CONNECTION, TIMEOUT_CONNECTION_REQUEST, TIMEOUT_SOCKET);
    }

    public static InputStream getData(String url, int connectionTimeout, int connectionRequestTimeout, int socketTimeout) throws IOException {
        return getData(url, null, null, connectionTimeout, connectionRequestTimeout, socketTimeout);
    }

    public static InputStream getData(String url, String requestHeaderName, String requestHeaderValue) throws IOException {
        return getData(url, requestHeaderName, requestHeaderValue, TIMEOUT_CONNECTION, TIMEOUT_CONNECTION_REQUEST, TIMEOUT_SOCKET);
    }

    public static InputStream getData(String url, String requestHeaderName, String requestHeaderValue, int connectionTimeout, int connectionRequestTimeout, int socketTimeout) throws IOException {
        HttpGet httpget = new HttpGet(url);
        if (requestHeaderValue != null) {
            httpget.addHeader(requestHeaderName, requestHeaderValue);
        }

        HttpClient httpclient = getClient(connectionTimeout, connectionRequestTimeout, socketTimeout);
        HttpResponse response = httpclient.execute(httpget);
        if (response.getStatusLine().getStatusCode() != 200) {
            return null;
        }

        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return null;
        }
        return entity.getContent();
    }

    public static void testUrl(String url) throws IOException {
        HttpHead head = new HttpHead(url);
        HttpClient httpclient = getClient(0, 0, 0);
        HttpResponse response = httpclient.execute(head);

        StatusLine status = response.getStatusLine();
        if (status.getStatusCode() == 404) {
            throw new FileNotFoundException();
        }

        if (status.getStatusCode() != 200) {
            throw new RuntimeException("Could not get URL: " + status.getStatusCode() + ": "
                    + status.getReasonPhrase());
        }
    }

    private static HttpClient getClient(int connectionTimeout, int connectionRequestTimeout, int socketTimeout) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(connectionTimeout > 0 ? connectionTimeout : TIMEOUT_CONNECTION)
                .setConnectionRequestTimeout(connectionRequestTimeout > 0 ? connectionRequestTimeout : TIMEOUT_CONNECTION_REQUEST)
                .setSocketTimeout(socketTimeout > 0 ? socketTimeout : TIMEOUT_SOCKET)
                .build();

        return HttpClientBuilder.create().setDefaultRequestConfig(config).build();
    }
}
