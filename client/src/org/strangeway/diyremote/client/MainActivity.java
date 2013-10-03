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

package org.strangeway.diyremote.client;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.strangeway.diyremote.Action;
import org.strangeway.diyremote.Command;
import org.strangeway.diyremote.R;
import org.strangeway.diyremote.Result;

import java.io.*;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Yuriy Artamonov
 */
public class MainActivity extends Activity {

    public static final int HTTP_STATUS_OK = 200;

    private View startPane;
    private TextView statusLabel;
    private Timer updateStatusTimer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);

        setContentView(R.layout.main);

        getActionBar().setDisplayShowTitleEnabled(true);
        getActionBar().setHomeButtonEnabled(false);

        final ImageButton startButton = (ImageButton) findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new UpdateActionsAsyncTask().execute();
            }
        });

        startPane = findViewById(R.id.startPane);
        statusLabel = (TextView) findViewById(R.id.statusLabel);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Context context = getApplicationContext();
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);

        boolean hasUrl = prefs.contains("connection.url") && (prefs.getString("connection.url", null) != null);
        boolean hasValidUrl = hasUrl;
        if (hasUrl) {
            try {
                new URL(prefs.getString("connection.url", null));
            } catch (MalformedURLException e) {
                hasValidUrl = false;
            }
        }

        if (!hasValidUrl) {
            // open preferences
            Intent settingsActivity = new Intent(getBaseContext(),
                    PreferencesActivity.class);
            startActivity(settingsActivity);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (updateStatusTimer != null) {
            updateStatusTimer.cancel();
            updateStatusTimer = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionPreferences:
                Intent settingsActivity = new Intent(getBaseContext(),
                        PreferencesActivity.class);
                startActivity(settingsActivity);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class UpdateActionsAsyncTask extends AsyncTask<Void, Void, List<Action>> {

        @Override
        protected List<Action> doInBackground(Void... params) {
            Context context = getApplicationContext();
            SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(context);

            String serviceUrlString = prefs.getString("connection.url", null);
            if (serviceUrlString != null) {
                if (!serviceUrlString.endsWith("/"))
                    serviceUrlString += "/";
                HttpGet listGet = new HttpGet(serviceUrlString + "action/list");
                HttpClient client = new DefaultHttpClient();
                try {
                    HttpResponse listResponse = client.execute(listGet);
                    if (listResponse.getStatusLine().getStatusCode() == HTTP_STATUS_OK) {
                        InputStream content = listResponse.getEntity().getContent();

                        // Get the response
                        BufferedReader rd = new BufferedReader(new InputStreamReader(content));
                        StringBuilder contentBuilder = new StringBuilder();

                        String line;
                        while ((line = rd.readLine()) != null) {
                            contentBuilder.append(line).append("\n");
                        }

                        Type listType = new TypeToken<ArrayList<Action>>() {
                        }.getType();

                        Gson gson = new Gson();

                        return gson.fromJson(contentBuilder.toString(), listType);
                    }
                } catch (IOException e) {
                    return null;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Action> actions) {
            if (actions == null) {
                statusLabel.setText("Unable to get actions");
                return;
            }

            // all is ok, replace start pane
            LinearLayout contentPane = (LinearLayout) findViewById(R.id.contentPane);
            contentPane.removeView(startPane);

            LinearLayout buttonsLayout = new LinearLayout(getApplicationContext());
            buttonsLayout.setGravity(Gravity.CENTER);

            TableLayout buttonsTable = new TableLayout(getApplicationContext());
            TableRow currentTableRow = new TableRow(getApplicationContext());

            TableRow.LayoutParams buttonMarginParams = new TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT,
                    TableRow.LayoutParams.WRAP_CONTENT
            );
            int marginPx = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());
            buttonMarginParams.setMargins(marginPx, marginPx, marginPx, marginPx);

            for (final Action action : actions) {
                ImageButton actionButton = new ImageButton(getApplicationContext());
                int drawableId = getResources().getIdentifier(action.icon, "drawable", getPackageName());
                actionButton.setImageDrawable(getResources().getDrawable(drawableId));
                actionButton.setContentDescription(action.description);
                actionButton.setLayoutParams(buttonMarginParams);
                actionButton.setBackgroundResource(R.drawable.button);
                actionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new PerformActionAsyncTask().execute(action.name);
                    }
                });

                currentTableRow.addView(actionButton);
                if (currentTableRow.getChildCount() == 3) {
                    buttonsTable.addView(currentTableRow);
                    currentTableRow = new TableRow(getApplicationContext());
                }
            }

            if (currentTableRow.getChildCount() > 0) {
                buttonsTable.addView(currentTableRow);
            }

            buttonsLayout.addView(buttonsTable);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            contentPane.addView(buttonsLayout, params);

            runStatusUpdateTimer();
        }

        private void runStatusUpdateTimer() {
            // run timed update status
            final Handler handler = new Handler();
            updateStatusTimer = new Timer();
            TimerTask doAsynchronousTask = new TimerTask() {
                @Override
                public void run() {
                    handler.post(new Runnable() {
                        public void run() {
                            try {
                                new UpdateStatusAsyncTask().execute();
                            } catch (Exception ignored) {
                                updateStatusTimer.cancel();
                            }
                        }
                    });
                }
            };
            updateStatusTimer.schedule(doAsynchronousTask, 0, 30000); //execute in every 30 s
        }
    }

    private class UpdateStatusAsyncTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            Context context = getApplicationContext();
            SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(context);

            String serviceUrlString = prefs.getString("connection.url", null);
            if (serviceUrlString != null) {
                if (!serviceUrlString.endsWith("/"))
                    serviceUrlString += "/";

                HttpPost statusPost = new HttpPost(serviceUrlString + "action/status");
                HttpClient client = new DefaultHttpClient();

                try {
                    HttpResponse statusResult = client.execute(statusPost);
                    if (statusResult.getStatusLine().getStatusCode() == HTTP_STATUS_OK) {
                        InputStream content = statusResult.getEntity().getContent();

                        // Get the response
                        BufferedReader rd = new BufferedReader(new InputStreamReader(content));
                        StringBuilder contentBuilder = new StringBuilder();

                        String line;
                        while ((line = rd.readLine()) != null) {
                            contentBuilder.append(line).append("\n");
                        }
                        Gson gson = new Gson();

                        Result result = gson.fromJson(contentBuilder.toString(), Result.class);
                        return result.message;
                    }
                } catch (IOException e) {
                    return null;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if (s == null) {
                s = "Unable to update status";
            }

            statusLabel.setText(s);
        }
    }

    private class PerformActionAsyncTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            Context context = getApplicationContext();
            SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(context);

            String serviceUrlString = prefs.getString("connection.url", null);
            if (serviceUrlString != null) {
                if (!serviceUrlString.endsWith("/"))
                    serviceUrlString += "/";

                Command command = new Command();
                command.actionName = params[0];

                Gson gson = new Gson();
                String commandContent = gson.toJson(command);

                HttpPost executePost = new HttpPost(serviceUrlString + "action/execute");
                BasicHttpEntity httpEntity = new BasicHttpEntity();
                httpEntity.setContentType("application/json");
                try {
                    httpEntity.setContent(new ByteArrayInputStream(commandContent.getBytes("UTF-8")));
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }

                executePost.setEntity(httpEntity);
                HttpClient client = new DefaultHttpClient();

                try {
                    HttpResponse executeResult = client.execute(executePost);
                    if (executeResult.getStatusLine().getStatusCode() == HTTP_STATUS_OK) {
                        InputStream content = executeResult.getEntity().getContent();

                        // Get the response
                        BufferedReader rd = new BufferedReader(new InputStreamReader(content));
                        StringBuilder contentBuilder = new StringBuilder();

                        String line;
                        while ((line = rd.readLine()) != null) {
                            contentBuilder.append(line).append("\n");
                        }

                        Result result = gson.fromJson(contentBuilder.toString(), Result.class);
                        return result.message;
                    }
                } catch (IOException e) {
                    return null;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if (s != null && !"".equals(s)) {
                statusLabel.setText(s);
            }
        }
    }
}