package za.co.twinc.a9letterjumble;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.andremion.counterfab.CounterFab;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;

import nl.dionsegijn.konfetti.KonfettiView;
import nl.dionsegijn.konfetti.models.Shape;
import nl.dionsegijn.konfetti.models.Size;

public class GameActivity extends AppCompatActivity implements RewardedVideoAdListener {

    private InterstitialAd mInterstitialAd;
    private boolean justCreated;

    private RewardedVideoAd rewardAd;
    private ImageButton rewardImage;

    private GridAdapter gridAdapter;
    private CounterFab counterFab;

    private TextView textViewGuess, textViewList, textViewScore;
    private CardView cardView;

    private List<String> wordsDict, defDict;
    private Set<String> wordsIn;
    private Set<String> wordsClues;

    private int gameNum, numLevels, sorting;
    private String gameName, gameLetters;

    private int score;
    private Stack<Integer> buttonStack;

    private boolean isChallenge;
    private String challengeSolution;
    private TextView textTimer;
    private CountDownTimer countDownTimer;
    private int secondsTimer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Check for dark mode in settings
        SharedPreferences settingsPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (settingsPref.getBoolean(SettingsActivity.KEY_PREF_DARK, false))
            setTheme(R.style.AppThemeDark);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Load sorting order from settings
        try{
            sorting = Integer.parseInt(settingsPref.getString(SettingsActivity.KEY_PREF_SORT, "0"));
        } catch (NumberFormatException e) {
            sorting = 0;
        }

        justCreated = true;

        // Create main share preference log
        final SharedPreferences mainLog = getSharedPreferences(MainActivity.MAIN_PREFS, 0);

        Intent startGameIntent = getIntent();
        gameNum = startGameIntent.getIntExtra("gameNum",0);
        isChallenge = gameNum < 0;

        // Initialise banner and MobileAds
        AdView adView = findViewById(R.id.adView);
        MobileAds.initialize(this, getString(R.string.app_id));

