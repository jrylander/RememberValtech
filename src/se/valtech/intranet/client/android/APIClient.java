package se.valtech.intranet.client.android;

import android.util.Log;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class APIClient {
    private final APIResponseParser parser;
    private final DefaultHttpClient httpClient;

    public APIClient(String username, String password, APIResponseParser parser) {
        this.parser = parser;
        DefaultHttpClient client = new DefaultHttpClient();
        ClientConnectionManager mgr = client.getConnectionManager();
        HttpParams params = client.getParams();
        httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(params, mgr.getSchemeRegistry()), params);
        httpClient.getCredentialsProvider().setCredentials(new AuthScope("intranet.valtech.se", 443),
                                                       new UsernamePasswordCredentials(username, password));
    }

    public boolean authenticate() {
        HttpGet request = new HttpGet("https://intranet.valtech.se/api/employees/");
        try {
            HttpResponse response = httpClient.execute(request);
            StatusLine status = response.getStatusLine();
            Log.i("RememberValtech", "status.getStatusCode() = " + status.getStatusCode());
            Log.i("RememberValtech", "status.getReasonPhrase() = " + status.getReasonPhrase());
            return status.getStatusCode() == 200;

        } catch (IOException e) {
            Log.w("RememberValtech", "Could not authenticate", e);
            return false;
        }
    }

    public synchronized List<Employee> getEmployees() {
        HttpGet request = new HttpGet("https://intranet.valtech.se/api/employees/");
        String data = execRequest(request);
        return parser.parseEmployees(data);
    }

    public synchronized void download(String path, ByteArrayOutputStream out) throws IOException {
        HttpGet request = new HttpGet("https://intranet.valtech.se" + path);
        Log.d("RememberValtech", "Downloading " + request.getURI());
        HttpResponse response = httpClient.execute(request);
        if (response.getStatusLine().getStatusCode() != 200) {
            response.getEntity().consumeContent();
            throw new IOException("Could not download path " + path + ", got status code " + response.getStatusLine().getStatusCode());
        }
        response.getEntity().writeTo(out);
    }

    private String execRequest(HttpUriRequest request) {
        try {
            HttpResponse response = httpClient.execute(request);

            StatusLine status = response.getStatusLine();
            if (status.getStatusCode() != 200) {
                throw new RuntimeException("Invalid response from server: " + status.toString());
            }

            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();

            int readBytes;
            byte[] sBuffer = new byte[512];
            ByteArrayOutputStream content = new ByteArrayOutputStream();
            while ((readBytes = inputStream.read(sBuffer)) != -1) {
                content.write(sBuffer, 0, readBytes);
            }

            return new String(content.toByteArray());
        } catch (IOException e) {
            Log.w("RememberValtech", "Problem communicating with API", e);
            throw new RuntimeException("Problem communicating with API", e);
        }
    }
}
