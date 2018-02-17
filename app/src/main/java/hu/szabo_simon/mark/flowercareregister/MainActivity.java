package hu.szabo_simon.mark.flowercareregister;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    final int REQUEST_CODE = 1;
    final int MAX_RETRY = 3;
    OkHttpClient client = new OkHttpClient();
    TextView logmessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.findViewById(android.R.id.content).setKeepScreenOn(true); //prevent from going to sleep while app is running

        displayProgressBarAndRegisterButton(false);

        Button BtnSearchDevice = findViewById(R.id.BtnSearchDevice);
        BtnSearchDevice.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                Intent i = new Intent(view.getContext(), SearchDevicesActivity.class);
                startActivityForResult(i, REQUEST_CODE);
            }
        });

        Button BtnRegister = findViewById(R.id.btn_register_device);
        logmessages = findViewById(R.id.log_messages);

        BtnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayProgressBarAndRegisterButton(true);
                logmessages.setText("");

                OkHttpHandler okHttpHandler= new OkHttpHandler();
                okHttpHandler.execute();

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_CODE) {
            if(resultCode == Activity.RESULT_OK){
                String selected_address=data.getStringExtra("mac_address");
                EditText devicemac = findViewById(R.id.devicemac);
                devicemac.setText(selected_address);
            }
        }
    }

    private void displayProgressBarAndRegisterButton(final boolean enable) {
        ProgressBar progressbar = findViewById(R.id.registering_progress_bar);
        Button registerBtn = findViewById(R.id.btn_register_device);
        if (enable) {
            progressbar.setVisibility(View.VISIBLE);
            registerBtn.setVisibility(View.GONE);
        } else {
            progressbar.setVisibility(View.GONE);
            registerBtn.setVisibility(View.VISIBLE);
        }
    }

    private class OkHttpHandler extends AsyncTask<Void, String, Void> {

        OkHttpClient client = new OkHttpClient();

        @Override
        protected Void doInBackground(Void... voids) {
            //Check the internet connection
            if(!isOnline()) {
                publishProgress("No Internet connection is available. Please connect to wifi or mobile data.");
                return null;
            }

            String x_auth_token = "";

            boolean loginSuccessful = false;
            for(int i=0; i<MAX_RETRY && !loginSuccessful; i++) {
                boolean ChineseProxySet = false;
                for(int j=0; j<MAX_RETRY && !ChineseProxySet; j++) {
                    client = new OkHttpClient();
                    //Get proxy
                    Proxy proxy = getChineseProxy();
                    if(proxy == null) {
                        publishProgress("Failed to get Chinese proxy. Please try again.");
                        return null;
                    }
                    publishProgress("Chinese proxy acquired: " + proxy.toString());

                    //Set proxy
                    OkHttpClient.Builder builder = new OkHttpClient.Builder().proxy(proxy);
                    client = builder.build();

                    //Test proxy
                    JSONObject resp = getJSON("http://www.ip-api.com/json");
                    if (resp == null) {
                        publishProgress("Could not set Chinese proxy, retrying...");
                    } else {
                        String countryCode = "";
                        String proxyip = "";
                        String location = "";
                        try {
                            countryCode = resp.getString("countryCode");
                            proxyip = resp.getString("query");
                            location = resp.getString("city") + ", " + resp.getString("regionName") + ", " + resp.getString("country");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (countryCode.equals("CN")) {
                            ChineseProxySet = true;
                            publishProgress("Chinese proxy set, our IP is now " + proxyip + " in " + location);
                        } else if (!location.equals("")) {
                            publishProgress("The proxy is not in China but in " + location + " retrying...");
                        } else {
                            publishProgress("Could not set Chinese proxy, retrying...");
                        }
                    }
                }
                if(!ChineseProxySet) {
                    publishProgress("Failed to set Chinese proxy, try again later.");
                    return null;
                }

                //Login
                EditText usernameET = findViewById(R.id.username);
                EditText passwordET = findViewById(R.id.password);
                String username = usernameET.getText().toString();
                String password = passwordET.getText().toString();

                String loginJson = "{\"method\":\"GET\",\"path\":\"/v2/token/email\",\"data\":{\"extra\":{\"app_channel\":\"google\",\"country\":\"US\",\"zone\":1,\"lang\":\"en\",\"version\":\"ASI_3021_4.3.0\",\"phone\":\"Xiaomi_2014813_22\",\"position\":[null,null],\"model\":\"\"},\"password\":" + JSONObject.quote(password) + ",\"email\":" + JSONObject.quote(username) + "},\"service\":\"auth\"}";

                MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                RequestBody loginBody = RequestBody.create(JSON, loginJson);

                JSONObject loginResults = getJSON("https://api.huahuacaocao.net/api", loginBody);
                String status = "";
                String description = "";
                try {
                    status = loginResults.getString("status");
                    description = loginResults.getString("description");
                } catch (JSONException | NullPointerException e) {
                    publishProgress("Failed to login, probably due to bad proxy. Retrying...");
                    e.printStackTrace();
                    continue;
                }
                if(!status.equals("") && !status.equals("100")) {
                    publishProgress("Login failed: " + description + ". Please double check your username and password");
                    return null;
                }
                try {
                    x_auth_token = loginResults.getJSONObject("data").getString("token");
                    loginSuccessful = true;
                } catch (JSONException | NullPointerException e) {
                    publishProgress("Failed to get x-auth-token, probably due to bad proxy. Retrying...");
                    e.printStackTrace();
                }
            }
            publishProgress("Login successful");

            //Register device
            EditText MACaddressET = findViewById(R.id.devicemac);
            String deviceMAC = MACaddressET.getText().toString();
            String deviceRegJson = "{\"method\":\"POST\",\"path\":" + JSONObject.quote("/v2/device/" + deviceMAC) + ",\"data\":{\"extra\":{\"app_channel\":\"google\",\"country\":\"US\",\"zone\":1,\"lang\":\"en\",\"version\":\"\",\"phone\":\"\",\"position\":[null,null],\"model\":\"hhcc.plantmonitor.v1\"},\"sn\":\"\",\"name\":\"Flower care\",\"battery\":100,\"model\":\"hhcc.plantmonitor.v1\"},\"service\":\"ble\"}";
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody deviceRegBody = RequestBody.create(JSON, deviceRegJson);

            JSONObject deviceRegResults = getJSON("https://api.huahuacaocao.net/api", deviceRegBody, x_auth_token);
            String status = "";
            String description = "";
            try {
                status = deviceRegResults.getString("status");
                description = deviceRegResults.getString("description");
            } catch (JSONException | NullPointerException e) {
                e.printStackTrace();
            }
            if(status.equals("100")) {
                publishProgress("Device successfully added");
            } else {
                publishProgress("Error while adding device: " + description);
                publishProgress("Please try again");
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... s) {
            logmessages.append(s[0] + "\n");
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            displayProgressBarAndRegisterButton(false);
        }

        private Proxy getChineseProxy() {
            Proxy p = null;
            for(int i=0; p == null && i < MAX_RETRY; i++) {
                String url = "https://gimmeproxy.com/api/getProxy?country=CN&protocol=http";
                JSONObject json = getJSON(url);
                String hostname;
                Integer port;
                try {
                    hostname = json.getString("ip");
                    port = Integer.parseInt(json.getString("port"));
                    p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(hostname, port));
                } catch (JSONException | NullPointerException e) {
                    e.printStackTrace();
                }
            }
            return p;
        }

        private String getUrl(String url, RequestBody postBody, String x_auth_token) {
            Request.Builder builder = new Request.Builder();
            builder.url(url);
            if(postBody != null)
                builder.post(postBody);
            if(x_auth_token != null)
                builder.addHeader("x-auth-token", x_auth_token);

            Request request = builder.build();
            String resp = "";
            boolean success = false;
            int retries = 0;
            while (!success && retries <= MAX_RETRY) {
                retries++;
                try {
                    Response response = client.newCall(request).execute();
                    resp = response.body().string();
                    success = true;
                } catch (IOException | NullPointerException e) {
                    e.printStackTrace();
                }
            }
            return resp;
        }

        private JSONObject getJSON(String url) {
            return getJSON(url, null, null);
        }
        private JSONObject getJSON(String url, RequestBody postBody) {
            return getJSON(url, postBody, null);
        }
        private JSONObject getJSON(String url, RequestBody postBody, String x_auth_token) {
            try {
                return new JSONObject(getUrl(url, postBody, x_auth_token));
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

        private boolean isOnline() { //from: https://stackoverflow.com/a/27312494/8590802
            try {
                int timeoutMs = 1500;
                Socket sock = new Socket();
                SocketAddress sockaddr = new InetSocketAddress("8.8.8.8", 53);

                sock.connect(sockaddr, timeoutMs);
                sock.close();

                return true;
            } catch (IOException e) { return false; }
        }
    }
}

