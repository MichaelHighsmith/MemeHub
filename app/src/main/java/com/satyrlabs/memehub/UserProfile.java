package com.satyrlabs.memehub;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by mhigh on 8/22/2017.
 */

public class UserProfile extends AppCompatActivity{

    private TextView usernameTV;
    private TextView swipesTV;
    private TextView pointsTV;

    private DatabaseReference mPointsDatabaseReference;
    private FirebaseDatabase mFirebaseDatabase;

    int totalPoints;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile);

        mFirebaseDatabase = FirebaseDatabase.getInstance();

        Bundle bundle = getIntent().getExtras();
        String username = bundle.getString("key_username");
        int totalSwipes = bundle.getInt("key_swipes");

        getUsersTotalPoints();

        setTextViews(username, totalSwipes);
    }

    private void setTextViews(String username, int totalSwipes){
        usernameTV = (TextView) findViewById(R.id.usernameTV);
        usernameTV.setText(username);

        swipesTV = (TextView) findViewById(R.id.swipesTV);
        String totalSwipesString = String.valueOf(totalSwipes);
        swipesTV.setText("Swipes: " + totalSwipesString);

    }

    private void getUsersTotalPoints(){

        FirebaseAuth mFirebaseAuth;
        mFirebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mFirebaseAuth.getCurrentUser();
        final String mId = user.getUid();

        mPointsDatabaseReference = mFirebaseDatabase.getReference();
        mPointsDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Object totalPointsObject = dataSnapshot.child("users").child(mId).child("points").getValue();
                if(totalPointsObject == null){
                    mPointsDatabaseReference.child("users").child(mId).child("points").setValue(0);
                } else {
                    String totalPointsString = totalPointsObject.toString();
                    Log.v("Total points equal", totalPointsString);
                    totalPoints = Integer.parseInt(totalPointsString);
                    mPointsDatabaseReference.child("users").child(mId).child("points").setValue(totalPoints);
                }

                //update the points TV
                pointsTV = (TextView) findViewById(R.id.pointsTV);
                String totalPointsString = String.valueOf(totalPoints);
                pointsTV.setText(totalPointsString);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}
