package ch.ethz.ikg.yuchang.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.esri.arcgisruntime.ArcGISRuntimeException;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.*;
import com.esri.arcgisruntime.geometry.*;
import com.esri.arcgisruntime.loadable.LoadStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 *  This activity is used to:
 *      show main control page of app
 *      show task list for user to pick
 *      upload treasure collection and track
 *      social share when exiting app
 */

public class MainActivity extends AppCompatActivity {

    private ArrayList<TreasureItem> treasureList;
    private String[] treasureDescribe;
    private int coin_count;
    private TextView user_id_txt;
    private String user_id;
    private ArrayList<Integer> complete_status;
    private TextView totalcoinText;
    private Spinner spinner;
    private ArrayAdapter<String> arrayAdapter;
    private Button helpBtn;
    private Button reviewBtn;
    private Button uploadBtn;

    private ArrayList<CollectedTreasure> collection;
    private ArrayList<Double> track;
    private CheckBox track_check;
    private CheckBox treasure_check;
    private Integer track_id;

    ServiceFeatureTable trackFeatureTable;
    ServiceFeatureTable treasureFeatureTable;

    private static final String TAG = MainActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        totalcoinText = (TextView) findViewById(R.id.totalcoin_main);
        user_id_txt = (TextView) findViewById(R.id.user_id_text);

