package za.co.twinc.a9letterjumble;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;

import za.co.twinc.a9letterjumble.billing.BillingManager;
import za.co.twinc.a9letterjumble.billing.BillingProvider;
import za.co.twinc.a9letterjumble.skulist.AcquireFragment;

import static za.co.twinc.a9letterjumble.billing.BillingManager.BILLING_MANAGER_NOT_INITIALIZED;


public class MainActivity extends AppCompatActivity implements BillingProvider {

    private final int SHARE_INTENT = 1;

    public static final String MAIN_PREFS = "main_app_prefs";

    private InterstitialAd mInterstitialAd;

    private BillingManager mBillingManager;
    private AcquireFragment mAcquireFragment;
    private MainViewController mViewController;
    private View mScreenWait, mScreenMain;

    private Sounds mySounds;
    private static AlarmReceiver alarmReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Check for dark mode in settings
        SharedPreferences settingsPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (settingsPref.getBoolean(SettingsActivity.KEY_PREF_DARK, false))
            setTheme(R.style.AppThemeDark);
        else
            setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialise interstitial ad
        MobileAds.initialize(this, getString(R.string.app_id));
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.ad_unit_id_interstitial));

        // Start the controller and load data
        mViewController = new MainViewController(this);
        // Create and initialize BillingManager which talks to BillingLibrary
        mBillingManager = new BillingManager(this, mViewController.getUpdateListener());
        mScreenWait = findViewById(R.id.screen_wait);
        mScreenMain = findViewById(R.id.screen_main);

        // Initialise the media player
        mySounds = new Sounds();

        // Check if we launched with intent
        Intent startMain = getIntent();
        if (startMain != null) {
            if (startMain.getBooleanExtra("challenge", false))
                onChallengeClicked(null);

            else if (startMain.getBooleanExtra("store", false)){
                TapTargetView.showFor(this,                 // `this` is an Activity
                        TapTarget.forView(findViewById(R.id.buttonStore),
                                getString(R.string.store_tip_title),
                                getString(R.string.store_tip_description))
                                // All options below are optional
                                .outerCircleColor(R.color.colorAccent)   // Specify a color for the outer circle
                                .transparentTarget(true)
                                .dimColor(android.R.color.black)            // If set, will dim behind the view with 30% opacity of the given color
                                .drawShadow(true)                           // Whether to draw a drop shadow or not
                                .titleTextColor(android.R.color.white)
                                .descriptionTextColor(android.R.color.black),
                        new TapTargetView.Listener() {          // The listener can listen for regular clicks, long clicks or cancels
                            @Override
                            public void onTargetClick(TapTargetView view) {
                                super.onTargetClick(view);
                                onStoreClicked(view);
                            }
                        });
            }
        }

        SharedPreferences mainLog = getSharedPreferences(MainActivity.MAIN_PREFS, 0);

        // Show onboarding the first time you open the game
        if (mainLog.getBoolean("show_intro", true)){
            Intent startIntro = new Intent(this, IntroActivity.class);
            startActivity(startIntro);
            mainLog.edit().putBoolean("show_intro", false).apply();
        }

        // Give hint to share
        if (mainLog.getBoolean("show_share_tip", true)) {
            if (mainLog.getInt("games_unlocked", 0) >= 2) {
                TapTargetView.showFor(this,                 // `this` is an Activity
                        TapTarget.forView(findViewById(R.id.buttonShare),
                                getString(R.string.share_tip_title),
                                getString(R.string.share_tip_description))
                                // All options below are optional
                                .outerCircleColor(R.color.colorAccent)   // Specify a color for the outer circle
                                .transparentTarget(true)
                                .dimColor(android.R.color.black)            // If set, will dim behind the view with 30% opacity of the given color
                                .drawShadow(true)                           // Whether to draw a drop shadow or not
                                .titleTextColor(android.R.color.white)
                                .descriptionTextColor(android.R.color.black),
                        new TapTargetView.Listener() {          // The listener can listen for regular clicks, long clicks or cancels
                            @Override
                            public void onTargetClick(TapTargetView view) {
                                super.onTargetClick(view);
                                onShareClicked(view);
                            }
                        });
                // Don't show this tip again
                mainLog.edit().putBoolean("show_share_tip", false).apply();
            }
        }

        // Set alarmReceiver for notifications
        setNotification();

        // Check if the sound icon should show muted
        if (mainLog.getFloat("volume", 0.5f) == 0f)
            ((ImageButton)findViewById(R.id.buttonVolume)).setImageResource(R.drawable.ic_volume_off);

    }

    @Override
    public void onResume(){
        super.onResume();
        if (mInterstitialAd.isLoaded())
            mInterstitialAd.show();
        //Check for dark mode background image
        SharedPreferences settingsPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (settingsPref.getBoolean(SettingsActivity.KEY_PREF_DARK, false))
            mScreenMain.setBackground(getResources().getDrawable(R.drawable.alphabet_dark));
        else
            mScreenMain.setBackground(getResources().getDrawable(R.drawable.alphabet));
    }

    @Override
    public BillingManager getBillingManager() {
        return mBillingManager;
    }

    @Override
    public boolean isPremiumPurchased() {
        return mViewController.isPremiumPurchased();
    }

    void onBillingManagerSetupFinished() {
        if (mAcquireFragment != null) {
            mAcquireFragment.onManagerReady(this);
        }
    }

    /**
     * Enables or disables the "please wait" screen.
     */
    private void setWaitScreen(@SuppressWarnings("SameParameterValue") boolean set) {
        mScreenMain.setVisibility(set ? View.GONE : View.VISIBLE);
        mScreenWait.setVisibility(set ? View.VISIBLE : View.GONE);
    }

    private boolean isAcquireFragmentShown() {
        return mAcquireFragment != null && mAcquireFragment.isVisible();
    }

    /**
     * Remove loading spinner and refresh the UI
     */
    public void showRefreshedUi() {
        setWaitScreen(false);
        if (mAcquireFragment != null) {
            mAcquireFragment.refreshUI();
        }
    }

    @Override
    public void onDestroy(){
        if (mBillingManager != null) {
            mBillingManager.destroy();
        }
        super.onDestroy();
    }


    public void onPlayClicked(View view){
        mySounds.playClick(getApplicationContext());
        Intent intent = new Intent(getApplicationContext(), SelectActivity.class);
        startActivity(intent);
    }

    public void onChallengeClicked(View view){
        mySounds.playClick(getApplicationContext());
        SharedPreferences mainLog = getSharedPreferences(MainActivity.MAIN_PREFS, 0);

        // Get the current day, setting hours and minutes to zero
        Calendar cal = Calendar.getInstance();
        long timeNow = System.currentTimeMillis();
        cal.setTimeInMillis(timeNow);
        cal.set(Calendar.HOUR_OF_DAY,0);
        cal.set(Calendar.MINUTE,0);

        // Check for only one play per day
        if (mainLog.getLong("last_challenge", 0L) > cal.getTimeInMillis() - 10*60*1000){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.button_challenge)
                    .setMessage(R.string.challenge_come_again)
                    .setPositiveButton(android.R.string.ok, null);
            builder.create().show();
            return;
        }

        // We are playing a challenge so load an ad now
        if (!isPremiumPurchased()) {
            mInterstitialAd.loadAd(new AdRequest.Builder()
                    .addTestDevice("5F2995EE0A8305DEB4C48C77461A7362")
                    .build());
        }

        // Get the index of the game that should be served
        final int challengeNum = mainLog.getInt("num_challenge", 0);

        // Save today as last challenge played and update the number for the next challenge
        SharedPreferences.Editor editor = mainLog.edit();
        editor.putLong("last_challenge", cal.getTimeInMillis());
        editor.putInt("num_challenge", challengeNum+1);
        editor.apply();

        // Explain the rules on first play only
        if (challengeNum == 0){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.button_challenge);
            builder.setMessage(R.string.challenge_message);

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    startChallenge(challengeNum);
                }
            });
            builder.create().show();
        }

        else {
            startChallenge(challengeNum);
        }
    }


    public void onStoreClicked(View view){
        mySounds.playClick(getApplicationContext());
        openStore();
    }

    public void onAdsButtonClicked(View view){
        mySounds.playClick(getApplicationContext());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.ads_title);
        builder.setMessage(R.string.ads_msg);

        builder.setPositiveButton(R.string.remove_ads, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                openStore();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {  }
        });
        builder.create().show();
    }

    public void onRateClicked(View view){
        mySounds.playClick(getApplicationContext());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        @SuppressLint("InflateParams")
            View dialogView = inflater.inflate(R.layout.rate_request, null);
        SharedPreferences settingsPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (settingsPref.getBoolean(SettingsActivity.KEY_PREF_DARK, false)) {
            TextView textViewMessage = dialogView.findViewById(R.id.rate_request_message);
            textViewMessage.setTextColor(this.getResources().getColor(android.R.color.white));
        }
        builder.setView(dialogView)
                .setPositiveButton(R.string.review, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        SharedPreferences mainLog = getSharedPreferences(MainActivity.MAIN_PREFS, 0);
                        mainLog.edit().putBoolean("show_rate_prompt", false).apply();

                        Intent goToMarket = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=" + getPackageName()));
                        startActivity(goToMarket);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) { }
                }).create().show();
    }

    public void onShareClicked(View view){
        mySounds.playClick(getApplicationContext());
        shareApp();
    }

    public void onVolumeClicked(View view){
        mySounds.playClick(getApplicationContext());

        final SharedPreferences mainLog = getSharedPreferences(MainActivity.MAIN_PREFS, 0);
        float floatVolume = mainLog.getFloat("volume", 0.5f);

        final ImageButton button = findViewById(R.id.buttonVolume);
        if (floatVolume == 0f)
            button.setImageResource(R.drawable.ic_volume_off);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.sfx_volume);

        SeekBar seekBar = new SeekBar(this);
        seekBar.setProgress((int)(100*floatVolume/0.5));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int vol;
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                vol = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float floatVolume = 0.5f*(vol/100f);
                if (floatVolume == 0f)
                    button.setImageResource(R.drawable.ic_volume_off);
                else
                    button.setImageResource(R.drawable.ic_volume_up);
                mainLog.edit().putFloat("volume", floatVolume).apply();

                mySounds.playDing(getApplicationContext());

            }
        });

        builder.setView(seekBar).create().show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu); //your file name
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_share:
                shareApp();
                return true;
            case R.id.menu_feedback:
                FeedbackClass feedbackClass = new FeedbackClass(this);
                feedbackClass.showFeedbackDialog();
                return true;
            case R.id.menu_why_ads:
                openStore();
                return true;
            case R.id.menu_how_to:
                Intent startIntro = new Intent(this, IntroActivity.class);
                startActivity(startIntro);
                return true;
            case R.id.menu_settings:
                Intent startSettings = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(startSettings);
                return true;
            case R.id.menu_privacy_policy:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://sites.google.com/view/twincapps-privacypolicy/home"));
                startActivity(browserIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Make sure the request was successful
        if (resultCode == RESULT_OK) {
            // Check which request we're responding to
            if (requestCode == SHARE_INTENT) {
                SharedPreferences mainLog = getSharedPreferences(MainActivity.MAIN_PREFS, 0);
                if (mainLog.getBoolean("give_share_reward", true)){
                    int clueCount = mainLog.getInt("clue_count", 0);
                    clueCount += 3;
                    SharedPreferences.Editor editor = mainLog.edit();
                    editor.putInt("clue_count", clueCount);
                    editor.putBoolean("give_share_reward", false);
                    editor.apply();
                    Snackbar.make(mScreenMain, getString(R.string.clue_gained,3),
                            Snackbar.LENGTH_LONG).show();
                }
            }
        }
    }

    private void shareApp(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        @SuppressLint("InflateParams")
            View dialogView = inflater.inflate(R.layout.share_request, null);
        SharedPreferences settingsPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (settingsPref.getBoolean(SettingsActivity.KEY_PREF_DARK, false)) {
            TextView textViewMessage = dialogView.findViewById(R.id.share_request_message);
            textViewMessage.setTextColor(this.getResources().getColor(android.R.color.white));
        }
        builder.setView(dialogView)
                .setPositiveButton(R.string.share, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String uri = "http://play.google.com/store/apps/details?id=" + getPackageName();
                        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                        sharingIntent.setType("text/plain");
                        sharingIntent.putExtra(Intent.EXTRA_SUBJECT,getString(R.string.app_name));
                        sharingIntent.putExtra(Intent.EXTRA_TEXT, uri);
                        // Start the share activity and check for the result
                        startActivityForResult(Intent.createChooser(sharingIntent, getResources().getText(R.string.invite_friends)), SHARE_INTENT);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) { }
                }).create().show();
    }

    public void openStore(){
        if (mAcquireFragment == null) {
            mAcquireFragment = new AcquireFragment();
        }
        if (!isAcquireFragmentShown()) {
            mAcquireFragment.show(getSupportFragmentManager(), "dialog");
            if (mBillingManager != null
                    && mBillingManager.getBillingClientResponseCode()
                    > BILLING_MANAGER_NOT_INITIALIZED) {
                mAcquireFragment.onManagerReady(this);
            }
        }
    }

    private void startChallenge(int num){
        String word, jumble;
        String fileName = "daily_challenge.txt";
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(getAssets().open(fileName)));
            reader.skip(16L*num);

            String[] temp = reader.readLine().trim().split(";");
            word = temp[0].trim();
            jumble = temp[1].trim();

            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("gameNum", -1);
            intent.putExtra("gameLetters", jumble);
            intent.putExtra("challengeSolution", word);
            this.startActivity(intent);

        }catch (IOException e){
            Toast.makeText(this, R.string.file_read_error, Toast.LENGTH_LONG).show();
        }
    }


    public static void setNotification(Context ctx){
        // Check if alarmReceiver is initiated
        if (alarmReceiver == null) alarmReceiver = new AlarmReceiver();

        // Return if Notifications switched off in settings
        SharedPreferences settingsPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean notify = settingsPref.getBoolean(SettingsActivity.KEY_PREF_CHALLENGE, true);
        if (!notify)
            return;

        alarmReceiver.setAlarm(ctx);
    }

    // Overload method for non-static calls
    private void setNotification(){ setNotification(this);}

    // Clear a notification
    public static void cancelNotification(){
        alarmReceiver.cancelAlarm();
    }

}
