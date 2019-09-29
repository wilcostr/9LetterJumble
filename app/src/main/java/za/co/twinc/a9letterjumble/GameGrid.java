package za.co.twinc.a9letterjumble;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Created by wilco on 2018/01/03.
 * 9LetterJumble
 */

public class GameGrid extends RecyclerView.Adapter<GameViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    final private OnItemClickListener listener;

    private int numGames;
    private int numPacks;
    private int gameOffset;
    private int packNum;

    private int [] packCounts;

    private boolean isPack;

    private String [] gameNames;
    private String [] packNames;

    /*
    Initialisation
    pack        gives the number of the level pack, or -1 to indicate pack selection
     */
    GameGrid(Context c, int pack, OnItemClickListener l) {
        super();

        listener = l;
        isPack = (pack==-1);
        packNum = pack;

        Resources res = c.getResources();
        gameNames = res.getStringArray(R.array.gameNames);
        packNames = res.getStringArray(R.array.levelPacks);

        packCounts = res.getIntArray(R.array.levelPackCounts);
        gameOffset = 0;
        for (int i=0; i<pack; i++)
            gameOffset += packCounts[i];

        if (pack >= 0)
            numGames = packCounts[pack];
        else if (pack == -10){
            gameNames = res.getStringArray(R.array.gameNamesPremium);
            numGames = res.getIntArray(R.array.levelPackCountsPremium)[0];
        }
        numPacks = packNames.length;

        SharedPreferences mainLog = c.getSharedPreferences(MainActivity.MAIN_PREFS, 0);
        int gamesUnlocked = mainLog.getInt("games_unlocked", 0);
        if (gamesUnlocked >= 3)
            numPacks += 1;

        setHasStableIds(true);
    }

    @Override
    public int getItemCount(){
        if (isPack)
            return numPacks;
        else
            return numGames;
    }

//    public Object getItem(int position) {
//        return null;
//    }

    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return GameViewHolder.make(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
        holder.setModel(position);
        View view = holder.getView();
        SharedPreferences mainLog = view.getContext().getSharedPreferences(MainActivity.MAIN_PREFS, 0);

        final int fp = position;

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onItemClick(view, fp);
            }
        });

        int [] gameImages = {R.drawable.ic_icon_game1, R.drawable.ic_icon_game2, R.drawable.ic_icon_game3,
                R.drawable.ic_icon_game4, R.drawable.ic_icon_game5, R.drawable.ic_icon_game6, R.drawable.ic_icon_game7};

        TextView gameTag = view.findViewById(R.id.game_tag);
        TextView cluesUsed = view.findViewById(R.id.clues_used);
        CardView wordCard = view.findViewById(R.id.word_card);
        ImageView arrowButton = view.findViewById(R.id.arrow);
        ImageView gameImage = view.findViewById(R.id.game_image);
        TextView score = view.findViewById(R.id.game_score);

        if (isPack) {
            String thisPack;
            int thisImage;
            int gamesUnlocked = mainLog.getInt("games_unlocked", 0);
            if (gamesUnlocked >= 3) {
                if (position==0) {
                    thisPack = view.getContext().getResources().getStringArray(R.array.levelPacksPremium)[0];
                    thisImage = R.drawable.ic_icon_premium;
                }
                else {
                    thisPack = packNames[position - 1];
                    thisImage = gameImages[position - 1];
                }
            }
            else{
                thisPack = packNames[position];
                thisImage = gameImages[position];
            }

            // Display level pack name
            gameTag.setText(String.format(Locale.US, "%s  ", thisPack));
            cluesUsed.setVisibility(View.GONE);
            score.setVisibility(View.GONE);

            // Set game image to rainbow
            gameImage.setImageResource(thisImage);

            gameOffset = 0;
            for (int i=0; i<position; i++)
                gameOffset += packCounts[i];
            if (gamesUnlocked >= 3 && position > 0)
                gameOffset -= packCounts[position-1];

            boolean isLocked = gameOffset > mainLog.getInt("games_unlocked", 0);
            boolean isNewsletter = mainLog.getBoolean("isNewsletter", false);
            if (position==0 && gamesUnlocked >= 3 && !isNewsletter)
                isLocked = true;

            if (isLocked) {
                wordCard.setCardBackgroundColor(view.getContext().getResources().getColor(R.color.rippleColor));
                arrowButton.setImageResource(R.drawable.ic_lock_outline);
            }
            else {
                wordCard.setCardBackgroundColor(view.getContext().getResources().getColor(R.color.colorAccent));
                //arrowButton.setColorFilter(view.getContext().getResources().getColor(R.color.altColorLight));
            }
        }
        else{
            //Level select

            String cluesUsedString = "clues_used_%d";
            String gamesUnlockedString = "games_unlocked";
            String scoreString = "score_%d";
            int thisImage = (packNum>=0)? gameImages[packNum] : R.drawable.ic_icon_premium;

            if (packNum == -10){
                cluesUsedString = "clues_used_premium_%d";
                gamesUnlockedString = "games_unlocked_premium";
                scoreString = "score_premium_%d";
            }

            // Display game name
            gameTag.setText(String.format(Locale.US,"%s  ", gameNames[position+gameOffset].toUpperCase()));

            // Display the number of clues spent on this level
            int clueUseCount = mainLog.getInt(String.format(Locale.US, cluesUsedString, position+gameOffset), 0);
            if (clueUseCount > 0)
                cluesUsed.setVisibility(View.VISIBLE);
            else
                cluesUsed.setVisibility(View.GONE);
            if (clueUseCount > 1)
                cluesUsed.setText(view.getContext().getString(R.string.game_display_clues_used, clueUseCount));
            else if (clueUseCount == 1)
                cluesUsed.setText(R.string.game_display_one_clue_used);

            // Set game image to rainbow
            gameImage.setImageResource(thisImage);

            if (position+gameOffset > mainLog.getInt(gamesUnlockedString, 0)) {
                wordCard.setCardBackgroundColor(view.getContext().getResources().getColor(R.color.rippleColor));
                arrowButton.setImageResource(R.drawable.ic_lock_outline);
                score.setText("");
            }
            // TODO: figure out why this else is necessary to fix Alfa background
            else {
                wordCard.setCardBackgroundColor(view.getContext().getResources().getColor(R.color.colorAccent));
                arrowButton.setImageResource(R.drawable.ic_arrow);
                String scoreDisplayString = mainLog.getString(String.format(Locale.US, scoreString, position+gameOffset), "0");
                score.setText(view.getContext().getString(R.string.game_display_score, scoreDisplayString));
            }
        }

    }

    @Override
    public long getItemId(int position) {
        if (isPack) {
            if (numPacks > packNames.length){
                if (position==0)
                    return "Gold Rush".hashCode();
                else
                    return packNames[position-1].hashCode();
            }
            return packNames[position].hashCode();
        }
        else
            return gameNames[position].hashCode();
    }

}