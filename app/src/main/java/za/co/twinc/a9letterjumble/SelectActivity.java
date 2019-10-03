package za.co.twinc.a9letterjumble;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;


public class SelectActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ActionBar actionBar;
    private int numGames;
    private String[] gameList;
    private int[] packCounts;

    private boolean showAnimation = false;
    private Sounds mySounds;
    private int currentPack; //-1 for pack selection

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Check for dark mode in settings
        SharedPreferences settingsPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (settingsPref.getBoolean(SettingsActivity.KEY_PREF_DARK, false))
            setTheme(R.style.AppThemeDark);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select);

        currentPack = -1;

        gameList = getResources().getStringArray(R.array.games);
        numGames = gameList.length;

        packCounts = getResources().getIntArray(R.array.levelPackCounts);

        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.select_pack);
        }
        recyclerView = findViewById(R.id.selection_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        recyclerView.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this,
                R.anim.layout_animation));

        mySounds = new Sounds();
    }

    @Override
    protected void onResume(){
        if (currentPack != -1)
            initGameSelect(currentPack);
        else
            initPackSelect();
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initPackSelect(){

        final SharedPreferences mainLog = this.getSharedPreferences(MainActivity.MAIN_PREFS,0);
        actionBar.setTitle(R.string.select_pack);

        // first check if this is the first time playing
        if (mainLog.getString("score_0", "empty").equals("empty"))
        {
            initGameSelect(0);
            return;
        }

        final String [] packNames = this.getResources().getStringArray(R.array.levelPacks);

        final GameGrid gameGrid = new GameGrid(this, -1,
                new GameGrid.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int posWithOffset) {
                        int gamesUnlocked = mainLog.getInt("games_unlocked", 0);
                        int pos = posWithOffset;
                        if (gamesUnlocked >= 3)
                            pos -= 1;
                        int firstInPack = 0;
                        for (int i=0; i<pos; i++)
                            firstInPack += packCounts[i];
                        if (pos == -1){
                            boolean isNewsletter = mainLog.getBoolean("isNewsletter", false);
                            if (!isNewsletter){
                                mySounds.play(view.getContext(), R.raw.reject);
                                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                                builder.setTitle(R.string.gold_locked)
                                        .setMessage(R.string.gold_message)
                                        .setPositiveButton(R.string.upgrade, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                Intent openMainIntent = new Intent(getApplicationContext(), MainActivity.class);
                                                openMainIntent.putExtra("store", true);
                                                startActivity(openMainIntent);
                                            }
                                        })
                                        .setNegativeButton(R.string.subscribe, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                subscribePrompt();
                                            }
                                        })
                                        .setNeutralButton(android.R.string.cancel, null)
                                        .create()
                                        .show();
                            }
                            else {
                                // Pack is not locked
                                mySounds.playClick(view.getContext());
                                // Show games
                                currentPack = -10;
                                showAnimation = true;
                                initGameSelect(currentPack);
                            }
                        }
                        else if (firstInPack <= gamesUnlocked) {
                            mySounds.playClick(view.getContext());
                            // Show games
                            currentPack = pos;
                            showAnimation = true;
                            initGameSelect(currentPack);
                        }
                        else{
                            mySounds.play(view.getContext(), R.raw.reject);
                            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                            builder.setTitle(getString(R.string.unlock_title, packNames[pos]))
                                    .setMessage(getString(R.string.unlock_pack_message, packNames[pos-1]))
                                    .setPositiveButton(android.R.string.ok, null)
                                    .create()
                                    .show();
                        }
                    }
                }
        );

        recyclerView.setAdapter(gameGrid);
        if (showAnimation)
            recyclerView.scheduleLayoutAnimation();
        showAnimation = false;
    }

    public void subscribePrompt(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.subscribe)
                .setMessage(R.string.subscribe_message)
                .setNeutralButton(android.R.string.cancel, null)
                .setNegativeButton(R.string.subscribe, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                        emailIntent.setData(Uri.parse("mailto:dev.twinc@gmail.com"))
                                .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.subscribe_subject))
                                .putExtra(Intent.EXTRA_TEXT, getString(R.string.subscribe_body));
                        try {
                            startActivity(emailIntent);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(getApplicationContext(), getString(R.string.txt_no_email),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });

        final EditText input = new EditText(this);
        //input.setTextColor(getResources().getColor(android.R.color.black));
        input.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        builder.setView(input);

        builder.setPositiveButton(R.string.unlock, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (input.getText().toString().toLowerCase().trim().equals("anagrams")) {
                    getApplicationContext().getSharedPreferences(MainActivity.MAIN_PREFS,0)
                            .edit()
                            .putBoolean("isNewsletter", true)
                            .apply();
                    Toast.makeText(getApplicationContext(), R.string.gold_unlocked, Toast.LENGTH_LONG).show();
                    currentPack = -10;
                    showAnimation = true;
                    initGameSelect(currentPack);
                }
                else{
                    Toast.makeText(getApplicationContext(), R.string.invalid_code, Toast.LENGTH_LONG).show();
                }
            }
        });

        builder.create().show();
    }


    @Override
    public void onBackPressed() {
        if (currentPack != -1){
            currentPack = -1;
            showAnimation = true;
            initPackSelect();
        }
        else
            super.onBackPressed();
    }


    private void initGameSelect(int pack){

        final SharedPreferences mainLog = this.getSharedPreferences(MainActivity.MAIN_PREFS,0);
        actionBar.setTitle(R.string.select_level);

        int firstInPack = 0;
        for (int i=0; i<pack; i++)
            firstInPack += packCounts[i];

        final int offset = firstInPack;
        final String [] gameNames;
        final int gamesUnlocked;
        final boolean isPremium = pack==-10;
        if (!isPremium) {
            gameNames = this.getResources().getStringArray(R.array.gameNames);
            gamesUnlocked = mainLog.getInt("games_unlocked", 0);
        }
        else {
            gameNames = this.getResources().getStringArray(R.array.gameNamesPremium);
            gameList = this.getResources().getStringArray(R.array.gamesPremium);
            numGames = gameList.length;
            gamesUnlocked = mainLog.getInt("games_unlocked_premium", 0);
        }


        GameGrid gameGrid = new GameGrid(this, pack,
                new GameGrid.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int pos) {
                        if (pos+offset <= gamesUnlocked) {
                            mySounds.playClick(view.getContext());
                            Intent intent = new Intent(view.getContext(), GameActivity.class);
                            int gameNum = (isPremium) ? -10 - pos : pos + offset;
                            intent.putExtra("gameNum", gameNum);
                            intent.putExtra("gameLetters", gameList[pos+offset]);
                            intent.putExtra("numLevels", numGames);
                            view.getContext().startActivity(intent);
                        }
                        else{
                            mySounds.play(view.getContext(), R.raw.reject);
                            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                            builder.setTitle(getString(R.string.unlock_title, gameNames[pos+offset]))
                                    .setMessage(getString(R.string.unlock_message, gameNames[pos+offset-1]))
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) { }
                                    }).create().show();
                        }
                    }
                });

        recyclerView.setAdapter(gameGrid);
        if (showAnimation)
            recyclerView.scheduleLayoutAnimation();
        showAnimation = false;

        // Launch ALFA if this is the first time playing
        if (mainLog.getString("score_0", "empty").equals("empty"))
        {
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("gameNum", 0);
            intent.putExtra("gameLetters", gameList[0]);
            intent.putExtra("numLevels", numGames);
            this.startActivity(intent);
        }
    }

}
