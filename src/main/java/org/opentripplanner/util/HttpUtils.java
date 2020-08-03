package org.opentripplanner.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public class HttpUtils {
    
    private static final long TIMEOUT_CONNECTION = 5000;
    private static final int TIMEOUT_SOCKET = 5000;

    private static final Gson GSON = new Gson();

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

    public static <T> T postData(String url, String data, Class<T> mapTo) {
        try {
            HttpPost request = new HttpPost(url);
            request.setEntity(new StringEntity(data, ContentType.APPLICATION_JSON));
            HttpClient client = getClient();
            request.addHeader("content-type", "application/json");
            request.addHeader("accept", "application/json");
            HttpResponse response = client.execute(request);
            String json = EntityUtils.toString(response.getEntity(), "UTF-8");
            return GSON.fromJson(json, mapTo);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        return null;
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
}
