package ch.ethz.ikg.yuchang.myapplication;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.*;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.*;

/**
    This activity is used to:
        show direction and distance
        measure temperature and speed
        if treasure is collected, create a CollectedTreasure for it
        social sharing when exiting app

 **/

public class CompassActivity extends AppCompatActivity implements LocationListener, SensorEventListener {
    private String treasureName;
    private double lat;
    private double lon;
    private int maxcoin;
    private int coin_count; // total coin count
    private int coin_count_this; // coin collected by single treasure
    private int treasure_idx;
    private ArrayList<Integer> complete_status;
    private String user_id;

    private ArrayList<CollectedTreasure> collection;
    private ArrayList<Double> track;
    private CollectedTreasure collectedTreasure;

    private List<Geofence> geofences;
    private Map<String, Double> lastDistToCenter;
    private BroadcastReceiver localBroadcastReceiver;
    private SensorManager mSensorManager;
    private Sensor mTempSensor;
    private Sensor mAccelerometer;
    private Sensor mMagnetic;
    private Location targetLocation;
    private LocationManager locationManager;
    private static final String PROX_ALERT_INTENT = "PROXIMITY_ALERT";
    float currentDegree;
    float ambient_temperature;
    ArrayList<Float> speed; // store all 'current_speed' on location change
    float avg_speed; // final average speed
    private float[] accelerometerValues = new float[3];
    private float[] magneticFieldValues = new float[3];
    private float bearing;
    private float azimuth;
    GeomagneticField geoField;

    private TextView treasureNameText;
    private TextView maxcoinText;
    private TextView latText;
    private TextView lonText;
    private TextView distText;
    private TextView bearingText;
    private TextView tempText;
    private TextView speedText;
    private TextView geoText;
    private TextView totalcoinText;
    private Button backBtn;
    private ImageView arrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        // initiate variables for view and button
        treasureNameText = (TextView) findViewById(R.id.treasureNameText);
        maxcoinText = (TextView) findViewById(R.id.maxcoinText);
        latText = (TextView) findViewById(R.id.latText);
        lonText = (TextView) findViewById(R.id.lonText);
        distText = (TextView) findViewById(R.id.distText);
        bearingText = (TextView) findViewById(R.id.bearingText);
        tempText = (TextView) findViewById(R.id.tempText);
        speedText = (TextView) findViewById(R.id.speedText);
        geoText = (TextView) findViewById(R.id.geoText);
        totalcoinText = (TextView) findViewById(R.id.totalcoin);
        backBtn = (Button) findViewById(R.id.backBtn);
        arrow = (ImageView) findViewById(R.id.arrow);

        // initialize speed list to store current speed
        speed = new ArrayList<Float>();

