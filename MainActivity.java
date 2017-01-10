package gpsuploader.source;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


public class MainActivity extends ActionBarActivity {

    TextView LatText;
    TextView LongText;
    TextView Status;
    TextView status3;
    TextView miniStat;
    Button enable;
    Button disable;
    Button upload;
    ProgressBar pb;
    RadioGroup radioGroup;
    RadioButton selectradio;

    RadioButton tempradio;


    float latitude, templat;
    float longitude, templong;
    private LocationManager manager;
    LocationListener listener;
    public Handler handler = new Handler();
    public Handler statushandler = new Handler();

    int i = 0;

    public static class Global {

        public static String message = "";
        public static int x = 0;
        public static String choice = "";
        public static boolean temp = true;

    }


    @Override
    protected void onStart() {
        super.onStart();

        disable.setEnabled(false);
        upload.setEnabled(false);
        //upload.setEnabled(false);
        latitude = 0;
        longitude = 0;
        templat = 0;
        templong = 0;
        Status.setText("-- SCAN AVAILABLE --");
        Status.setTextColor(Color.parseColor("#78FFFD"));
        status3.setText("  PRECISE SCAN [SLOW]");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LatText = (TextView) findViewById(R.id.LatView);
        LongText = (TextView) findViewById(R.id.LongView);
        Status = (TextView) findViewById(R.id.statusview);
        miniStat = (TextView) findViewById(R.id.ministatus);
        pb = (ProgressBar) findViewById(R.id.pb);
        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        status3 = (TextView) findViewById(R.id.status3);

        enable = (Button) findViewById(R.id.ebutton);
        disable = (Button) findViewById(R.id.dbutton);
        upload = (Button) findViewById(R.id.ubutton);


        pb.setVisibility(View.GONE);

        //
        manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                latitude = (float) location.getLatitude();
                longitude = (float) location.getLongitude();

                LatText.setText("  " + Float.toString(latitude));
                LongText.setText("  " + Float.toString(longitude));

                statushandler.removeCallbacks(statusblink);
                Status.setTextColor(Color.parseColor("#5DFC0A"));
                Status.setText("GPS ACQUIRED");
                manager.removeUpdates(listener);

                enable.setEnabled(true);
                disable.setEnabled(false);
                miniStat.setText("");
                Global.x = 0;

                templat = latitude;
                templong = longitude;

                upload.setEnabled(true);


            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String s) {
            }

