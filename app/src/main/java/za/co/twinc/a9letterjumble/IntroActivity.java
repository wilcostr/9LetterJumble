package za.co.twinc.a9letterjumble;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntroFragment;

/**
 * Created by wilco on 2018/03/08.
 * 9LetterJumble
 */

public class IntroActivity extends AppIntro2 {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Note here that we DO NOT use setContentView();
        addSlide(AppIntroFragment.newInstance(getString(R.string.intro_title_1),
                "normal",
                getString(R.string.intro_desc_1),
                "normal",
                R.drawable.intro_02,
                getResources().getColor(R.color.colorPrimaryDark),
                getResources().getColor(android.R.color.white),
                getResources().getColor(android.R.color.white)));

        addSlide(AppIntroFragment.newInstance(getString(R.string.intro_title_2),
                "normal",
                getString(R.string.intro_desc_2),
                "normal",
                R.drawable.intro_02,
                getResources().getColor(R.color.colorPrimaryDark),
                getResources().getColor(android.R.color.white),
                getResources().getColor(android.R.color.white)));

        addSlide(AppIntroFragment.newInstance(getString(R.string.intro_title_3),
                "normal",
                getString(R.string.intro_desc_3),
                "normal",
                R.drawable.intro_03,
                getResources().getColor(R.color.colorPrimaryDark),
                getResources().getColor(android.R.color.white),
                getResources().getColor(android.R.color.white)));

        showSkipButton(false);
    }


    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        finish();
    }
}
