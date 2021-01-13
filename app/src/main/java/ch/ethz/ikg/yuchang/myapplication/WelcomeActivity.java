package ch.ethz.ikg.yuchang.myapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 *  This activity is used to:
 *      show welcome page
 *      let valid user log in
 *      let guest user start as 'guest'
 *      social share when exiting app
 */

public class WelcomeActivity extends AppCompatActivity {

    private Button startBtn;
    private Button startwithID;
    private EditText userID;
    private String user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        userID = (EditText) findViewById(R.id.user_id);
        userID.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                user_id = s.toString();
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                user_id = s.toString();
            }
        });

        // click 'Sign in' to direct to main page with User ID
        startwithID = (Button) findViewById(R.id.login_button);
        startwithID.setOnClickListener( new View.OnClickListener() {
            // click welcome and turn to Main Page
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(v.getContext(), MainActivity.class);
                startIntent.putExtra("user_id", user_id);
                startIntent.setAction("fromWelcome");
                startActivity(startIntent);
            }
        });

        // click 'Start As Guest' to direct to main page without User ID
        startBtn = (Button) findViewById(R.id.welcomeBtn);
        startBtn.setOnClickListener( new View.OnClickListener() {
            // click welcome and turn to Main Page
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(v.getContext(), MainActivity.class);
                startIntent.putExtra("user_id", "guest");
                startIntent.setAction("fromWelcome");
                startActivity(startIntent);
            }
        });

    }
    //  when exiting app, ask whether user wants social sharing
    @Override
    public void onBackPressed() {
        AlertDialog.Builder alertbuilder = new AlertDialog.Builder(WelcomeActivity.this);
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
                shareIntent.putExtra(Intent.EXTRA_TEXT, "My Score: 0, My User ID: " + user_id);
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
}