            @Override
            public void onProviderDisabled(String s) {
            }

        };


        //enable scan
        enable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                Global.x = 0;
                //temporary storage of coordinates

                LatText.setText("");
                LongText.setText("");
                miniStat.setText("");

                latitude = 0;
                longitude = 0;
                //Toast.makeText(getBaseContext(),"" +i,Toast.LENGTH_LONG).show();

                disable.setEnabled(true);
                enable.setEnabled(false);
                upload.setEnabled(false);

                int selected = radioGroup.getCheckedRadioButtonId();
                selectradio = (RadioButton) findViewById(selected);
                Global.choice = selectradio.getText().toString();

                Global.temp = haveNetworkConnection();

                handler.post(timedTask);

                Global.message = "ACQUIRING GPS-FEEDS";
                statushandler.post(statusblink);
                //Toast.makeText(getBaseContext(),"" + Global.message,Toast.LENGTH_LONG).show();
                //Status.setText(Global.message);


            }
        });


        //Disable scan
        disable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Global.x = 0;
                handler.removeCallbacks(timedTask);
                statushandler.removeCallbacks(statusblink);
                manager.removeUpdates(listener);
                //Toast.makeText(getBaseContext(),"" + i,Toast.LENGTH_LONG).show();

                enable.setEnabled(true);
                disable.setEnabled(false);
                miniStat.setText("");

                if (latitude == 0) {
                    Status.setText("-- SCAN AVAILABLE --");
                    Status.setTextColor(Color.parseColor("#78FFFD"));
                    upload.setEnabled(false);
                }
            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (templat == 0 && templong == 0) {
                    Status.setTextColor(Color.parseColor("#FF2917"));
                    Status.setText("NO GPS-FEEDS FOUND");
                } else {

                    String lati = Float.toString(templat);
                    String longi = Float.toString(templong);

                    //login function call
                    String latlong = lati + "&" + longi;

                    new MyAsyncTask().execute(latlong);

                    Global.message = "UPLOADING";
                    statushandler.post(statusblink);
                    pb.setVisibility(View.VISIBLE);

                    upload.setEnabled(false);
                    enable.setEnabled(false);


                }

            }
        });


        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {

                tempradio = (RadioButton) findViewById(i);

                if (tempradio.getText().equals("NETWORK")) {
                    status3.setText("  APPROXIMATION SCAN [FAST]");
                } else {
                    status3.setText("  PRECISE SCAN [SLOW]");
                }
            }
        });


    }

    //RESET
    public void reset() {
        statushandler.removeCallbacks(statusblink);
        miniStat.setText("");
        Status.setTextColor(Color.parseColor("#78FFFD"));
        Status.setText("-- SCAN AVAILABLE --");
        Global.x = 0;
        enable.setEnabled(true);
        disable.setEnabled(false);

        manager.removeUpdates(listener);
        handler.removeCallbacks(timedTask);

    }


    //Scans for location change
    public Runnable timedTask = new Runnable() {

        @Override
        public void run() {

            if (Global.choice.equals("NETWORK")) {
                //status3.setText("Approximation Scan[Fast]");
                if (Global.temp == false) {
                    Status.setTextColor(Color.parseColor("#FF1B19"));
                    Status.setText("SCAN HAS FAILED!");
                    miniStat.setText("No Internet Connection!");
                    statushandler.removeCallbacks(statusblink);
                    enable.setEnabled(true);
                    disable.setEnabled(false);
                    upload.setEnabled(false);


                } else {
                    manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000, 0, listener);
                }
            } else {
                //status3.setText("Precise Scan[Slow]");
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, listener);
            }

        }
    };


    private final Runnable statusblink = new Runnable() {
        int endloop = 0;

        @Override
        public synchronized void run() {

            Global.x++;
            endloop++;

            //Main Status
            if (Global.x % 2 == 0) {
                Status.setText("");
            } else {
                Status.setTextColor(Color.parseColor("#FF7722"));
                Status.setText(Global.message);
            }

            //Mini Status
            if (Global.x == 60) {
                if (Global.message == "ACQUIRING GPS-FEEDS") {
                    miniStat.setText("Satellite Coverage Is Weak");
                } else {
                    miniStat.setText("Waiting for connection");
                }
            } else if (Global.x == 84) {
                if (Global.message == "ACQUIRING GPS-FEEDS") {
                    miniStat.setText("Move to Outdoor");
                } else {
                    miniStat.setText("Internet Connection is Poor");
                }
                Global.x = 24;
            }

            if (endloop == 30) {

                //pending work here----hv to kill thread--
            }


            statushandler.postDelayed(statusblink, 500);

        }


    };


    //Upload gps coordinates to the server

    private class MyAsyncTask extends AsyncTask<String, Integer, Double> {

        int c = 0;
        String x;


        @Override
        protected Double doInBackground(String... params) {
            // TODO Auto-generated method stub

            String latlong = params[0];
            StringTokenizer tokens = new StringTokenizer(latlong, "&");

            String lati = tokens.nextToken();
            String longi = tokens.nextToken();

            //Check for net connection
            Global.temp = haveNetworkConnection();

            if (Global.temp == true) {
                upload(lati, longi);
            }

            return null;
        }


        protected void onPostExecute(Double result) {
            pb.setVisibility(View.GONE);
            handler.removeCallbacks(timedTask);
            statushandler.removeCallbacks(statusblink);
            manager.removeUpdates(listener);

            if (Global.temp == false) {
                Status.setTextColor(Color.parseColor("#FF1B19"));
                Status.setText("UPLOAD FAILED!");
                miniStat.setText("No Internet Connection!");
            } else {
                Status.setTextColor(Color.parseColor("#5DFC0A"));
                Status.setText("UPLOAD SUCCESSFUL!");
            }

            enable.setEnabled(true);
            disable.setEnabled(false);

        }

        protected void onProgressUpdate(Integer... progress) {
            pb.setProgress(progress[0]);
        }

        public void upload(String lati, String longi) {

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://www.bonnyzhub.in/incominggps.php");

            try {
                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                nameValuePairs.add(new BasicNameValuePair("lat", lati));
                nameValuePairs.add(new BasicNameValuePair("long", longi));

                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                Log.w("GPS LOGS", "Execute HTTP Post Request");
                HttpResponse response = httpclient.execute(httppost);


            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
            } catch (IOException e) {
                // TODO Auto-generated catch block
            }


        }


    }

    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }


}