        // set up click button for help info (game rules)
        helpBtn = (Button) findViewById(R.id.help);
        helpBtn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goHelp(v);
            }
        });
        reviewBtn = (Button) findViewById(R.id.review_btn);
        reviewBtn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goReview(v);
            }
        });
        uploadBtn = (Button) findViewById(R.id.upload_btn);
        track_check = (CheckBox) findViewById(R.id.track_box);
        treasure_check = (CheckBox) findViewById(R.id.treasure_box);

        collection = new ArrayList<CollectedTreasure>();
        track = new ArrayList<Double>();
        track_id = 1;

        // set up listener for track upload
        uploadBtn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadTrack();
            }
        });

        trackFeatureTable = new ServiceFeatureTable(getString(R.string.url_track));
        treasureFeatureTable = new ServiceFeatureTable(getString(R.string.url_treasure));
        trackFeatureTable.loadAsync();
        trackFeatureTable.addDoneLoadingListener(new Runnable() {
            @Override
            public void run() {
                // monitor status of loading
                if (trackFeatureTable.getLoadStatus() == LoadStatus.LOADED){
                    track_check.setChecked(true); // show status
                    setTrackID(); // set trackID from track layer when it is loaded
                }
                else {
                    Toast.makeText(MainActivity.this, getString(R.string.fail_load), Toast.LENGTH_LONG).show();
                    track_check.setChecked(false); // show status
                }
            }
        });
        treasureFeatureTable.loadAsync();
        treasureFeatureTable.addDoneLoadingListener(new Runnable() {
            @Override
            public void run() {
                // monitor status of loading
                if (treasureFeatureTable.getLoadStatus() == LoadStatus.LOADED){
                    treasure_check.setChecked(true);
                }
                else {
                    Toast.makeText(MainActivity.this, getString(R.string.fail_load), Toast.LENGTH_LONG).show();
                    treasure_check.setChecked(false);
                }
            }
        });

        // initialize total count
        coin_count = 0;
        totalcoinText.setText(getString(R.string.totalcoin) + " " +String.valueOf(coin_count));

        // read treasure.csv into ArrayList<TreasureItem>
        treasureList = readCsv();
        complete_status = initCompleteStatus(treasureList.size());
        // convert ArrayList<TreasureItem> into string array (treasureDescribe)
        treasureDescribe = toTreasureDescribe(treasureList);

        // put treasures into Spinner
        spinner = (Spinner) findViewById(R.id.spinner);
        arrayAdapter = new ArrayAdapter<String>(this, R.layout.custom_spinner, treasureDescribe);
        spinner.setAdapter(arrayAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                // skip header (position 0)
                if (position != 0){
                    TreasureItem selectedTreasure = treasureList.get(position-1);
                    // if selected task is not complete, continue to compass page
                    if (selectedTreasure.getComplete() == 0){
                        Intent compassIntent = new Intent(view.getContext(), CompassActivity.class);
                        compassIntent.putExtra("name", selectedTreasure.getName());
                        compassIntent.putExtra("lat", selectedTreasure.getLat());
                        compassIntent.putExtra("lon", selectedTreasure.getLon());
                        compassIntent.putExtra("maxcoin", selectedTreasure.getMaxcoin());
                        compassIntent.putExtra("totalcoin", coin_count);
                        compassIntent.putExtra("idx", position-1);
                        compassIntent.putExtra("complete_status", complete_status);
                        compassIntent.putExtra("user_id", user_id);
                        compassIntent.putExtra("collection",collection);
                        compassIntent.putExtra("track",track);
                        startActivity(compassIntent);
                    }
                    // if selected task is already complete
                    else{
                        Toast toast=Toast.makeText(MainActivity.this,getString(R.string.found_treasure),Toast.LENGTH_SHORT    );
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }
            }
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // get info back from welcome
        String action = getIntent().getAction();
        Bundle extras = getIntent().getExtras();
        if (action.equals("fromWelcome")){
            if (extras != null) {
                user_id = extras.getString("user_id");
                if (user_id != null) {
                    user_id_txt.setText(getString(R.string.user_text) + " " + user_id);
                }
            }
        }
        // get info back from compass
        else if (action.equals("fromCompass"))
        {
            if (extras != null) {
                // get treasureItem info
                coin_count = extras.getInt("totalcoin");
                user_id = extras.getString("user_id");
                if (user_id != null) {
                    user_id_txt.setText(getString(R.string.user_text) + " " + user_id);
                }
                totalcoinText.setText(getString(R.string.totalcoin) + " " + String.valueOf(coin_count));

                // get collected treasures and track from compass activity
                collection = (ArrayList<CollectedTreasure>) extras.getSerializable("collection");
                track = (ArrayList<Double>) extras.getSerializable("track");

                complete_status = extras.getIntegerArrayList("complete_status");
                // change complete status
                treasureList = resetComplete_status(treasureList, complete_status);
                // update spinnder according to complete_status
                treasureDescribe = toTreasureDescribe(treasureList);
                arrayAdapter = new ArrayAdapter<String>(this,
                        R.layout.custom_spinner, treasureDescribe);
                spinner.setAdapter(arrayAdapter);
            }
        }
        else {
            if (extras != null) {
                // get treasureItem info
                coin_count = extras.getInt("totalcoin");
                user_id = extras.getString("user_id");
                if (user_id != null) {
                    user_id_txt.setText(getString(R.string.user_text) + " " + user_id);
                }

                collection = (ArrayList<CollectedTreasure>) extras.getSerializable("collection");
                track = (ArrayList<Double>) extras.getSerializable("track");

                totalcoinText.setText(getString(R.string.totalcoin) + " " + String.valueOf(coin_count));
                complete_status = extras.getIntegerArrayList("complete_status");
                // change complete status
                treasureList = resetComplete_status(treasureList, complete_status);
                // update spinnder according to complete_status
                treasureDescribe = toTreasureDescribe(treasureList);
                arrayAdapter = new ArrayAdapter<String>(this,
                        R.layout.custom_spinner, treasureDescribe);
                spinner.setAdapter(arrayAdapter);
            }
        }

    }

    /** use this method to:
     *  set existing trcakID and make this track ID = max(existing track ID) +1
     *  if no existing track ID, track ID = 1
     *  if no valid user (guest), track ID = -1
     */

    public void setTrackID(){
        // if user is guest, just return -1 as track ID
        if (user_id.equals("guest")){
            track_id = -1;
            return;
        }
        // if valid user, start query existing track ID of user ID
        final ArrayList<Integer> exist_id = new ArrayList<Integer>();
        QueryParameters query = new QueryParameters();
        query.setWhereClause("user_id = "+user_id);
        final ListenableFuture<FeatureQueryResult> future = trackFeatureTable.queryFeaturesAsync(query);
        // add done loading listener to fire when the selection returns
        future.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    // call get on the future to get the result
                    FeatureQueryResult result = future.get();
                    Iterator<Feature> resultIterator = result.iterator();
                    // check if there are some results
                    if (resultIterator.hasNext()) {
                        while (resultIterator.hasNext()) {
                            Feature feature = resultIterator.next();
                            // create a Map of all available attributes as name value pairs
                            Map<String, Object> attr = feature.getAttributes();
                            Integer current_id = (Integer) attr.get("track_id");
                            exist_id.add(current_id);
                        }
                        if (!exist_id.isEmpty()) {
                            track_id = Collections.max(exist_id) + 1;
                        }
                    }
                    // if empty result
                    else {
                        Toast.makeText(MainActivity.this, getString(R.string.unvalid_user), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    String error = getString(R.string.fail_search) + getString(R.string.error) + e.getMessage();
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                    Log.e(TAG, error);
                }
            }
        });

    }

    /** use this method to:
     *  when 2 feature table loaded successfully,
     *  upload treasure collection
     *  upload track
     */
    public void uploadTrack(){
        // check if 2 feature layers are loaded successfully
        if (!track_check.isChecked() || !treasure_check.isChecked()){
            Toast.makeText(this,getString(R.string.wait_load), Toast.LENGTH_LONG).show();
        }
        // check if treasure collection is empty
        if (collection.isEmpty()){
            Toast.makeText(this, getString(R.string.empty_treasure), Toast.LENGTH_LONG).show();
            return;
        }
        // upload collected treasures
        else if (!collection.isEmpty()){
            for (int i=0; i<collection.size();i++){
                CollectedTreasure current = collection.get(i);
                double lon = current.getLon();
                double lat = current.getLat();
                String currentDateTime = current.getTimestamp();
                String treasureName = current.getName();
                Integer treasureCoin = current.getCollectedcoin();

                Point treasure = new Point(lon, lat, SpatialReferences.getWgs84());
                Map<String, Object> attributes = new HashMap<>();
                // timestamp (string), user_id (int), track_id (int), treasure_name (string), collected_coins (double)
                attributes.put("timestamp", currentDateTime);
                // use -1 as user_id for guest user
                if (user_id.equals("guest")) {user_id = String.valueOf(-1);}
                attributes.put("user_id", Integer.valueOf(user_id));
                attributes.put("track_id", track_id);
                attributes.put("treasure_name", treasureName);
                attributes.put("collected_coins", Double.valueOf(treasureCoin));

                addFeature(attributes,treasure,treasureFeatureTable);
                Toast.makeText(this, getString(R.string.ok_load), Toast.LENGTH_LONG).show();
            }
            // empty collection after uploading
            collection = new ArrayList<CollectedTreasure>();
        }
        // check if track is empty
        if (track.isEmpty()){
            Toast.makeText(this, getString(R.string.empty_track), Toast.LENGTH_LONG).show();
            return;
        }
        // upload track
        else if (!track.isEmpty()){
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentDateTime = String.valueOf(System.currentTimeMillis());
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("timestamp", currentDateTime);
            // use 0 as user_id for guest user
            if (user_id.equals("guest")) {user_id = String.valueOf(-1);}
            attributes.put("user_id", Integer.valueOf(user_id));
            attributes.put("track_id", track_id);

            ArrayList<Point> points = new ArrayList<Point>();

            for (int i=0; i<track.size()/2; i++){
                points.add(new Point(track.get(i),track.get(i+1)));
                i++;
            }
            PointCollection pointSet = new PointCollection(points);
            Polyline lines = new Polyline(pointSet, SpatialReferences.getWgs84());

            addFeature(attributes,lines,trackFeatureTable);
            Toast.makeText(this, getString(R.string.ok_upload), Toast.LENGTH_LONG).show();

            track = new ArrayList<Double>();
        }

    }

    /**  function to add features
     *
     * @param attributes: attributes of table
     * @param geom: geometry feature
     * @param featureTable: service feature table (where to upload)
     */
    private void addFeature(Map<String, Object> attributes, Geometry geom, final ServiceFeatureTable featureTable) {

        // Create a new feature from the attributes and an existing point geometry, and then add the feature
        Feature addedFeature = featureTable.createFeature(attributes,geom);
        final ListenableFuture<Void> addFeatureFuture = featureTable.addFeatureAsync(addedFeature);
        addFeatureFuture.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    // check the result of the future to find out if/when the addFeatureAsync call succeeded - exception will be
                    // thrown if the edit failed
                    addFeatureFuture.get();

                    // if using an ArcGISFeatureTable, call getAddedFeaturesCountAsync to check the total number of features
                    // that have been added since last sync

                    // if dealing with ServiceFeatureTable, apply edits after making updates; if editing locally, then edits can
                    // be synchronized at some point using the SyncGeodatabaseTask.
                    if (featureTable instanceof ServiceFeatureTable) {
                        ServiceFeatureTable serviceFeatureTable = (ServiceFeatureTable) featureTable;
                        // apply the edits
                        final ListenableFuture<List<FeatureEditResult>> applyEditsFuture = serviceFeatureTable.applyEditsAsync();
                        applyEditsFuture.addDoneListener(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    final List<FeatureEditResult> featureEditResults = applyEditsFuture.get();
                                    // if required, can check the edits applied in this operation
                                    Toast.makeText(MainActivity.this, getString(R.string.ok_upload), Toast.LENGTH_LONG).show();
                                } catch (InterruptedException | ExecutionException e) {
                                    //dealWithException(e);
                                    Toast.makeText(MainActivity.this, getString(R.string.error), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    }

                } catch (InterruptedException | ExecutionException e) {
                    // executionException may contain an ArcGISRuntimeException with edit error information.
                    if (e.getCause() instanceof ArcGISRuntimeException) {
                        ArcGISRuntimeException agsEx = (ArcGISRuntimeException) e.getCause();
                        Toast.makeText(MainActivity.this, String.format("Add Feature Error %d\n=%s", agsEx.getErrorCode(), agsEx.getMessage()), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, getString(R.string.error), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    /** use this method to:
     * when exiting app, ask whether user wants social sharing
     */
    @Override
    public void onBackPressed() {
        AlertDialog.Builder alertbuilder = new AlertDialog.Builder(MainActivity.this);
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

    /** click 'TRACK REVIEW' to review page
     *
     * @param view: view of current activity
     */
    public void goReview(View view){
        Intent reviewIntent = new Intent(view.getContext(),ReviewActivity.class);
        reviewIntent.putExtra("totalcoin", coin_count);
        reviewIntent.putExtra("user_id", user_id);
        reviewIntent.putExtra("complete_status", complete_status);
        reviewIntent.putExtra("collection",collection);
        reviewIntent.putExtra("track",track);
        startActivity(reviewIntent);
    }

    /** click 'HELP' and go to HelpActivity
     *
     * @param view: view of current activity
     */
    public void goHelp(View view){
        Intent helpIntent = new Intent(view.getContext(), HelpActivity.class);
        helpIntent.putExtra("totalcoin", coin_count);
        helpIntent.putExtra("user_id", user_id);
        helpIntent.putExtra("complete_status", complete_status);
        helpIntent.putExtra("collection",collection);
        helpIntent.putExtra("track",track);
        startActivity(helpIntent);
    }
    // read treasures.csv into ArrayList<TreasureItem>
    public ArrayList<TreasureItem> readCsv() {
        ArrayList<TreasureItem> treasureList = new ArrayList<>();
        try{
            InputStream inputStream = this.getResources().openRawResource(R.raw.treasures);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            // skip header
            String line = bufferedReader.readLine();
            while ((line = bufferedReader.readLine()) != null) {
                String[] treatureRow = line.split(";");
                // read each line and create each line as TreasureItem object
                TreasureItem treasureItem = new TreasureItem(treatureRow[0],
                        Double.parseDouble(treatureRow[2]),
                        Double.parseDouble(treatureRow[1]),
                        Integer.parseInt(treatureRow[3]));
                treasureList.add(treasureItem);
            }
            bufferedReader.close();
            inputStreamReader.close();
            inputStream.close();
        } catch (IOException e){
            e.printStackTrace();
        }
        return treasureList;
    }

    // convert treasure item object list into string array (prepare for display in spinner)
    public String[] toTreasureDescribe(ArrayList<TreasureItem> treasureList){
        if (treasureList == null) return null;
        ArrayList<String> treasureDescribe = new ArrayList<>();
        treasureDescribe.add("Please select one treasure: ");
        for (int i=0; i<treasureList.size(); i++){
            String description = treasureList.get(i).getName() + "(coin: " + treasureList.get(i).getMaxcoin() + ")";
            // check complete status
            if (treasureList.get(i).getComplete() == 1){
                description = description + " COLLECTED";
            }
            treasureDescribe.add(description);
        }
        return treasureDescribe.toArray(new String[0]);
    }
    // initiate complete status list
    public ArrayList<Integer> initCompleteStatus(int totalsize){
        // return a list containing complete status of all TreasureItem object
        ArrayList<Integer> complete_status = new ArrayList<Integer>(Collections.nCopies(totalsize, 0));
        return  complete_status;
    }
    // reset complete status of objects in TreasureList
    public ArrayList<TreasureItem> resetComplete_status(ArrayList<TreasureItem> treasureList, ArrayList<Integer> complete_status){
        for (int i=0; i<treasureList.size(); i++){
            if (complete_status.get(i) == 1){
                treasureList.get(i).setComplete(1);
            }
        }
        return treasureList;
    }

}
