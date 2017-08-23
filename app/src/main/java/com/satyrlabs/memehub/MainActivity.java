package com.satyrlabs.memehub;

import android.*;
import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mindorks.placeholderview.SwipeDecor;
import com.mindorks.placeholderview.SwipePlaceHolderView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    public static final int RC_PHOTO_PICKER = 2;
    public static final int RC_SIGN_IN = 1;

    private SwipePlaceHolderView mSwipeView;
    private Context mContext;

    private String mUsername;
    private String mEmail;
    private String mId;

    private ImageButton mPhotoPickerButton;

    //Firebase instance variables
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private DatabaseReference mPointsDatabaseReference;
    private ChildEventListener mChildEventListener;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatPhotosStorageReference;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private DatabaseReference mUsersDatabaseReference;

    int totalViewCount = 0;
    boolean userBanned = false;
    int totalSwipes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        totalViewCount = 0;

        //Give permission to download photos
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        mUsername = "anonymous";

        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);

        //Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();

        mMessagesDatabaseReference = mFirebaseDatabase.getReference();
        mUsersDatabaseReference = mFirebaseDatabase.getReference().child("users");

        mChatPhotosStorageReference = mFirebaseStorage.getReference().child("chat_photos");


        mSwipeView = (SwipePlaceHolderView) findViewById(R.id.swipeView);
        mContext = getApplicationContext();

        mSwipeView.getBuilder()
                .setDisplayViewCount(3)
                .setSwipeDecor(new SwipeDecor().
                        setPaddingTop(-50)
                        .setRelativeScale(0.01f)
                        .setSwipeInMsgLayoutId(R.layout.meme_swipe_right_msg_view)
                        .setSwipeOutMsgLayoutId(R.layout.meme_swipe_left_msg_view));

        setNewPhotoButton();

        //Triggered when the user's auth state changes
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){
                    //user is signed in
                    onSignedInInitialize(user.getDisplayName(), user.getEmail(), user.getUid());
                } else {
                    //user is signed out
                    onSignedOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)  //allows the phone to automatically save user credentials and log users in
                                    .setAvailableProviders(
                                            Arrays.asList(
                                                    new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                                    new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                                    .build(),
                            RC_SIGN_IN);  //RC_SIGN_IN is a flag for when we return from startActivityForResult (used in onActivityResult)
                }
            }
        };

        //Introduce the user if it's their first time
        SharedPreferences intro = getSharedPreferences("MyFirstTime", 0);
        if(intro.getBoolean("my_first_time", true)){
            introDialog();
            intro.edit().putBoolean("my_first_time", false).apply();
        }

    }

    private void setNewPhotoButton(){
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.sign_out:
                //Sign out
                AuthUI.getInstance().signOut(this);
                return true;
            case R.id.view_profile:

                //Store the user's data (for the intent)
                Bundle bundle = new Bundle();
                bundle.putString("key_username", mUsername);
                bundle.putInt("key_swipes", totalSwipes);
                //open the user's profile
                Intent intent = new Intent(MainActivity.this, UserProfile.class);
                intent.putExtras(bundle);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK){
            Uri selectedImageUri = data.getData();
            StorageReference photoRef = mChatPhotosStorageReference.child(selectedImageUri.getLastPathSegment());
            //upload the file to firebase storage
            photoRef.putFile(selectedImageUri).addOnSuccessListener
                    (this, new OnSuccessListener<UploadTask.TaskSnapshot>(){
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot){
                            Uri downloadUrl = taskSnapshot.getDownloadUrl();
                            String pushIdd = mMessagesDatabaseReference.child("messages").push().getKey();
                            Meme meme = new Meme(downloadUrl.toString(), 1, mUsername, pushIdd, mId);
                            Log.v("My pushId is", mMessagesDatabaseReference.child("messages").push().getKey());
                            mMessagesDatabaseReference.child("messages").child(pushIdd).setValue(meme);
                        }
                    });

        } else if(requestCode == RC_SIGN_IN){
            //Allow the user to exit the app if they hit back before logging in
            if(resultCode == RESULT_OK){
                Toast.makeText(this, "signed in!", Toast.LENGTH_SHORT).show();
            } else if(resultCode == RESULT_CANCELED){
                Toast.makeText(this, "Sign in cancelled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(mAuthStateListener != null){
            //if AuthstateListener is active, remove it
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        detachDatabaseReadListener();
        mSwipeView.removeAllViews();
    }

    @Override
    protected void onResume(){
        super.onResume();
        totalViewCount = 0;
        //Add the listener (checks if the user is logged in in onCreate)
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    private void onSignedInInitialize(String username, String email, String id){
        //Set the username to be displayed with posts (see photoRef.putFile)
        mUsername = username;
        mEmail = email;
        mId = id;
        //add the user's data to the "users" tree in the DB
        mMessagesDatabaseReference.child("users").child(mId).child("username").setValue(mUsername);
        mMessagesDatabaseReference.child("users").child(mId).child("email").setValue(mEmail);
        mMessagesDatabaseReference.child("users").child(mId).child("id").setValue(mId);
        //Retrieve data on the user's total points
        getUsersTotalSwipes();
        attachDatabaseReadListener();
    }

    private void onSignedOutCleanup(){
        mUsername = "anonymous";
        mSwipeView.removeAllViews();
        detachDatabaseReadListener();
    }

    private void attachDatabaseReadListener(){
        if(mChildEventListener == null){
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Meme meme = dataSnapshot.getValue(Meme.class);
                    int totalViews = mSwipeView.getChildCount();
                    String totalMemes = String.valueOf(totalViews);
                    Log.v("Total memes = ", totalMemes);
                    //only add 50 memes to the swipeView

                        //Check that the meme hasn't been viewed by the user before
                        if(!dataSnapshot.child("usersHaveViewed").child(mId).exists()){
                            //Check that the meme user isn't banned
                            checkIfBanned(meme);
                        }
                }
                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                }
                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                }
                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            };
            mMessagesDatabaseReference.child("messages").addChildEventListener(mChildEventListener);
        }
    }

    private void detachDatabaseReadListener(){
        if(mChildEventListener != null){
            mMessagesDatabaseReference.child("messages").removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

    private void checkIfBanned(final Meme meme){
        mUsersDatabaseReference = mFirebaseDatabase.getReference().child("users").child(mId);
        mUsersDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                GenericTypeIndicator<HashMap<String, String>> abc = new GenericTypeIndicator<HashMap<String, String>>(){};
                HashMap<String, String> listOfBannedUsers = dataSnapshot.child("bannedUsers").getValue(abc);
                if(listOfBannedUsers == null){
                    Log.v("I guess its null", "do nothing");
                    if(totalViewCount < 50){
                        mSwipeView.addView(new MemeCard(mContext, meme, mSwipeView));
                        totalViewCount++;
                    }
                }
                if(listOfBannedUsers != null){
                    //List of banned users exists
                    if(listOfBannedUsers.containsValue(meme.getUsernameId())){
                        Log.v("This user is banned", "B");
                    } else {
                        //If the poster isn't banned, and the meme hasn't been seen yet, add it to the swipe view.
                        if(totalViewCount < 50){
                            mSwipeView.addView(new MemeCard(mContext, meme, mSwipeView));
                            totalViewCount++;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    //retrieve the totalswipes that the user has earned. Saved into a variable for the intent bundle
    private void getUsersTotalSwipes(){
        mPointsDatabaseReference = mFirebaseDatabase.getReference();
        mPointsDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Object totalSwipesObject = dataSnapshot.child("users").child(mId).child("swipes").getValue();
                if(totalSwipesObject == null){
                    mPointsDatabaseReference.child("users").child(mId).child("swipes").setValue(0);
                } else {
                    String totalSwipesString = totalSwipesObject.toString();
                    Log.v("Total swipes equal", totalSwipesString);
                    totalSwipes = Integer.parseInt(totalSwipesString);
                    mPointsDatabaseReference.child("users").child(mId).child("swipes").setValue(totalSwipes);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    public void introDialog(){
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.intro_dialog);
        dialog.setTitle("Welcome to MemeHub");
        dialog.setCancelable(false);

        final Button button = (Button) dialog.findViewById(R.id.intro_ok_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

}
