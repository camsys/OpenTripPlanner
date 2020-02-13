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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.eclipse.jetty.util.StringUtil;

public class HttpUtils {
    
    private static final long TIMEOUT_CONNECTION = 5000;
    private static final int TIMEOUT_SOCKET = 5000;

    private static final String SCHEME_FILE = "file";

    public static InputStream getData(String url) throws IOException {
        return getData(url, null, null);
    }

    public static InputStream getData(String url, String requestHeaderName, String requestHeaderValue) throws ClientProtocolException, IOException {
        return getDataWithAuthentication(url, requestHeaderName, requestHeaderValue, null, null);
    }

    public static InputStream getData(String url, String requestHeaderName, String requestHeaderValue, String username, String password) throws IOException {
        return getDataWithAuthentication(url, requestHeaderName, requestHeaderValue, username, password);
    }

    public static InputStream getDataWithAuthentication(String url, String requestHeaderName, String requestHeaderValue,
            String username, String password) throws ClientProtocolException, IOException {
        URI uri = URI.create(url);
        if (SCHEME_FILE.equals(uri.getScheme())) {
            return FileUtils.openInputStream(new File(uri.getPath()));
        }
        HttpGet httpget = new HttpGet(uri);
        if (requestHeaderValue != null) {
            httpget.addHeader(requestHeaderName, requestHeaderValue);
        }

        HttpClient httpclient = getClient(username, password);

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
        return getClient(null, null);
    }

    private static HttpClient getClient(String username, String password) {
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, (int) TIMEOUT_CONNECTION);
        HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_SOCKET);
        DefaultHttpClient httpclient = new DefaultHttpClient();

        if(StringUtil.isNotBlank(username) &&
                StringUtil.isNotBlank(password))
        {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            httpclient.setCredentialsProvider(credentialsProvider);
        }

        httpclient.setParams(httpParams);

        return httpclient;
    }
}
