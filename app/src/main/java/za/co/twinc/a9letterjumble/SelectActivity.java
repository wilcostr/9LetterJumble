package za.co.twinc.a9letterjumble;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.Locale;

public class SelectActivity extends AppCompatActivity {

    private ListView listView;
    private int numGames;
    private String[] gameList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select);

        gameList = getResources().getStringArray(R.array.games);
        numGames = gameList.length;

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.play_game);
        }
        listView = findViewById(R.id.selection_list);
        initList();
    }

    @Override
    protected void onResume(){
        initList();
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

    private void initList(){
        GameGrid gameGrid = new GameGrid(this, numGames);
        listView.setAdapter(gameGrid);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long l) {

                SharedPreferences mainLog = view.getContext().getSharedPreferences(MainActivity.MAIN_PREFS,0);
                int unlockedPosition = mainLog.getInt("games_unlocked", 0);

                if (pos <= unlockedPosition) {
                    Intent intent = new Intent(view.getContext(), GameActivity.class);
                    intent.putExtra("gameNum", pos);
                    intent.putExtra("gameLetters", gameList[pos]);
                    intent.putExtra("numLevels", numGames);
                    view.getContext().startActivity(intent);
                }
                else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                    builder.setTitle(getString(R.string.unlock_title, GameGrid.gameNames[pos]))
                            .setMessage(getString(R.string.unlock_message, GameGrid.gameNames[pos-1]))
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) { }
                            }).create().show();
                }
            }
        });

    }

}
