package com.satyrlabs.memehub;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mindorks.placeholderview.SwipeDecor;
import com.mindorks.placeholderview.SwipePlaceHolderView;

import java.util.Arrays;

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
    private ChildEventListener mChildEventListener;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatPhotosStorageReference;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    int totalViewCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        totalViewCount = 0;

        mUsername = "anonymous";

        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);

        //Initialize Firebase components
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();

        mMessagesDatabaseReference = mFirebaseDatabase.getReference();
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




        mPhotoPickerButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });


        //Triggered when the user's auth state changes
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){
                    //user is signed in
                    onSignedInInitialize(user.getDisplayName(), user.getEmail(), user.getUid());
                    Toast.makeText(MainActivity.this, "Welcome to MemeHub!  Swipe left or right to start refining your meme feed", Toast.LENGTH_LONG).show();
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
            case R.id.add_image:
                //add an image
                //TODO implement adding image (To storage/database) (currently in the bottom photo button)
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
                            Meme meme = new Meme("title", downloadUrl.toString(), 1, mUsername, pushIdd);
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
                    Log.v("meme is", meme.toString());
                    int totalViews = mSwipeView.getChildCount();
                    String totalMemes = String.valueOf(totalViews);
                    Log.v("Total memes = ", totalMemes);
                    //only add 10 memes to the swipeView
                    if (totalViewCount < 10){
                        mSwipeView.addView(new MemeCard(mContext, meme, mSwipeView));
                        totalViewCount++;
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


}
