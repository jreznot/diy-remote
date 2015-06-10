/*
 * Copyright (c) 2013, DIY-Remote Contributors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * Neither the name of the StrangeWay.org nor the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.strangeway.diyremote.scripting;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author Yuriy Artamonov
 */
public class HttpUtils {

    public static String get(String url) {
        HttpGet request = new HttpGet(url);
        HttpClient client = HttpClientBuilder.create().build();

        StringBuilder contentBuilder = new StringBuilder();
        processRequest(request, client, contentBuilder);

        return contentBuilder.toString();
    }

    public static String get(String url, String username, String password) {
        HttpGet request = new HttpGet(url);

        if (username == null)
            username = "";

        if (password == null)
            password = "";

        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        Credentials credentials = new UsernamePasswordCredentials(username, password);
        credentialsProvider.setCredentials(AuthScope.ANY, credentials);

        HttpClient client = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build();

        StringBuilder contentBuilder = new StringBuilder();
        processRequest(request, client, contentBuilder);

        return contentBuilder.toString();
    }

    private static void processRequest(HttpGet request, HttpClient client, StringBuilder contentBuilder) {
        try {
            HttpResponse result = client.execute(request);
            if (result.getStatusLine().getStatusCode() == 200) {
                InputStream content = result.getEntity().getContent();

                BufferedReader rd = new BufferedReader(new InputStreamReader(content));

                String line;
                while ((line = rd.readLine()) != null) {
                    contentBuilder.append(line).append("\n");
                }
            }
        } catch (Exception ignored) {
        }
    }
}