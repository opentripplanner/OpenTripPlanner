package org.opentripplanner.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClientBuilder;

public class HttpUtils {
    
    private static final long TIMEOUT_CONNECTION = 5000;
    private static final int TIMEOUT_SOCKET = 5000;

    public static InputStream getData(String url) throws IOException {
        return getData(url, null, null);
    }

    public static InputStream getData(String url, String requestHeaderName, String requestHeaderValue) throws ClientProtocolException, IOException {
        HttpGet httpget = new HttpGet(url);
        if (requestHeaderValue != null) {
            httpget.addHeader(requestHeaderName, requestHeaderValue);
        }
        HttpClient httpclient = getClient();
        HttpResponse response = httpclient.execute(httpget);
        if(response.getStatusLine().getStatusCode() != 200)
            return null;

        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return null;
        }
        return entity.getContent();
    }

    public static void testUrl(String url) throws IOException {
        HttpHead head = new HttpHead(url);
        HttpClient httpclient = getClient();
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
    
    private static HttpClient getClient() {
        HttpClient httpClient = HttpClientBuilder.create()
                .setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(TIMEOUT_SOCKET).build())
                .setConnectionTimeToLive(TIMEOUT_CONNECTION, TimeUnit.MILLISECONDS)
                .build();

        return httpClient;
    }

    public static InputStream getDataFromUrlOrFile (String urlOrFile) throws IOException {
        return getDataFromUrlOrFile(urlOrFile, null, null);
    }

    public static InputStream getDataFromUrlOrFile(
        String urlOrFile,
        String requestHeaderName,
        String requestHeaderValue
    ) throws IOException {
        InputStream data;
        URL url2 = new URL(urlOrFile);

        String proto = url2.getProtocol();
        if (proto.equals("http") || proto.equals("https")) {
            data = getData(urlOrFile, requestHeaderName, requestHeaderValue);
        } else {
            // Local file probably, try standard java
            data = url2.openStream();
        }
        return data;
    }
}
