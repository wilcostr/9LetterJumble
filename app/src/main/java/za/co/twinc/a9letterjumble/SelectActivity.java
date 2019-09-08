package za.co.twinc.a9letterjumble;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
        if (currentPack>=0)
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
                    public void onItemClick(View view, int pos) {
                        int firstInPack = 0;
                        for (int i=0; i<pos; i++)
                            firstInPack += packCounts[i];
                        if (firstInPack <= mainLog.getInt("games_unlocked", 0)) {
                            mySounds.playClick(view.getContext());
                            // Show games
                            currentPack = pos;
                            showAnimation = true;
                            initGameSelect(pos);
                        }
                        else{
                            mySounds.play(view.getContext(), R.raw.reject);
                            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                            builder.setTitle(getString(R.string.unlock_title, packNames[pos]))
                                    .setMessage(getString(R.string.unlock_pack_message))
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) { }
                                    }).create().show();
                        }
                    }
                }
        );

        recyclerView.setAdapter(gameGrid);
        if (showAnimation)
            recyclerView.scheduleLayoutAnimation();
        showAnimation = false;
    }


    @Override
    public void onBackPressed() {
        if (currentPack >= 0){
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
        final String [] gameNames = this.getResources().getStringArray(R.array.gameNames);

        GameGrid gameGrid = new GameGrid(this, pack,
                new GameGrid.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int pos) {
                        if (pos+offset <= mainLog.getInt("games_unlocked", 0)) {
                            mySounds.playClick(view.getContext());
                            Intent intent = new Intent(view.getContext(), GameActivity.class);
                            intent.putExtra("gameNum", pos+offset);
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