        // set back to main page button
        backBtn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBackMain();
            }
        });

        // prepare for treasure and track
        coin_count_this = 0;
        track = new ArrayList<Double>();
        collection = new ArrayList<CollectedTreasure>();
        collectedTreasure = null;

        // set sensors (temperature, accelerometer and magnetic field)
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        //check permission for temperature sensor
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
            mTempSensor= mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE); // requires API level 14.
        }
        if (mTempSensor == null) {
            tempText.setText("NO TEMP SENSOR");
        }

        // prepare for GPS measurement
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        // initiate orientation
        currentDegree = 0f;
        azimuth = calculateOrientation();

        // read treasure info from MainActivity
        Bundle extras = getIntent().getExtras();
        if (extras != null){
            // get treasureItem info
            treasureName = extras.getString("name");
            lat = extras.getDouble("lat");
            lon = extras.getDouble("lon");
            targetLocation = new Location("");
            targetLocation.setLatitude(lat);
            targetLocation.setLongitude(lon);
            maxcoin = extras.getInt("maxcoin");
            coin_count = extras.getInt("totalcoin");
            treasure_idx = extras.getInt("idx");
            complete_status = extras.getIntegerArrayList("complete_status");
            user_id = extras.getString("user_id");
            collection = (ArrayList<CollectedTreasure>) extras.getSerializable("collection");
            track = (ArrayList<Double>) extras.getSerializable("track");
            // display received info in TextView
            treasureNameText.setText(treasureName);
            maxcoinText.setText(getString(R.string.coin) +"  "+ String.valueOf(maxcoin));
            latText.setText(getString(R.string.lat)+"  " + String.valueOf(lat));
            lonText.setText(getString(R.string.lon)+"  "+String.valueOf(lon));
            totalcoinText.setText(  getString(R.string.totalcoin) +" "+ String.valueOf(coin_count));
        }

        // prepare for geofencing
        geofences = new ArrayList<>();
        lastDistToCenter = new HashMap<>();
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // A local BroadcastReceiver can handle intents sent by the LocationManager Proximity Alert
        // implementation directly inside the Activity.
        localBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String name = intent.getStringExtra("name");
                Boolean entering = intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false);

                if (entering) {
                    geoText.setText("Found Treasure  " + name);
                    // count total coin including bonus
                    // check if treasure is collected already
                    if (maxcoin > 0){
                        coin_count = calculateCoin(maxcoin, ambient_temperature, speed);
                        // add timestamp in attributes
                        String currentDateTime = String.valueOf(System.currentTimeMillis());
                        collectedTreasure = new CollectedTreasure(currentDateTime,treasureName, lon, lat, coin_count_this);
                        collection.add(collectedTreasure);
                    }
                    // and avoid adding coin count or add collectedTreasure repeatedly
                    maxcoin = 0;
                    totalcoinText.setText(getString(R.string.totalcoin) +"  "+ String.valueOf(coin_count));
                    // change complete_status
                    complete_status.set(treasure_idx, 1);


                } else {
                    geoText.setText("Leaving " + name);
                }
            }
        };

    }


    /**
     * use this method to:
     * go back to main page
     */
    private void goBackMain(){

        Intent backIntent = new Intent(this, MainActivity.class);
        backIntent.putExtra("totalcoin", coin_count);
        backIntent.putExtra("name", treasureName);
        backIntent.putExtra("idx", treasure_idx);
        backIntent.putExtra("complete_status", complete_status);
        backIntent.putExtra("user_id", user_id);
        backIntent.putExtra("collection",collection);
        backIntent.putExtra("track",track);
        backIntent.setAction("fromCompass");
        startActivity(backIntent);
    }
    /**
     * use this method to:
     * when exiting app, ask whether user wants social sharing
     */
    @Override
    public void onBackPressed() {
        AlertDialog.Builder alertbuilder = new AlertDialog.Builder(CompassActivity.this);
        alertbuilder.setTitle("Social Sharing");
        alertbuilder.setMessage(getString(R.string.social_sharing));
        alertbuilder.setPositiveButton("yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //close dialog
                dialog.dismiss();

                // sharing via Intent
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT,"Game Result");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "My Score: "+ coin_count + ", My User ID: "+user_id);
                startActivity(Intent.createChooser(shareIntent, "Share via "));

                //if you want to kill app . from other then your main avtivity.(Launcher)
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);

            }
        });
        alertbuilder.setNegativeButton("no", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
        });

        alertbuilder.show();
    }
    /**
     * use this method to:
     *     calculate coin obtained based on coin of treasure, temp, and speed
     *     total coin = coin number + Math.round(avg_speed - 2) * 1 + Math.round(ambient_temperature - 20) * 2
     * @param maxcoin: maxcoin defined in csv (NOT INCLUDE BONUS)
     * @param ambient_temperature: temperature
     * @param speed: arraylist of all saved speed
     * @return
     */
    private int calculateCoin(int maxcoin, float ambient_temperature, ArrayList<Float> speed){
        // compute average speed overall whole route
        float sum_speed = 0;
        for (int i = 1; i<speed.size(); i++){
            sum_speed += speed.get(i);
        }
        avg_speed = sum_speed / speed.size();
        int speed_bonus = Math.round(avg_speed - 2) * 1;
        int temp_bonus = Math.round(ambient_temperature - 20) * 2;
        if (speed_bonus > 0) {
            coin_count += speed_bonus;
            coin_count_this += speed_bonus;
        }
        if (temp_bonus > 0) {
            coin_count += temp_bonus;
            coin_count_this += temp_bonus;
        }
        if (speed_bonus > 0 || temp_bonus > 0 ){
            Toast.makeText(this, getString(R.string.get_bonus), Toast.LENGTH_LONG).show();
        }
        coin_count += maxcoin;
        coin_count_this += maxcoin;
        return coin_count;
    }

    /**
     * use this method to:
     * calculate orientation of sensor at initial stage (Reference: https://blog.csdn.net/yywan1314520/article/details/52123899)
     */
    private float calculateOrientation(){
        float[] values = new float[3];
        float[] R = new float[9];
        SensorManager.getRotationMatrix(R, null, accelerometerValues,
                magneticFieldValues);
        SensorManager.getOrientation(R, values);
        values[0] = (float) Math.toDegrees(values[0]); // azimuth, angle from magnetic north to current orientation
        if (values[0]<0){values[0]+=360;}
        return values[0];
    }

    /**
     * use this method to:
     * rotate arrow image based on direction (bearing and azimuth)
     *
     *  azimuth: azimuth of current location based on magnetic north
     *  bearing: bearing to target location based on true north
     *  geoField.getDeclination: deviation between magnetic north and true north
     *  direction: angle between current location and target location
     */
    public void rotateArrowImage() {
        azimuth -= geoField.getDeclination(); // convert from magnetic north to true north used in bearing
        float direction = bearing - azimuth - 180;
        RotateAnimation rotateAnimation = new RotateAnimation(currentDegree, direction, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(200);
        arrow.startAnimation(rotateAnimation);
        rotateAnimation.setFillAfter(true);
        currentDegree = direction;
    }

    /**
     * following functions are for location services
     */
    @Override
    protected void onStart() {
        // set geofence
        int radius = 30;
        addProximityAlert(treasureName, lat, lon, radius);
        super.onStart();
        // check permissions
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(this,
                                    Manifest.permission.ACCESS_COARSE_LOCATION) ==
                                    PackageManager.PERMISSION_GRANTED){

                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, this);
                    }

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_LONG).show();
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // add location to track
        track.add(location.getLongitude());
        track.add(location.getLatitude());
        // bearing = from current location to target location in North-East, w.r.t true north
        bearing = targetLocation.bearingTo(location);
        if (bearing < 0) { bearing+=360; }
        // prepare to convert magnetic north to true north for azimuth
        geoField = new GeomagneticField( Double
                .valueOf( location.getLatitude() ).floatValue(), Double
                .valueOf( location.getLongitude() ).floatValue(),
                Double.valueOf( location.getAltitude() ).floatValue(),
                System.currentTimeMillis() );
        // calculate ratation of arrow image
        azimuth = calculateOrientation(); // from magnetic north to current orientation [0,360]
        rotateArrowImage();

        Float distance = targetLocation.distanceTo(location);
        // get current speed
        float current_speed = location.getSpeed();
        // store it in speed list
        speed.add(current_speed);
        // show all info in TextView
        speedText.setText(String.valueOf(current_speed) +" "+ getResources().getString(R.string.speed_unit));
        bearingText.setText(String.valueOf(bearing) +" "+ getResources().getString(R.string.bear_unit));
        distText.setText(distance.toString() +" "+ getResources().getString(R.string.dist_unit));

        // geofence
        // For all the geofences, we check if we're entering of leaving them.
        for (Geofence g : geofences) {
            // In case the new distance is smaller than the radius of the fence, and
            // the old one is bigger, we are entering the geofence.
            if (distance < g.getRadius() && lastDistToCenter.get(g.getName()) > g.getRadius()) {
                sendProximityIntent(g.getName(), true);
            } else if (distance > g.getRadius() && lastDistToCenter.get(g.getName()) < g.getRadius()) {
                // In the opposite case, we must be leaving the geofence.
                sendProximityIntent(g.getName(), false);
            }
            lastDistToCenter.put(g.getName(), Double.parseDouble(String.valueOf(distance))); // convert float to double
        }

    }
    @Override
    protected void onStop() {
        geofences = new ArrayList<>();
        lastDistToCenter = new HashMap<>();
        locationManager.removeUpdates(this);
        mSensorManager.unregisterListener(this);
        super.onStop();
    }
    /*
       functions for temperature sensor and localBroadcastReceiver
   */
    // get temperature, accelerator and magnetic filed (for azimuth)
    public void onSensorChanged(SensorEvent event) {
        // monitor changes of 3 different sensors
        if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE){
            ambient_temperature = event.values[0];
            tempText.setText(String.valueOf(ambient_temperature) +" "+ getResources().getString(R.string.temp_unit));
        }
        else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            accelerometerValues = event.values;
        }
        else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            magneticFieldValues = event.values;
        }

    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    protected void onResume() {
        super.onResume();
        registerReceiver(localBroadcastReceiver, new IntentFilter(PROX_ALERT_INTENT));
        mSensorManager.registerListener(this, mTempSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this,mAccelerometer,SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this,mMagnetic,SensorManager.SENSOR_DELAY_GAME);
    }

    protected void onPause() {
        super.onPause();
        unregisterReceiver(localBroadcastReceiver);
        mSensorManager.unregisterListener(this);
    }

    /*
       functions for geofencing
    */
    private void addProximityAlert(String name, double lat, double lon, int radius) {
        try {
            geofences.add(new Geofence(name, lat, lon, radius));
            lastDistToCenter.put(name, Double.MAX_VALUE);

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,5,this);

        } catch (SecurityException e) {
            Log.d("ERROR", e.getMessage());
        }
    }
    /**
     * use this method to send an intent stating that we are in proximity of a certain object,
     * denoted by its "name". The boolean passed along tells us if we are entering of leaving
     * the proximity.
     *
     * @param name     The name of the proximity area.
     * @param entering True if we're entering, false otherwise.
     */
    private void sendProximityIntent(String name, boolean entering) {
        Intent i = new Intent(PROX_ALERT_INTENT);
        i.putExtra("name", name);
        i.putExtra(LocationManager.KEY_PROXIMITY_ENTERING, entering);
        this.sendBroadcast(i);
    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}

