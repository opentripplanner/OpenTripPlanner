package org.opentripplanner.updater;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class HttpUtils {

    public static InputStream getData(String url) throws ClientProtocolException, IOException {
        HttpGet httpget = new HttpGet(url);
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return null;
        }
        InputStream instream = entity.getContent();
        return instream;
    }
}
