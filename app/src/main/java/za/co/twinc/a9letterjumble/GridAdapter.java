package za.co.twinc.a9letterjumble;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by wilco on 2017/11/28.
 * 9LetterJumble
 */

public class GridAdapter extends BaseAdapter{
    final private Context mContext;
    private String word;
    private int count;
    private boolean centreTint;
    private ArrayList<Boolean> itemClickable = new ArrayList<> ();

    GridAdapter(Context c, String jumbleWord, boolean centreMandatory) {
        mContext = c;
        centreTint = centreMandatory;
        word = jumbleWord.toUpperCase();
        count = word.length();
        for(int j=0;j<count;j++)
            itemClickable.add(true);

    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean isEnabled ( int position ) {
        return itemClickable.get ( position );
    }

    void setItemClickable(int position, Boolean typeValue){
        itemClickable.set (position,typeValue);
    }

    void setAllClickableTrue(){
        for (int j=0; j<getCount(); j++)
            itemClickable.set(j, true);
    }

    void shuffleLetters(){
        List<Character> characters = new ArrayList<>();
        for(char c:word.toCharArray()){
            characters.add(c);
        }
        Character centreChar = 'a';
        if (centreTint)
            centreChar = characters.remove(4);
        StringBuilder output = new StringBuilder(getCount());
        while(characters.size()!=0) {
            int randPicker = (int) (Math.random() * characters.size());
            output.append(characters.remove(randPicker));
            if (centreTint && output.length()==4)
                output.append(centreChar);
        }
        word = output.toString();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    // create a new TextView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textV;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            textV = new TextView(mContext);

            int scaledSize = (int)mContext.getResources().getDimension(R.dimen.grid_item);
            textV.setLayoutParams(new GridView.LayoutParams(scaledSize, scaledSize));

            textV.setTextSize(TypedValue.COMPLEX_UNIT_PX, mContext.getResources().getDimension(R.dimen.grid_text));
            textV.setTypeface(null, Typeface.BOLD);
            textV.setGravity(Gravity.CENTER);
            if (Build.VERSION.SDK_INT>=21)
                textV.setElevation(4);
        }
        else {
            textV = (TextView) convertView;
        }

        // Set the following attributes here so that they are refreshed when changes are made
        textV.setText(String.valueOf(word.charAt(position)));
        if (isEnabled(position)) {
            if (position == 4 && centreTint) {
                textV.setBackground(mContext.getResources().getDrawable(R.drawable.button_round_accent));
                textV.setTextColor(mContext.getResources().getColor(android.R.color.black));
            } else {
                textV.setBackground(mContext.getResources().getDrawable(R.drawable.button_round_light));
                textV.setTextColor(mContext.getResources().getColor(R.color.gameItemText));
            }
        }
        else {
            textV.setTextColor(mContext.getResources().getColor(R.color.background));
            if (position == 4 && centreTint)
                textV.setBackground(mContext.getResources().getDrawable(R.drawable.button_round_grey));
        }

        return textV;
    }
}

