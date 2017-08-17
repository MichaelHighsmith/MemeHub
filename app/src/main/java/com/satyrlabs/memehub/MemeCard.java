package com.satyrlabs.memehub;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.mindorks.placeholderview.SwipePlaceHolderView;
import com.mindorks.placeholderview.annotations.Layout;
import com.mindorks.placeholderview.annotations.NonReusable;
import com.mindorks.placeholderview.annotations.Resolve;
import com.mindorks.placeholderview.annotations.View;
import com.mindorks.placeholderview.annotations.swipe.SwipeCancelState;
import com.mindorks.placeholderview.annotations.swipe.SwipeIn;
import com.mindorks.placeholderview.annotations.swipe.SwipeInState;
import com.mindorks.placeholderview.annotations.swipe.SwipeOut;
import com.mindorks.placeholderview.annotations.swipe.SwipeOutState;

/**
 * Created by mhigh on 8/17/2017.
 */

@Layout(R.layout.meme_card_view)
@NonReusable
public class MemeCard {

    @View(R.id.memeImageView)
    private ImageView memeImageView;

    @View(R.id.titlePointsTxt)
    private TextView titlePointsTxt;

    @View(R.id.usernameTxt)
    private TextView usernameTxt;

    private Meme mMeme;
    private Context mContext;
    private SwipePlaceHolderView mSwipeView;

    public MemeCard(Context context, Meme meme, SwipePlaceHolderView swipeView){
        mContext = context;
        mMeme = meme;
        mSwipeView = swipeView;
    }

    @Resolve
    private void onResolved(){
        Glide.with(mContext).load(mMeme.getImageUrl()).into(memeImageView);
        titlePointsTxt.setText(mMeme.getTitle() + "   Points:  " + mMeme.getPoints());
        usernameTxt.setText(mMeme.getUsername());
    }

    @SwipeOut
    private void onSwipedOut(){
        Log.d("EVENT", "onSwipedOut");
    }

    @SwipeCancelState
    private void onSwipeCancelState(){
        Log.d("EVENT", "onSwipeCancelState");
    }

    @SwipeIn
    private void onSwipeIn(){
        Log.d("EVENT", "onSwipedIn");
        Toast.makeText(mContext.getApplicationContext(), "Accepted", Toast.LENGTH_SHORT).show();
    }

    @SwipeInState
    private void onSwipeInState(){
        Log.d("EVENT", "onSwipeInState");
    }

    @SwipeOutState
    private void onSwipeOutState(){
        Log.d("EVENT", "onSwipeOutState");
    }

}
