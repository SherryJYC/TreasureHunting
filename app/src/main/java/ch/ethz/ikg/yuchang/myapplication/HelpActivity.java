package ch.ethz.ikg.yuchang.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;

/**
 *  This activity is used to:
 *      show game rules
 *      social share when exiting app
 */

public class HelpActivity extends AppCompatActivity {

    private Button goMainBtn;
    private ArrayList<Integer> complete_status;
    private int coin_count;
    private String user_id;

    private ArrayList<CollectedTreasure> collection;
    private ArrayList<Double> track;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        goMainBtn = (Button) findViewById(R.id.helpBackMain);

        goMainBtn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helpBackMain(v);
            }
        });

//         read treasure info from MainActivity
//         Those variables (count_coin and complete_status) are used to keep a record for user
//
//         For example, during the game, user may click 'help' to check game rules,
//         when he returns main page from help session, his records should still exist

        Bundle extras = getIntent().getExtras();
        if (extras != null){
            // get treasureItem info
            coin_count = extras.getInt("totalcoin");
            complete_status = extras.getIntegerArrayList("complete_status");
            user_id = extras.getString("user_id");
            collection = (ArrayList<CollectedTreasure>) extras.getSerializable("collection");
            track = (ArrayList<Double>) extras.getSerializable("track");
        }

    }

    /** use this method to:
     * when exiting app, ask whether user wants social sharing
     */
    @Override
    public void onBackPressed() {
        AlertDialog.Builder alertbuilder = new AlertDialog.Builder(HelpActivity.this);
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
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Game Result");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "My Score: " + coin_count + ", My User ID: " + user_id);
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
    /** use this method to:
     * go back to main page (MainActivity)
     * also send back records of user to keep consistancy
     */
    public void helpBackMain(View view){
            Intent goMainIntent = new Intent(view.getContext(), MainActivity.class);
            goMainIntent.putExtra("totalcoin", coin_count);
            goMainIntent.putExtra("user_id", user_id);
            goMainIntent.putExtra("complete_status", complete_status);
            goMainIntent.putExtra("collection",collection);
            goMainIntent.putExtra("track",track);
            goMainIntent.setAction("fromHelp");
            startActivity(goMainIntent);
    }

}