        // Initialise interstitial ad
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.ad_unit_id_interstitial_resume));

        // No banner ad if premium
        // Also don't show an ad in the daily challenge
        if (!mainLog.getBoolean("premium", false) && !isChallenge){
            // Load add
            adView.setVisibility(View.VISIBLE);
            AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice("5F2995EE0A8305DEB4C48C77461A7362")
                    .build();
            adView.loadAd(adRequest);
        }

        // Set up the reward ad
        rewardAd = MobileAds.getRewardedVideoAdInstance(this);
        rewardAd.setRewardedVideoAdListener(this);
        rewardImage = findViewById(R.id.button_gift);
        rewardImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRewardedAd();
            }
        });
        // Don't show ad in the first ad_interval_time of gameplay
        saveLongToPrefs("ad_interval_time", System.currentTimeMillis());

        // Add touch listener for words card
        cardView = findViewById(R.id.word_card);
        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayStats();
            }
        });

        // Add longClick for backspace
        ImageButton backspaceButton = findViewById(R.id.button_backspace);
        backspaceButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                onButtonBackspaceLongClick();
                return true;
            }
        });

        // Initialise back stack for backspace
        buttonStack = new Stack<>();

        numLevels = startGameIntent.getIntExtra("numLevels",1);
        gameLetters = startGameIntent.getStringExtra("gameLetters");
        gameName = isChallenge ? getString(R.string.button_challenge) : GameGrid.gameNames[gameNum];
        textViewGuess = findViewById(R.id.text_guess);

        // Save the score to SharedPreferences when updated
        textViewScore = findViewById(R.id.score);
        textViewScore.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String scoreString = charSequence.toString().split(" ")[1];
                saveStringToPrefs("score", scoreString);
            }
            @Override
            public void afterTextChanged(Editable editable) {            }
        });

        score = 0;

        wordsDict = new ArrayList<>();
        defDict = new ArrayList<>();
        getWords(wordsDict, defDict);

        // Get the words already guessed correctly
        wordsIn = new LinkedHashSet<>();
        Set<String> prefSet = mainLog.getStringSet(String.format(Locale.US, "words_%d", gameNum), null);
        if (prefSet != null) {
            wordsIn.addAll(prefSet);
            score = prefSet.size();
        }

        // Save the guessed words as a whole string to shared preferences when updating
        textViewList = findViewById(R.id.word_list);
        updateWordList();

        // Get previous clues
        wordsClues = new LinkedHashSet<>();
        prefSet = mainLog.getStringSet(String.format(Locale.US, "clues_%d", gameNum), null);
        if (prefSet != null)
            wordsClues.addAll(prefSet);

        // Update the old clue system
        if (wordsClues.size() > 0 && getStringFromPrefs("clue_words", "empty").equals("empty")){
            StringBuilder temp = new StringBuilder();
            for (String s : wordsClues)
                temp.append(";").append(s);
            // remove the first ";"
            temp = new StringBuilder(temp.substring(1));
            saveStringToPrefs("clue_words", temp.toString());
        }

        updateScore();
        setGameNameDisplay();
        counterFab = findViewById(R.id.counter_fab);
        counterFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showClueDialog();
            }
        });
        setCounterFab(mainLog.getInt("clue_count", 0));


        GridView gridView = findViewById(R.id.grid);
        gridAdapter = new GridAdapter(this, gameLetters);
        gridView.setAdapter(gridAdapter);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (textViewGuess.getText().toString().equals(getString(R.string.game_display_name, gameName)))
                    textViewGuess.setText(((TextView)view).getText());
                else
                    textViewGuess.append(((TextView)view).getText());
                gridAdapter.setItemClickable(i, false);
                ((TextView) view).setTextColor(getResources().getColor(R.color.background));
                if (i == 4 && !isChallenge)
                    view.setBackground(getResources().getDrawable(R.drawable.button_round_grey));
                buttonStack.push(i);
                if (sorting==0) updateWordList();

                // Shake the enter button
                if (gameNum==0 && score==0 && textViewGuess.getText().length()==4){
                    ObjectAnimator shake = ObjectAnimator.ofFloat(findViewById(R.id.button_enter),
                            "rotation",
                            0, -15, 15, -15, 15);
                    shake.setDuration(250);
                    shake.setRepeatCount(1);
                    shake.setRepeatMode(ObjectAnimator.REVERSE);
                    shake.setInterpolator(new LinearInterpolator());
                    shake.start();
                }
            }
        });

        // Check if we are doing a daily challenge and hide some elements
        if (isChallenge){
            challengeSolution = startGameIntent.getStringExtra("challengeSolution");

            textViewScore.setVisibility(View.GONE);
            counterFab.setVisibility(View.GONE);
            textViewList.setText(R.string.challenge_message);

            textTimer = findViewById(R.id.text_timer);
            textTimer.setVisibility(View.VISIBLE);

            // The timer is set-up and started from onResume()
        }
    }

    @Override
    public void onStop(){
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        super.onStop();
    }

    @Override
    public void onDestroy(){
        rewardAd.destroy(this);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (isChallenge) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.challenge_exit_title)
                    .setMessage(R.string.challenge_exit_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface arg0, int arg1) {
                            // Save the default start time to prefs
                            saveIntToPrefs("paused_time_timer", 121);
                            secondsTimer = 0;
                            GameActivity.super.onBackPressed();
                        }
                    }).create().show();
        }

        else if (rewardAd.isLoaded()){
            new AlertDialog.Builder(this)
                    .setTitle(R.string.reward_title)
                    .setMessage(R.string.reward_message)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            rewardAd.show();
                        }
                    })
                    .setNegativeButton(R.string.exit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            GameActivity.super.onBackPressed();
                        }
                    })
                    .create()
                    .show();
        }
        else
            super.onBackPressed();
    }

    @Override
    public void onPause(){
        if (countDownTimer != null) {
            saveLongToPrefs("paused_time_wall", System.currentTimeMillis());
            if (secondsTimer != 0)
                saveIntToPrefs("paused_time_timer", secondsTimer);
            countDownTimer.cancel();
        }
        rewardAd.pause(this);
        super.onPause();
    }

    @Override
    public void onResume(){
        if (isChallenge){
            secondsTimer = getIntFromPrefs("paused_time_timer", 121);

            if (secondsTimer != 121)
                secondsTimer -= (System.currentTimeMillis() -
                        getLongFromPrefs("paused_time_wall", System.currentTimeMillis()))/1000;

            // Don't go negative
            if (secondsTimer <= 0){
                secondsTimer = 0;
                // Save the default start time to prefs
                saveIntToPrefs("paused_time_timer", 121);
                timedOut();
            }
            else
                startTimer();
        }

        rewardAd.resume(this);

        if (!justCreated && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        }

        justCreated = false;
        super.onResume();
    }



    private void setGameNameDisplay(){
        textViewGuess.setText(getString(R.string.game_display_name, gameName));
    }

    public void onButtonShuffleClick(View v){
        gridAdapter.shuffleLetters();
        onButtonBackspaceLongClick();

        // Rotate the shuffle button
        ObjectAnimator rotate = ObjectAnimator.ofFloat(findViewById(R.id.button_shuffle),
                "rotation",
                0, 180);
        rotate.setDuration(250);
        rotate.setInterpolator(new LinearInterpolator());
        rotate.start();
    }

    public void onButtonBackspaceClick(View v){
        String guess = textViewGuess.getText().toString();
        // Check for default display and return
        if (guess.charAt(0) == getString(R.string.game_display_name," ").charAt(0))
            return;

        int i = buttonStack.pop();
        gridAdapter.setItemClickable(i, true);
        gridAdapter.notifyDataSetChanged();

        // Remove last letter if more than one letter is present, otherwise display level name
        if (guess.length() > 1)
            textViewGuess.setText(guess.subSequence(0, guess.length()-1));
        else
            setGameNameDisplay();

        if (sorting==0) updateWordList();
    }

    public void onButtonBackspaceLongClick() {
        // Display the level name
        setGameNameDisplay();
        gridAdapter.setAllClickableTrue();
        gridAdapter.notifyDataSetChanged();
        if (sorting==0) updateWordList();
    }

    public void onButtonEnterClick(View v){
        final CharSequence guess = textViewGuess.getText();
        setGameNameDisplay();
        gridAdapter.setAllClickableTrue();
        gridAdapter.notifyDataSetChanged();
        if (sorting==0) updateWordList();


        //TODO: Include some animation here when getting the guess wrong
//
//        if (android.os.Build.VERSION.SDK_INT >= 21) {
//            ObjectAnimator flash = ObjectAnimator.ofArgb(cardView,
//                    "CardBackgroundColor",
//                    this.getResources().getColor(R.color.colorPrimary),
//                    this.getResources().getColor(R.color.altColor));
//            flash.setDuration(300);
//            flash.setRepeatCount(1);
//            flash.setRepeatMode(ObjectAnimator.REVERSE);
//            flash.setInterpolator(new FastOutSlowInInterpolator());
//            flash.start();
//        }



        // Check for daily challenge
        if (isChallenge){
            if (guess.toString().toLowerCase().equals(challengeSolution)) {
                if (countDownTimer != null)
                    countDownTimer.cancel();
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.challenge_solved_title)
                        .setMessage(R.string.challenge_solved)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Save the default start time to prefs
                                saveIntToPrefs("paused_time_timer", 121);
                                secondsTimer = 0;
                                increaseCounterFab();
                                // End the activity here since the challenge is solved
                                finish();
                            }
                        })
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                finish();
                            }
                        });
                builder.create().show();
            }
            else
                Snackbar.make(findViewById(R.id.game_content), R.string.challenge_wrong_guess,
                        Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Load video ad if appropriate
        loadRewardedAd();

        // Load interstitial if not already loaded
        if (!getBooleanFromPrefs("premium", false) && !mInterstitialAd.isLoaded()){
            mInterstitialAd.loadAd(new AdRequest.Builder()
                    .addTestDevice("5F2995EE0A8305DEB4C48C77461A7362")
                    .build());
        }

        // Check for empty guess
        if (guess.toString().equals(getString(R.string.game_display_name, gameName)))
            return;

        // Check for too short guess
        if (guess.length() < 4){
            Snackbar.make(findViewById(R.id.game_content), getString(R.string.game_short_guess),
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Check for middle letter
        char requiredChar = gameLetters.toUpperCase().charAt(4);
        if (guess.toString().indexOf(requiredChar) < 0){
            Snackbar.make(findViewById(R.id.game_content), getString(R.string.game_traditional_guess, requiredChar),
                    Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Check for wrong guess
        if (!wordsDict.contains(guess.toString().toLowerCase())){
            Snackbar.make(findViewById(R.id.game_content), getString(R.string.game_invalid_guess, guess),
                    Snackbar.LENGTH_LONG)
                    .setAction(R.string.game_dispute, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                            emailIntent.setData(Uri.parse("mailto:dev.twinc@gmail.com"))
                                        .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.game_dispute_subject,
                                            guess, gameName))
                                        .putExtra(Intent.EXTRA_TEXT, getString(R.string.game_dispute_body));
                            try {
                                startActivity(emailIntent);
                            } catch (ActivityNotFoundException e) {
                                Toast.makeText(getApplicationContext(), getString(R.string.txt_no_email),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .show();
            return;
        }

        // Check for repeated guess
        if (wordsIn.contains(guess.toString())){
            Snackbar.make(findViewById(R.id.game_content), getString(R.string.game_repeated_guess, guess), Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Entered word is correct input
        // Animate the cardview when guessing correctly
        ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(
                cardView,
                PropertyValuesHolder.ofFloat("scaleX", 1.15f),
                PropertyValuesHolder.ofFloat("scaleY", 1.15f));
        pulse.setDuration(250);
        pulse.setRepeatCount(1);
        pulse.setRepeatMode(ObjectAnimator.REVERSE);
        pulse.setInterpolator(new FastOutSlowInInterpolator());
        pulse.start();

        score += 1;
        updateScore();

        // Special treatment in ALFA
        if (gameNum == 0){
            // Hand out clues every 15 words
            if (score%15 == 0)
                increaseCounterFab();
            // Show a tapTarget for getting clues after the first 8 words
            else if (score == 8)
                showClueTip();
            // Show a tapTarget for stats screen after the first 13 words
            else if (score == 13)
                showStatsTip();
        }

        // 3 Clue tokens when completing the level
        if (score == wordsDict.size() && !getBooleanFromPrefs("completed_"+gameNum, false)) {
            saveBooleanToPrefs("completed_"+gameNum, true);
            setCounterFab(counterFab.getCount() + 3);
            Snackbar.make(findViewById(R.id.game_content), getString(R.string.clue_gained, 3),
                    Snackbar.LENGTH_LONG).show();
            throwConfetti();
        }

        // Clue for getting a 9 letter word
        if (guess.length() == 9) {
            increaseCounterFab();
            throwConfetti();
        }

        // Unlock next level after half the words
        int unlockRequirement = wordsDict.size()/2;
        if (wordsDict.size()%2 > 0)
            unlockRequirement++;
        if (score == unlockRequirement)
            unlockNext();

        addWord(guess.toString());
    }

    private void addWord(String newWord){
        wordsIn.add(newWord);
        saveStringSetToPrefs("words", wordsIn);

        String list = getStringFromPrefs("wordstring", " ");
        if (list.equalsIgnoreCase(" "))
            list = newWord;
        else
            list = String.format("%s, %s", newWord, list);

        saveStringToPrefs("wordstring", list);
        updateWordList();
    }

    private void updateWordList(){
        // Nothing to be done in challenge mode
        if (isChallenge)
            return;
        String list = "";
        switch (sorting) {
            case 0:
                String guess = textViewGuess.getText().toString();
                List<String> smartList;
                if (guess.indexOf('-')==0)
                    smartList = new ArrayList<String>(wordsIn);
                else
                    smartList = new ArrayList<String>();
                    for (String s : wordsIn){
                        if (s.startsWith(guess))
                            smartList.add(s);
                    }
                Collections.sort(smartList);
                list = smartList.toString();
                list = list.substring(1, list.length()-1);
                break;
            case 1:
                List<String> listSort = new ArrayList<String>(wordsIn);
                Collections.sort(listSort);
                list = listSort.toString();
                list = list.substring(1, list.length()-1);
                break;
            case 2:
                list = getStringFromPrefs("wordstring", getString(R.string.prompt_no_words));
                break;
        }

        if (list.equals("") && wordsIn.size()==0)
            list = getString(R.string.prompt_no_words);
        textViewList.setText(list);
    }

    private void getWords(List<String> words, List<String> definitions){
        if (gameLetters.length()<9)
            return;
        String line;
        String fileName = "_british-english.txt";
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(getAssets().open(gameLetters + fileName)));
            while ((line = reader.readLine()) != null){
                String[] temp = line.split(";");
                words.add(temp[0].trim());
                definitions.add(temp[1].trim().substring(0,1).toUpperCase() + temp[1].trim().substring(1));
            }

        }catch (IOException e){
            Toast.makeText(this, R.string.file_read_error, Toast.LENGTH_LONG).show();
        }
    }

    private void updateScore(){
        String scoreString = String.format(Locale.US,"%d/%d",score, wordsDict.size());
        textViewScore.setText(getString(R.string.game_display_score, scoreString));
    }

    private void displayStats(){
        if (isChallenge)
            return;
        int [] scoreCount = new int[6];
        int [] totalCount = new int[6];

        for (String s: wordsIn)
            scoreCount[s.length()-4]++;
        for (String s: wordsDict)
            totalCount[s.length()-4]++;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String statsString = String.format("%s\n\n%s\n%s\n%s\n%s\n%s\n%s",
                textViewScore.getText().toString().replace(" ", "\t\t\t\t\t\t\t\t"),
                getString(R.string.game_stats_4, scoreCount[0], totalCount[0]),
                getString(R.string.game_stats_5, scoreCount[1], totalCount[1]),
                getString(R.string.game_stats_6, scoreCount[2], totalCount[2]),
                getString(R.string.game_stats_7, scoreCount[3], totalCount[3]),
                getString(R.string.game_stats_8, scoreCount[4], totalCount[4]),
                getString(R.string.game_stats_9, scoreCount[5], totalCount[5]));
        builder.setTitle(R.string.game_stats_screen)
                .setMessage(statsString)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {}
                })
                .setPositiveButton(R.string.definitions, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        displayDefinitions();
                    }
                }).create().show();
    }

    private void displayDefinitions(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String [] wordsArray = wordsIn.toArray(new String[wordsIn.size()]);
        Arrays.sort(wordsArray);

        ExpandableListView myList = new ExpandableListView(this);
        MyExpandableListAdapter myAdapter = new MyExpandableListAdapter(this, wordsArray);
        myList.setAdapter(myAdapter);

        builder.setTitle(R.string.definitions)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) { }
                })
                .setView(myList)
                .create()
                .show();
    }

    private void showClueDialog(){
        String message;
        if (counterFab.getCount() == 0 && wordsClues.size() == 0)
            message = getString(R.string.clues_message_no_tokens);
        else if( counterFab.getCount() == 0)
            message = getString(R.string.clues_message,
                    getString(R.string.clues_message_no_tokens),
                    wordsClues.size());
        else
            message = getString(R.string.clues_message,
                    getString(R.string.clues_available_tokens, counterFab.getCount()),
                    wordsClues.size());

        // TODO: Make positive button a shortcut to store if the player has no clue tokens

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.clues)
                .setMessage(message)
                .setPositiveButton(R.string.clues_use, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        giveClue();
                    }
                })
                .setNeutralButton(R.string.clues_old, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        showOldClues();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
        if (counterFab.getCount() < 1 || score == wordsDict.size())
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        if (wordsClues.size() < 1)
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
    }

    private void giveClue(){
        for (String word: wordsDict){
            if (!wordsIn.contains(word.toUpperCase()) && !wordsClues.contains(word.toUpperCase())){
                decreaseCounterFab();
                wordsClues.add(word.toUpperCase());
                saveStringSetToPrefs("clues", wordsClues);
                saveCluesUsed();

                if (wordsClues.size() == 1)
                    saveStringToPrefs("clue_words", word.toUpperCase());
                else
                    saveStringToPrefs("clue_words",
                            getStringFromPrefs("clue_words", "") + ";" + word.toUpperCase());

                String clue = defDict.get(wordsDict.indexOf(word.toLowerCase()));
                clue = getString(R.string.clue_new_message, clue, word.toUpperCase().charAt(0));

                // add double space to the clue when displaying in isolation
                clue = clue.replace("First letter:", "\nFirst letter:");

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.clue_new)
                        .setMessage(clue)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) { }
                        }).create().show();
                return;
            }
        }
    }

    private void showOldClues(){
        String clueString = "";
        for (String s : getStringFromPrefs("clue_words", "").split(";")){
            String clue = defDict.get(wordsDict.indexOf(s.toLowerCase()));
            if (wordsIn.contains(s))
                clue = getString(R.string.clue_solved_message, clue, s);
            else
                clue = getString(R.string.clue_new_message, clue, s.charAt(0));

            // Add some spacing if this is not the first clue
            if (clueString.length() > 1)
                clueString = String.format(Locale.US,"%s\n\n\n%s", clueString, clue);
            else
                clueString = String.format(Locale.US,"\n%s", clue);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.clues)
                .setMessage(clueString)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) { }
                }).create().show();
    }

    private void saveCluesUsed(){
        saveIntToPrefs(String.format(Locale.US, "clues_used_%d", gameNum), wordsClues.size());
    }

    private void unlockNext(){
        // Quick return if we are playing an old level with newly added words
        if (getIntFromPrefs("games_unlocked", 0) > gameNum)
            return;
        saveIntToPrefs("games_unlocked", gameNum + 1);

        // Return if this is the last available level
        if (gameNum == numLevels-1)
            return;

        // Build a dialog to congratulate the player on unlocking the next level
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String unlockMessage = getString(R.string.unlocked_message, GameGrid.gameNames[gameNum+1]);

        // Encourage the player to finish levels
        if (gameNum == 0)
            unlockMessage = String.format(Locale.US, "%s\n\n%s", unlockMessage, getString(R.string.unlocked_keep_going));

        // From level Bravo, give a rating prompt
        SharedPreferences mainLog = getSharedPreferences(MainActivity.MAIN_PREFS, 0);
        if (mainLog.getBoolean("show_rate_prompt", true) && gameNum > 0){
            unlockMessage = String.format(Locale.US, "%s\n\n%s", unlockMessage, getString(R.string.enjoying_game));
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    askReview();
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    stopRatePrompts();
                    askFeedback();
                }
            });
        }
        else{
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) { }
            });
        }

        builder.setTitle(R.string.unlocked_title)
                .setMessage(unlockMessage)
                .create().show();

        // Throw some confetti
        throwConfetti();
    }

    private void throwConfetti(){
        KonfettiView viewKonfetti = findViewById(R.id.viewKonfetti);
        viewKonfetti.build()
                .addColors(getResources().getColor(R.color.altColor),
                        getResources().getColor(R.color.colorPrimaryDark),
                        getResources().getColor(R.color.rippleColor))
                .setDirection(0.0, 359.0)
                .setSpeed(1f, 5f)
                .setFadeOutEnabled(true)
                .addShapes(Shape.RECT)
                .addSizes(new Size(12, 5f))
                .setPosition((float)viewKonfetti.getWidth()/2, 200f)
                .stream(300, 700L);
    }

    private void askFeedback() {
        FeedbackClass feedbackClass = new FeedbackClass(this);
        feedbackClass.showFeedbackDialog();
    }

    private void askReview(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.rate_request, null);
        builder.setView(dialogView)
                .setPositiveButton(R.string.review, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        stopRatePrompts();
                        Intent goToMarket = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=" + getPackageName()));
                        startActivity(goToMarket);
                    }
                })
                .setNegativeButton(R.string.later, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) { }
                })
                .setNeutralButton(R.string.never, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) { stopRatePrompts(); }
                }).create().show();

    }

    private void stopRatePrompts(){
        saveBooleanToPrefs("show_rate_prompt", false);
    }

    private void showClueTip(){
        TapTargetView.showFor(this,                 // `this` is an Activity
                TapTarget.forView(findViewById(R.id.score),
                        getString(R.string.clue_tip_title),
                        getString(R.string.clue_tip_description))
                        // All options below are optional
                        .outerCircleColor(R.color.colorPrimaryDark)   // Specify a color for the outer circle
                        .transparentTarget(true)
                        .drawShadow(true)                             // Whether to draw a drop shadow or not
                        .textColor(android.R.color.white));           // Specify a color for both the title and description text
    }

    private void showStatsTip(){
        TapTargetView.showFor(this,                 // `this` is an Activity
                TapTarget.forView(textViewList,
                        getString(R.string.game_stats_screen),
                        getString(R.string.stats_tip_description))
                        // All options below are optional
                        .outerCircleColor(R.color.colorPrimaryDark)   // Specify a color for the outer circle
                        .transparentTarget(true)
                        .drawShadow(true)                             // Whether to draw a drop shadow or not
                        .textColor(android.R.color.white)
                        .targetRadius(textViewList.getWidth()/4),
                new TapTargetView.Listener() {          // The listener can listen for regular clicks, long clicks or cancels
                    @Override
                    public void onTargetClick(TapTargetView view) {
                        super.onTargetClick(view);      // This call is optional
                        displayStats();
                    }
                });
    }

    private void startTimer(){
        countDownTimer = new CountDownTimer(secondsTimer * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                secondsTimer -= 1;
                displayTimer();
            }

            @Override
            public void onFinish() {
                secondsTimer = 0;
                // Save the default start time to prefs
                saveIntToPrefs("paused_time_timer", 121);
                displayTimer();
                timedOut();
            }

        }.start();
    }

    private void displayTimer(){
        int min = secondsTimer/60;
        int sec = secondsTimer - min*60;
        textTimer.setText(String.format(Locale.UK,"%02d:%02d", min, sec));
    }

    private void timedOut(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.challenge_failed_title)
                .setMessage(getString(R.string.challenge_failed, challengeSolution.toUpperCase()))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // End the activity here since the challenge is done
                        finish();
                    }
                }).setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                finish();
            }
        });

        builder.create().show();
    }

    private void loadRewardedAd() {
        // Return if Notifications switched off in settings
        SharedPreferences settingsPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (!settingsPref.getBoolean(SettingsActivity.KEY_PREF_REWARD, true))
            return;
        if (gameNum == 0 && score < 18)
            return;
        if (score < 7)
            return;
        // Load after 2 minutes, play every 3
        if (System.currentTimeMillis() - getLongFromPrefs("ad_interval_time", 0L) < 2*60*1000)
            return;

        // Start loading the video ad
        if (!rewardAd.isLoaded()) {
            rewardAd.loadAd(getString(R.string.ad_unit_id_reward),
                    new AdRequest.Builder()
                            .addTestDevice("5F2995EE0A8305DEB4C48C77461A7362")
                            .build());
        }
        else { // Ad is already loaded
            // Still don't show ad until 3 minutes
            if (System.currentTimeMillis() - getLongFromPrefs("ad_interval_time", 0L) < 3*60*1000)
                return;

            rewardImage.setVisibility(View.VISIBLE);
        }
    }

    private void showRewardedAd(){
        rewardImage.setVisibility(View.GONE);
        saveLongToPrefs("ad_interval_time", System.currentTimeMillis());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.reward_title)
                .setMessage(R.string.reward_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        rewardAd.show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null);
        builder.create().show();
    }


    private void increaseCounterFab() {
        Snackbar.make(findViewById(R.id.game_content), getString(R.string.clue_gained_one),
                Snackbar.LENGTH_LONG).show();
        counterFab.increase();
        saveCounterFab();
    }

    private void decreaseCounterFab(){
        counterFab.decrease();
        saveCounterFab();
    }

    private void setCounterFab(int count){
        counterFab.setCount(count);
        saveCounterFab();
    }

    private void saveCounterFab(){
        saveIntToPrefs("clue_count", counterFab.getCount());
    }

    private void saveStringToPrefs(String stringName, String string){
        SharedPreferences mainLog = getSharedPreferences(MainActivity.MAIN_PREFS, 0);
        SharedPreferences.Editor editor = mainLog.edit();
        editor.putString(String.format(Locale.US, "%s_%d", stringName, gameNum), string);
        editor.apply();
    }

    private String getStringFromPrefs(String stringName, String defaultString){
        SharedPreferences mainLog = getSharedPreferences(MainActivity.MAIN_PREFS, 0);
        return mainLog.getString(String.format(Locale.US, "%s_%d", stringName, gameNum), defaultString);
    }

    private void saveStringSetToPrefs(String setName, Set<String> set){
        SharedPreferences mainLog = getSharedPreferences(MainActivity.MAIN_PREFS, 0);
        SharedPreferences.Editor editor = mainLog.edit();
        editor.putStringSet(String.format(Locale.US, "%s_%d", setName, gameNum), set);
        editor.apply();
    }

    private void saveIntToPrefs(String intName, int i){
        SharedPreferences mainLog = getSharedPreferences(MainActivity.MAIN_PREFS, 0);
        SharedPreferences.Editor editor = mainLog.edit();
        editor.putInt(intName, i);
        editor.apply();
    }

    private int getIntFromPrefs(String intName, int defaultInt){
        SharedPreferences mainLog = getSharedPreferences(MainActivity.MAIN_PREFS, 0);
        return mainLog.getInt(intName, defaultInt);
    }

    private void saveLongToPrefs(String longName, long l){
        SharedPreferences mainLog = getSharedPreferences(MainActivity.MAIN_PREFS, 0);
        SharedPreferences.Editor editor = mainLog.edit();
        editor.putLong(longName, l);
        editor.apply();
    }

    private long getLongFromPrefs(String longName, long defaultLong){
        SharedPreferences mainLog = getSharedPreferences(MainActivity.MAIN_PREFS, 0);
        return mainLog.getLong(longName, defaultLong);
    }

    private void saveBooleanToPrefs(String boolName, boolean bool){
        SharedPreferences mainLog = getSharedPreferences(MainActivity.MAIN_PREFS, 0);
        SharedPreferences.Editor editor = mainLog.edit();
        editor.putBoolean(boolName, bool);
        editor.apply();
    }

    private boolean getBooleanFromPrefs(String boolName, boolean defaultBool){
        SharedPreferences mainLog = getSharedPreferences(MainActivity.MAIN_PREFS, 0);
        return mainLog.getBoolean(boolName, defaultBool);
    }



    // Required to reward the user.
    @Override
    public void onRewarded(RewardItem reward) {
        increaseCounterFab();
    }

    // The following listener methods are optional (but apparently aren't).
    @Override
    public void onRewardedVideoAdLeftApplication() {    }
    @Override
    public void onRewardedVideoAdClosed() {    }
    @Override
    public void onRewardedVideoAdFailedToLoad(int errorCode) {    }

    @Override
    public void onRewardedVideoAdLoaded() {
        // Don't show rewardImage here, as we are now preloading ads before we want to show them.
    }
    @Override
    public void onRewardedVideoAdOpened() {    }
    @Override
    public void onRewardedVideoStarted() {    }
    @Override
    public void onRewardedVideoCompleted() {    }


    private class MyExpandableListAdapter implements ExpandableListAdapter {

        private Context context;
        private String[] wordsArray;

        MyExpandableListAdapter(Context context, String [] wordsArray){
            this.context = context;
            this.wordsArray = wordsArray;
        }


        @Override
        public void registerDataSetObserver(DataSetObserver observer) {

        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {

        }

        @Override
        public int getGroupCount() {
            return wordsArray.length;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            // Only one definition per word
            return 1;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return wordsArray[groupPosition];
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return wordsArray[groupPosition];
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if (convertView == null){
                // Create a new view
                LayoutInflater inf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inf.inflate(R.layout.definitions_group, null);
            }
            TextView heading = convertView.findViewById(R.id.heading);
            String word = wordsArray[groupPosition].toLowerCase();
            heading.setText(word.substring(0,1).toUpperCase() + word.substring(1));
            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            if (convertView == null) {
                // Create a new view
                LayoutInflater infalInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = infalInflater.inflate(R.layout.definitions_child, null);
            }
            TextView childItem = convertView.findViewById(R.id.childItem);
            String def = defDict.get(wordsDict.indexOf(wordsArray[groupPosition].toLowerCase()));
            childItem.setText(def);
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public void onGroupExpanded(int groupPosition) {        }

        @Override
        public void onGroupCollapsed(int groupPosition) {        }

        @Override
        public long getCombinedChildId(long groupId, long childId) {
            return 0;
        }

        @Override
        public long getCombinedGroupId(long groupId) {
            return 0;
        }
    }
}
