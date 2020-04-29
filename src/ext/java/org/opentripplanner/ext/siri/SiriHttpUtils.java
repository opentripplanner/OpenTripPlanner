package org.opentripplanner.ext.siri;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.opentripplanner.util.HttpUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SiriHttpUtils extends HttpUtils {

    public static InputStream postData(
        String url,
        String xmlData,
        int timeout,
        Map<String, String> headers
    ) throws IOException {

        HttpPost httppost = new HttpPost(url);
        if (xmlData != null) {
            httppost.setEntity(new StringEntity(xmlData, ContentType.APPLICATION_XML));
        }
        org.apache.http.client.HttpClient httpclient = getClient(timeout, timeout);

        HttpResponse response = httpclient.execute(httppost);
        if(response.getStatusLine().getStatusCode() != 200)
            return null;

        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return null;
        }
        return entity.getContent();
    }

    /**
     * Gets a unique ET-Client-Name HTTP header for this instance of OTP.
     */
    public static String getUniqueETClientName(String postFix) {
        String hostname = System.getenv("HOSTNAME");
        if (hostname == null) {
            hostname = "otp-"+ UUID.randomUUID().toString();
        }
        return hostname + postFix;
    }

    private static HttpClient getClient(int socketTimeout, int connectionTimeout) {
        HttpClient httpClient = HttpClientBuilder.create()
                .setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(socketTimeout).build())
                .setConnectionTimeToLive(connectionTimeout, TimeUnit.MILLISECONDS)
                .build();

        return httpClient;
    }
}
