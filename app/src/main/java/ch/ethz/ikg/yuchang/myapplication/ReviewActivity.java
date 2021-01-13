package ch.ethz.ikg.yuchang.myapplication;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.*;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.*;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 *  This activity is used to:
 *      show list of all user_id
 *      show tracks of selected user_id
 *      show information of clicked track
 *      social share when exiting app
 */

public class ReviewActivity extends AppCompatActivity {
    private static final String TAG = ReviewActivity.class.getSimpleName();
    private MapView mMapView;
    private Callout mCallout;
    private ServiceFeatureTable mServiceFeatureTable;
    Spinner userIDSpinner;
    ArrayAdapter<String> userIDAdapter;
    private String[] all_user_id;
    private ArrayList<Integer> complete_status;
    private int coin_count;
    private String user_id;
    private String selected_user_id;
    private Button returnBtn;
    private Button searchBtn;

    private ArrayList<CollectedTreasure> collection;
    private ArrayList<Double> track;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        returnBtn = (Button) findViewById(R.id.review_returnBtn);
        returnBtn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helpBackMain(v);
            }
        });
        searchBtn = (Button) findViewById(R.id.search_button);
        searchBtn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchTrack(v);
            }
        });

        // set up placeholder for spinner before successful loading
        final ArrayList<String> all_user_id = new ArrayList<>();
        all_user_id.add(getString(R.string.wait_load));
        userIDSpinner = (Spinner) findViewById(R.id.spinner_review);
        userIDAdapter = new ArrayAdapter<String>(ReviewActivity.this, R.layout.custom_spinner, all_user_id);
        userIDSpinner.setAdapter(userIDAdapter);

        // get info from main page
        Bundle extras = getIntent().getExtras();
        if (extras != null){
            // get treasureItem info
            coin_count = extras.getInt("totalcoin");
            complete_status = extras.getIntegerArrayList("complete_status");
            user_id = extras.getString("user_id");
            collection = (ArrayList<CollectedTreasure>) extras.getSerializable("collection");
            track = (ArrayList<Double>) extras.getSerializable("track");
        }
        // get reference to map view
        mMapView = findViewById(R.id.mapView);
        // create a map with the topographic basemap
        final ArcGISMap map = new ArcGISMap(Basemap.Type.TOPOGRAPHIC, 47.408992,8.507847,  15);
        // set the map to the map view
        mMapView.setMap(map);
        mCallout = mMapView.getCallout();
        mServiceFeatureTable = new ServiceFeatureTable(getString(R.string.url_track));
        mServiceFeatureTable.loadAsync();
        mServiceFeatureTable.addDoneLoadingListener(new Runnable() {
            @Override
            public void run() {
                if (mServiceFeatureTable.getLoadStatus() == LoadStatus.LOADED){
                    Toast.makeText(ReviewActivity.this, getString(R.string.ok_load), Toast.LENGTH_LONG).show();
                }
                else {
                    Toast.makeText(ReviewActivity.this, getString(R.string.fail_load), Toast.LENGTH_LONG).show();
                }
            }
        });
        // set up spinner of user_id by querying track feature table
        setUserIDSpinner();

        // set an on touch listener to listen for click events
        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                // remove any existing callouts
                if (mCallout.isShowing()) {
                    mCallout.dismiss();
                }
                // get the point that was clicked and convert it to a point in map coordinates
                final Point clickPoint = mMapView.screenToLocation(new android.graphics.Point(Math.round(e.getX()), Math.round(e.getY())));
                // create a selection tolerance
                double tolerance = 0.1;
                double mapTolerance = tolerance * mMapView.getUnitsPerDensityIndependentPixel();
                // use tolerance to create an envelope to query
                Envelope envelope = new Envelope(clickPoint.getX() - mapTolerance, clickPoint.getY() - mapTolerance,
                        clickPoint.getX() + mapTolerance, clickPoint.getY() + mapTolerance, map.getSpatialReference());
                QueryParameters query = new QueryParameters();
                query.setGeometry(envelope);
                if (selected_user_id!=null){ query.setWhereClause("user_id ="+selected_user_id);}
                // request all available attribute fields
                final ListenableFuture<FeatureQueryResult> future = mServiceFeatureTable.queryFeaturesAsync(query,
                        ServiceFeatureTable.QueryFeatureFields.LOAD_ALL);
                // add done loading listener to fire when the selection returns
                future.addDoneListener(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //call get on the future to get the result
                            FeatureQueryResult result = future.get();
                            // create an Iterator
                            Iterator<Feature> iterator = result.iterator();
                            // create a TextView to display field values
                            TextView calloutContent = new TextView(getApplicationContext());
                            calloutContent.setTextColor(Color.BLACK);
                            calloutContent.setSingleLine(false);
                            calloutContent.setVerticalScrollBarEnabled(true);
                            calloutContent.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);

                            calloutContent.setMovementMethod(new ScrollingMovementMethod());
                            calloutContent.setLines(5);
                            calloutContent.append("=== Nearby Tracks ===\n");
                            // cycle through selections
                            Feature feature;
                            while (iterator.hasNext()) {
                                feature = iterator.next();
                                // create a Map of all available attributes as name value pairs
                                Map<String, Object> attr = feature.getAttributes();
                                Set<String> keys = attr.keySet();
                                for (String key : keys) {
                                    Object value = attr.get(key);
                                    if (value instanceof GregorianCalendar) {
                                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
                                        value = simpleDateFormat.format(((GregorianCalendar) value).getTime());
                                    }
                                    // append name value pairs to TextView
                                    if (key.equals("track_id") || key.equals("user_id") || key.equals("CreationDate") ){
                                        calloutContent.append(key + " : " + value + "\n");
                                    }
                                }
                                calloutContent.append("=== === === === ===\n");
                                // center the mapview on selected feature
                                Envelope envelope = feature.getGeometry().getExtent();
                                mMapView.setViewpointGeometryAsync(envelope, 200);
                                // show CallOut
                                mCallout.setLocation(clickPoint);
                                mCallout.setContent(calloutContent);
                                mCallout.show();
                            }
                        } catch (Exception e) {
                            Log.e(getResources().getString(R.string.app_name), "Select feature failed: " + e.getMessage());
                        }
                    }
                });
                return super.onSingleTapConfirmed(e);
            }
        });


    }

    /** use this method to:
     *  get all user_id by feature query
     *  put all user_id in spinner
     *
     */
    public void setUserIDSpinner(){
        QueryParameters query = new QueryParameters();
        query.setWhereClause("user_id >= 0"); // guest user with user_id = -1 is not included
        final ListenableFuture<FeatureQueryResult> future = mServiceFeatureTable.queryFeaturesAsync(query);
        // add done loading listener to fire when the selection returns
        future.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    // call get on the future to get the result
                    FeatureQueryResult result = future.get();
                    // check there are some results
                    Iterator<Feature> resultIterator = result.iterator();
                    final ArrayList<String> all_user_id = new ArrayList<>();
                    all_user_id.add("User ID: ");
                    while (resultIterator.hasNext()) {
                        // get the extent of the first feature in the result to zoom to
                        Feature feature = resultIterator.next();
                        Map<String, Object> attributes = feature.getAttributes();
                        String current_user_id = attributes.get("user_id").toString();
                        if (all_user_id.contains(current_user_id)) continue;
                        all_user_id.add(current_user_id);
                    }

                    all_user_id.toArray(new String[0]);
                    userIDSpinner = (Spinner) findViewById(R.id.spinner_review);
                    userIDAdapter = new ArrayAdapter<String>(ReviewActivity.this, R.layout.custom_spinner, all_user_id);
                    userIDSpinner.setAdapter(userIDAdapter);
                    userIDSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
                    {
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                        {   // skip header of spinner
                            if (position != 0){
                                selected_user_id = all_user_id.get(position);
                            }

                        }
                        public void onNothingSelected(AdapterView<?> parent) { }
                    });
                } catch (Exception e) {
                    String error = getString(R.string.fail_search) + e.getMessage();
                    Toast.makeText(ReviewActivity.this, error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, error);
                }
            }});

    }

    /**
     * this method is used to:
     *  search for tracks of selected user id
     * @param view
     */
    public void searchTrack(View view){
        displayTracks(selected_user_id);

    }


    /** display tracks based on selected user_id
     *
     * @param selected_user_id: (String) picked user id from spinner
     */
    public void displayTracks(String selected_user_id){
        // before showing tracks, remove previous tracks displayed
        mMapView.getGraphicsOverlays().clear();
        // before showing tracks, remove any existing callouts
        if (mCallout.isShowing()) {
            mCallout.dismiss();
        }
        // start query
        QueryParameters query = new QueryParameters();
        query.setWhereClause("user_id ="+selected_user_id);
        final ListenableFuture<FeatureQueryResult> future = mServiceFeatureTable.queryFeaturesAsync(query);
        future.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    // call get on the future to get the result
                    FeatureQueryResult result = future.get();
                    // check there are some results
                    Iterator<Feature> resultIterator = result.iterator();
                    // create a TextView to display field values

                    while (resultIterator.hasNext()) {
                        Feature feature = resultIterator.next();
                        // get geometry
                        Geometry track = feature.getGeometry();
                        if (track == null) continue;
                        // set track as graphic on map
                        final GraphicsOverlay overlay = new GraphicsOverlay();
                        mMapView.getGraphicsOverlays().add(overlay);
                        SimpleLineSymbol routeSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.DOT, Color.BLUE, 4);
                        Graphic routeGraphic = new Graphic(track, routeSymbol);
                        overlay.getGraphics().add(routeGraphic);
                        Envelope envelope = feature.getGeometry().getExtent();
                        mMapView.setViewpointGeometryAsync(envelope, 50);
                    }

                } catch (Exception e) {
                    String error = getString(R.string.fail_search) + e.getMessage();
                    Toast.makeText(ReviewActivity.this, error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, error);
                }
            }});

    }

    /** use this method to:
     * when exiting app, ask whether user wants social sharing
     */
    @Override
    public void onBackPressed() {
        AlertDialog.Builder alertbuilder = new AlertDialog.Builder(ReviewActivity.this);
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

                //if you want to kill app . from other then your main activity.(Launcher)
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);

                //if you want to finish just current activity

                //yourActivity.this.finish();
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

    /** go back to main page (MainActivity)
     *  also send back records of user to keep consistancy
     */
    public void helpBackMain(View view){
        Intent goMainIntent = new Intent(view.getContext(), MainActivity.class);
        goMainIntent.putExtra("totalcoin", coin_count);
        goMainIntent.putExtra("user_id", user_id);
        goMainIntent.putExtra("complete_status", complete_status);
        goMainIntent.putExtra("collection",collection);
        goMainIntent.putExtra("track",track);
        goMainIntent.setAction("fromReview");
        startActivity(goMainIntent);
    }

}
