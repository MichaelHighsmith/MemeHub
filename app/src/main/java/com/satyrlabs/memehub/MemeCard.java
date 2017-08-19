package com.satyrlabs.memehub;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mhigh on 8/17/2017.
 */

@Layout(R.layout.meme_card_view)
@NonReusable
public class MemeCard {

    //Firebase instances
    private FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference mMessagesDatabaseReference = mFirebaseDatabase.getReference();

    private FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    @View(R.id.memeImageView)
    private ImageView memeImageView;

    @View(R.id.titlePointsTxt)
    private TextView titlePointsTxt;

    @View(R.id.usernameTxt)
    private TextView usernameTxt;

    private Meme mMeme;
    private Context mContext;
    private SwipePlaceHolderView mSwipeView;
    private FirebaseUser user = mFirebaseAuth.getCurrentUser();
    final String userId = user.getUid();

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

        //add the meme's pushId under the list of viewed id's by the user
        logMemeAsViewed(mGroupId);
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
        final String mGroupId = mMessagesDatabaseReference.child("messages").child(mMeme.getPushIdd()).getKey();
        Log.v("mGroupId = ", mGroupId);
        mMessagesDatabaseReference.child("messages").child(mGroupId).child("points").setValue(currentPoints);

        //add the meme's pushId under the list of viewed id's by the user
        logMemeAsViewed(mGroupId);


    }

    @SwipeInState
    private void onSwipeInState(){
        Log.d("EVENT", "onSwipeInState");
    }

    @SwipeOutState
    private void onSwipeOutState(){
        Log.d("EVENT", "onSwipeOutState");
    }


    private void logMemeAsViewed(final String memeId){
        //Create a single event listener that will add the memeId to the list of memes already viewed by the user that is logged in
        mMessagesDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Add the current meme's Id to the list of memes that the user has already seen (this way we can avoid it from showing up again in their future feed)
                try{
                    //TODO about 1/10 times the app crashes here with null refernece to a child path.  I can't figure out why this is, but try-catch skips the event for now
                    GenericTypeIndicator<HashMap<String, String>> g = new GenericTypeIndicator<HashMap<String, String>>(){};
                    HashMap<String, String> memeList = dataSnapshot.child("users").child(userId).child("memesViewed").getValue(g);
                    if(memeList != null){
                        memeList.put(memeId, memeId);
                        mMessagesDatabaseReference.child("users").child(userId).child("memesViewed").setValue(memeList);
                    } else {
                        HashMap<String, String> dummyMap = new HashMap<String, String>();
                        dummyMap.put("dummyData", "dummyData");
                        mMessagesDatabaseReference.child("users").child(userId).child("memesViewed").setValue(dummyMap);
                    }
                } catch (Exception e){
                    Log.v("Saving the meme failed", "Idk why");
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

}
