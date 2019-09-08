package za.co.twinc.a9letterjumble;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by wilco on 2019/05/01.
 * 9LetterJumble
 */

public class GameViewHolder extends ViewHolder<Integer> {

    //private final ImageView gameImage   = getView().findViewById(R.id.game_image);

    private GameViewHolder(View itemView)
    {
        super(itemView);
    }

    @NonNull
    public static GameViewHolder make(ViewGroup parent)
    {
        LayoutInflater viewInflater = LayoutInflater.from(parent.getContext());
        View gameListItemView = viewInflater.inflate(R.layout.game_item_layout,parent,false);
        return new GameViewHolder(gameListItemView);
    }

    @Override
    protected void onSetModel(Integer position)
    {

    }


}