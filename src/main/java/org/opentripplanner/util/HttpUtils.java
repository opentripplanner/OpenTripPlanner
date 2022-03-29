package org.opentripplanner.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

public class HttpUtils {
    
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    public static InputStream getData(URI uri) throws IOException {
        return getData(uri, null);
    }

    public static InputStream getData(String uri) throws IOException {
        return getData(URI.create(uri));
    }

    public static InputStream getData(String uri, Map<String, String> headers) throws IOException {
        return getData(URI.create(uri), headers);
    }

    public static InputStream getData(
        URI uri, Duration timeout, Map<String, String> requestHeaderValues
    ) throws IOException {

        var to = (int) timeout.toMillis();
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(to)
                .setConnectTimeout(to)
                .setConnectionRequestTimeout(to)
                .build();

        HttpGet httpget = new HttpGet(uri);
        httpget.setConfig(requestConfig);

        if (requestHeaderValues != null) {
            for (Map.Entry<String, String> entry : requestHeaderValues.entrySet()) {
                httpget.addHeader(entry.getKey(), entry.getValue());
            }
        }

        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpResponse response = httpclient.execute(httpget);
        if(response.getStatusLine().getStatusCode() != 200) {
            return null;
        }

        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return null;
        }
        return entity.getContent();
    }

    public static InputStream getData(URI uri, Map<String, String> requestHeaderValues) throws IOException {
        return getData(uri, DEFAULT_TIMEOUT, requestHeaderValues);
    }

    public static InputStream openInputStream(String url, Map<String, String> headers) throws IOException {
        return openInputStream(URI.create(url), headers);
    }
    public static InputStream openInputStream(URI uri, Map<String, String> headers) throws IOException {
        URL downloadUrl = uri.toURL();
        String proto = downloadUrl.getProtocol();
        if (proto.equals("http") || proto.equals("https")) {
            return HttpUtils.getData(uri, headers);
        } else {
            // Local file probably, try standard java
            return downloadUrl.openStream();
        }
    }

}
