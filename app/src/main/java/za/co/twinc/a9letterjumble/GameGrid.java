package za.co.twinc.a9letterjumble;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.Image;
import android.support.v7.widget.CardView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * Created by wilco on 2018/01/03.
 * 9LetterJumble
 */

public class GameGrid extends BaseAdapter {
    final private Context mContext;
    private int numGames;
    public static String [] gameNames = {"Alfa", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel", "India",
            "Juliett", "Kilo", "Lima", "Mike", "November", "Oscar", "Papa", "Quebec", "Romeo", "Sierra",
            "Tango", "Uniform", "Victor", "Whiskey", "X-ray", "Yankee", "Zulu", "Alpha", "Beta","Gamma",
            "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota", "Kappa", "Lambda", "Mu", "Nu", "Xi",
            "Omicron", "Pi", "Rho", "Sigma", "Tau", "Upsilon", "Phi", "Chi", "Psi", "Omega"};
    private int [] gameImages = {R.drawable.ic_icon_game1, R.drawable.ic_icon_game2, R.drawable.ic_icon_game3,
            R.drawable.ic_icon_game4, R.drawable.ic_icon_game5, R.drawable.ic_icon_game6, R.drawable.ic_icon_game7};

    GameGrid(Context c, int num) {
        mContext = c;
        numGames = num;
    }

    public int getCount() {
        return numGames;
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new GameItemLayout for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        CustomGameView view;

        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            view = new CustomGameView(this.mContext);

        } else {
            view = (CustomGameView) convertView;
        }
        // Set game image to rainbow
        ImageView gameImage = view.findViewById(R.id.game_image);
        gameImage.setImageResource(gameImages[position%gameImages.length]);

        // Display a special game name
        TextView gameTag = view.findViewById(R.id.game_tag);
        gameTag.setText(String.format(Locale.US,"%s  ", gameNames[position%gameNames.length].toUpperCase()));

        SharedPreferences mainLog = view.getContext().getSharedPreferences(MainActivity.MAIN_PREFS,0);

        // Display the number of clues spent on this level
        TextView cluesUsed = view.findViewById(R.id.clues_used);
        int clueUseCount = mainLog.getInt(String.format(Locale.US, "clues_used_%d", position), 0);
        if (clueUseCount > 0)
            cluesUsed.setVisibility(View.VISIBLE);
        else
            cluesUsed.setVisibility(View.GONE);
        if (clueUseCount > 1)
            cluesUsed.setText(view.getContext().getString(R.string.game_display_clues_used, clueUseCount));
        else if (clueUseCount == 1)
            cluesUsed.setText(R.string.game_display_one_clue_used);

        CardView wordCard = view.findViewById(R.id.word_card);
        ImageView arrowButton = view.findViewById(R.id.arrow);
        TextView score = view.findViewById(R.id.game_score);
        if (position > mainLog.getInt("games_unlocked", 0)) {
            wordCard.setCardBackgroundColor(view.getContext().getResources().getColor(R.color.rippleColor));
            arrowButton.setImageResource(R.drawable.ic_lock_outline_black_36dp);
            score.setText("");
        }
        // TODO: figure out why this else is necessary to fix Alfa background
        else {
            wordCard.setCardBackgroundColor(view.getContext().getResources().getColor(R.color.colorAccent));
            arrowButton.setImageResource(R.drawable.ic_arrow);
            String scoreString = mainLog.getString(String.format(Locale.US, "score_%d", position), "0");
            score.setText(view.getContext().getString(R.string.game_display_score, scoreString));
        }
        return view;
    }

    public class CustomGameView extends RelativeLayout{
        public CustomGameView(Context context){
            super(context);
            View.inflate(getContext(), R.layout.game_item_layout, this);
        }
    }
}