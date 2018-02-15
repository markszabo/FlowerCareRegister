package hu.szabo_simon.mark.flowercareregister;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.net.Proxy;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    final int REQUEST_CODE = 1;
    final int MAX_RETRY = 5;
    OkHttpClient client = new OkHttpClient();
    TextView logmessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button BtnSearchDevice = (Button) findViewById(R.id.BtnSearchDevice);
        BtnSearchDevice.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                Intent i = new Intent(view.getContext(), SearchDevicesActivity.class);
                startActivityForResult(i, REQUEST_CODE);
            }
        });

        Button BtnRegister = (Button) findViewById(R.id.btn_register_device);
        logmessages = (TextView) findViewById(R.id.log_messages);

        BtnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                EditText devicemac = (EditText) findViewById(R.id.devicemac);
                devicemac.setText(selected_address);
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }

    public class OkHttpHandler extends AsyncTask {

        OkHttpClient client = new OkHttpClient();

        @Override
        protected String doInBackground(Object[] params) {
            boolean loginSuccessful = false;
            do {
                boolean ChineseProxySet = false;
                do {
                    client = new OkHttpClient();
                    //Get proxy
                    Proxy proxy = getChineseProxy();
                    publishProgress("Chinese proxy aquired: " + proxy.toString());

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
                            publishProgress("Chinese proxy properly set, our IP is now " + proxyip + " in " + location);
                        } else if (location != "") {
                            publishProgress("The proxy is not in China but in " + location + " retrying...");
                        } else {
                            publishProgress("Could not set Chinese proxy, retrying...");
                        }
                    }
                } while (!ChineseProxySet); //TODO add limit/timeout

                //Login
                EditText usernameET = findViewById(R.id.username);
                EditText passwordET = findViewById(R.id.password);
                String username = usernameET.getText().toString();
                String password = passwordET.getText().toString();

                String loginJson = "{\"method\":\"GET\",\"path\":\"/v2/token/email\",\"data\":{\"extra\":{\"app_channel\":\"google\",\"country\":\"US\",\"zone\":1,\"lang\":\"en\",\"version\":\"ASI_3021_4.3.0\",\"phone\":\"Xiaomi_2014813_22\",\"position\":[null,null],\"model\":\"\"},\"password\":" + JSONObject.quote(password) + ",\"email\":" + JSONObject.quote(username) + "},\"service\":\"auth\"}";
                Log.d("loginJson", loginJson);

                MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                RequestBody loginBody = RequestBody.create(JSON, loginJson);

                JSONObject loginResults = getJSON("https://api.huahuacaocao.net/api", loginBody);
                String status = "";
                String description = "";
                try {
                    status = loginResults.getString("status");
                    description = loginResults.getString("description");
                    loginSuccessful = true;
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            if(!status.equals("100")) {
                publishProgress("Login failed: " + description + ". Please double check your username and password");
                return "";
            }
            }while(!loginSuccessful);
            publishProgress("Login successful");

            //Register device
            return "";
        }

        @Override
        protected void onProgressUpdate(Object... s) {
            logmessages.append(s[0].toString() + "\n");
        }

        @Override
        protected void onPostExecute(Object s) {
            super.onPostExecute(s);
            logmessages.append("PostExecute");
        }

        private Proxy getChineseProxy() {
            Proxy p = null;
            while(p == null) {//TODO add limit/timeout
                String url = "https://gimmeproxy.com/api/getProxy?country=CN&protocol=http";
                JSONObject json = getJSON(url);
                String hostname;
                Integer port;
                try {
                    hostname = json.getString("ip");
                    port = Integer.parseInt(json.getString("port"));
                    p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(hostname, port));
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
            return p;
        }

        private String getUrl(String url, RequestBody postBody) {
            Request.Builder builder = new Request.Builder();
            builder.url(url);
            if(postBody != null)
                builder.post(postBody);
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
                }catch (Exception e){
                    e.printStackTrace();
                }
            } //TODO handle if still unsuccessful after MAX_RETRY
            return resp;
        }

        private JSONObject getJSON(String url) {
            return getJSON(url, null);
        }
        private JSONObject getJSON(String url, RequestBody postBody) {
            try {
                return new JSONObject(getUrl(url, postBody));
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
