package com.satyrlabs.memehub;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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

    //Firebase instances
    private FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference mMessagesDatabaseReference = mFirebaseDatabase.getReference();

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
        //get points and subtract 1
        int currentPoints = mMeme.getPoints() - 1;
        //get the pushId and then navigate using it to update the points value for the meme
        String mGroupId = mMessagesDatabaseReference.child("messages").child(mMeme.getPushIdd()).getKey();
        Log.v("mGroupId = ", mGroupId);
        mMessagesDatabaseReference.child("messages").child(mGroupId).child("points").setValue(currentPoints);
    }

    @SwipeCancelState
    private void onSwipeCancelState(){
        Log.d("EVENT", "onSwipeCancelState");
    }

    @SwipeIn
    private void onSwipeIn(){
        Log.d("EVENT", "onSwipedIn");
        //get points and add 1
        int currentPoints = mMeme.getPoints() + 1;
        //get the pushId and then navigate using it to update the points value for the meme
        String mGroupId = mMessagesDatabaseReference.child("messages").child(mMeme.getPushIdd()).getKey();
        Log.v("mGroupId = ", mGroupId);
        mMessagesDatabaseReference.child("messages").child(mGroupId).child("points").setValue(currentPoints);
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
