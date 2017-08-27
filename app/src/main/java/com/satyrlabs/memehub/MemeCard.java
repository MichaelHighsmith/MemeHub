package com.satyrlabs.memehub;

import android.*;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
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
public class MemeCard extends AppCompatActivity{

    //Firebase instances
    private FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference mMessagesDatabaseReference = mFirebaseDatabase.getReference();

    private FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    //private ImageButton mPhotoDownloadButton;

    @View(R.id.memeImageView)
    private ImageView memeImageView;

    @View(R.id.titlePointsTxt)
    private TextView titlePointsTxt;

    @View(R.id.usernameTxt)
    private TextView usernameTxt;

    @View(R.id.photoDownloadButton)
    private ImageView mPhotoDownloadButton;

    private Meme mMeme;
    private Context mContext;
    private SwipePlaceHolderView mSwipeView;
    private FirebaseUser user = mFirebaseAuth.getCurrentUser();
    final String userId = user.getUid();

    private boolean swipedLeft;

    public MemeCard(Context context, Meme meme, SwipePlaceHolderView swipeView){
        mContext = context;
        mMeme = meme;
        mSwipeView = swipeView;

    }

    @Resolve
    private void onResolved(){
        Glide.with(mContext).load(mMeme.getImageUrl()).asBitmap().into(memeImageView);
        titlePointsTxt.setText("   Points:  " + mMeme.getPoints());
        usernameTxt.setText(mMeme.getUsername());

        //download the current image when the user clicks the download button
        mPhotoDownloadButton.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                BitmapDrawable drawable = (BitmapDrawable) memeImageView.getDrawable();
                Bitmap bitmap = drawable.getBitmap();
                String ImagePath = MediaStore.Images.Media.insertImage(mContext.getContentResolver(), bitmap, "demo_image", "demo_image");
                Uri URI = Uri.parse(ImagePath);
                Toast.makeText(mContext, "Image saved", Toast.LENGTH_SHORT).show();
            }
        });
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

        swipedLeft = true;

        //add the meme's pushId under the list of viewed id's by the user
        logMemeAsViewed(mGroupId, swipedLeft, mMeme);



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

        swipedLeft = false;

        //add the meme's pushId under the list of viewed id's by the user
        logMemeAsViewed(mGroupId, swipedLeft, mMeme);


    }

    @SwipeInState
    private void onSwipeInState(){
        Log.d("EVENT", "onSwipeInState");
    }

    @SwipeOutState
    private void onSwipeOutState(){
        Log.d("EVENT", "onSwipeOutState");
    }


    private void logMemeAsViewed(final String memeId, boolean leftSwipe, final Meme meme){
        //Create a single event listener that will add the memeId to the list of memes already viewed by the user that is logged in
        mMessagesDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Add the current user Id to the list of usersHaveViewed that the meme has already been viewed by (this way we can avoid it from showing up again in their future feed)
                try{
                    //TODO about 1/10 times the app crashes here with null refernece to a child path.  I can't figure out why this is, but try-catch skips the event for now
                    GenericTypeIndicator<HashMap<String, String>> g = new GenericTypeIndicator<HashMap<String, String>>(){};
                    HashMap<String, String> memeList = dataSnapshot.child("messages").child(memeId).child("usersHaveViewed").getValue(g);
                    if(memeList != null){
                        memeList.put(userId, userId);
                        mMessagesDatabaseReference.child("messages").child(memeId).child("usersHaveViewed").setValue(memeList);
                    } else {
                        HashMap<String, String> introMap = new HashMap<String, String>();
                        introMap.put(userId, userId);
                        mMessagesDatabaseReference.child("messages").child(memeId).child("usersHaveViewed").setValue(introMap);
                    }
                    //If meme is swiped left, add Tally against the poster
                    if(swipedLeft){
                        //ban the poster from showing up in the user's feed anymore
                        addTallyAgainstPoster(dataSnapshot, memeId);
                    }
                    updateUsersTotalSwipes(dataSnapshot);

                    String posterId = meme.getUsernameId();
                    updatePostersTotalPoints(dataSnapshot, swipedLeft, posterId);

                } catch (Exception e){
                    Log.v("Saving the meme failed", "Idk why");
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void addTallyAgainstPoster(DataSnapshot dataSnapshot, String memeId){
        String memePoster = mMeme.getUsernameId();
        Object bannedUserObject = dataSnapshot.child("users").child(userId).child("bannedUsers").child(memePoster).child("strikes").getValue();
        if(bannedUserObject == null){
            //The user is not currently banned, so we add them to the list
            mMessagesDatabaseReference.child("users").child(userId).child("bannedUsers").child(memePoster).child("strikes").setValue(1);
        } else {
            //bannedUserObject currently equals the number of strikes against this poster, increment it and add it
            String bannedUserString = bannedUserObject.toString();
            int bannedUserTally = Integer.parseInt(bannedUserString);
            bannedUserTally++;
            mMessagesDatabaseReference.child("users").child(userId).child("bannedUsers").child(memePoster).child("strikes").setValue(bannedUserTally);
        }

    }

    private void updateUsersTotalSwipes(DataSnapshot dataSnapshot){
        String userSwipesString = dataSnapshot.child("users").child(userId).child("swipes").getValue().toString();
        int userSwipes = Integer.parseInt(userSwipesString);
        userSwipes = userSwipes + 1;
        mMessagesDatabaseReference.child("users").child(userId).child("swipes").setValue(userSwipes);
    }

    private void updatePostersTotalPoints(DataSnapshot dataSnapshot, boolean leftSwipe, String posterId){
        Object posterPointsObject = dataSnapshot.child("users").child(posterId).child("points").getValue();
        //if the posterpoints is null, add it to the user's data
        if(posterPointsObject == null){
            mMessagesDatabaseReference.child("users").child(posterId).child("points").setValue(0);
        } else {
            //If it's not null, + or - 1 point and store it
            String posterPointsString = posterPointsObject.toString();
            int posterPoints = Integer.parseInt(posterPointsString);
            if(leftSwipe){
                posterPoints = posterPoints - 1;
            } else {
                posterPoints = posterPoints + 1;
            }
            mMessagesDatabaseReference.child("users").child(posterId).child("points").setValue(posterPoints);
        }

    }



}
