package com.droiddigger.firebaseautho;

import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "AndroidBash";
    public static final String JOKES = "jokes";

    private String mUsername;
    private String mPhotoUrl;
    private EditText mJokeEditText;
    private Button mPostButton;

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private FirebaseRecyclerAdapter<Joke, MyJokeViewHolder> mFirebaseAdapter;
    private ProgressBar mProgressBar;
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    private GoogleApiClient mGoogleApiClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser == null) {
            // User has  not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            mUsername = mFirebaseUser.getDisplayName();
            if (mFirebaseUser.getPhotoUrl() != null)
                mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
        }

        //The main entry point for Google Play services integration. Builder to configure a GoogleApiClient is required.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mRecyclerView = (RecyclerView) findViewById(R.id.mRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        //Gets a Reference to your Firebase Database.
        // You should have included a google-services.json (downloaded from Firebase console) under "app" folder of your project.
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        //Adapter for Firebase RecyclerView : FirebaseRecyclerAdapter
        /**
         * @param modelClass Firebase will marshall the data at a location into an instance of a class that you provide
         * @param modelLayout This is the layout used to represent a single item in the list. You will be responsible for populating an
         *                    instance of the corresponding view with the data from an instance of modelClass.
         * @param viewHolderClass The class that hold references to all sub-views in an instance modelLayout.
         * @param ref        The Firebase location to watch for data changes. Can also be a slice of a location, using some
         *                   combination of <code>limit()</code>, <code>startAt()</code>, and <code>endAt()
         */
        mFirebaseAdapter = new FirebaseRecyclerAdapter<Joke, MyJokeViewHolder>(
                Joke.class,
                R.layout.item_joke_layout,
                MyJokeViewHolder.class,
                mFirebaseDatabaseReference.child(JOKES)) {

            @Override
            protected void populateViewHolder(MyJokeViewHolder viewHolder, final Joke joke, int position) {
                /**
                 * Each time the data at the given Firebase location changes, this method will be called for each item that needs
                 * to be displayed. The first two arguments correspond to the mLayout and mModelClass given to the constructor of
                 * this class. The third argument is the item's position in the list.
                 * <p>
                 * Your implementation should populate the view using the data contained in the model.
                 *
                 * @param viewHolder The view to populate
                 * @param model      The object containing the data used to populate the view
                 * @param position  The position in the list of the view being populated
                 */
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                viewHolder.jokeTextView.setText(joke.text);
                viewHolder.jokeAuthorTextView.setText(joke.name);
                viewHolder.likeCount.setText(joke.likeCount + "");
                //Before loading jokes from firebase database to the RecyclerView, we check whether the current user has
                //liked the joke or not. Depending on true/false we set the likes button.
                //likesGivenBy is a Map<String,Boolean> which stores uid and true/false (user has liked or not).
                if (joke.likesGivenBy.get(mFirebaseUser.getUid()) == null) {
                    viewHolder.likeButton.setBackgroundResource(R.drawable.ic_favorite_border_black_24dp);
                } else {
                    viewHolder.likeButton.setBackgroundResource(R.drawable.ic_favorite_black_24dp);
                }
                //If the User has no google profile picture we set a default image.
                if (joke.photoUrl == null) {
                    viewHolder.circleImageView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this,
                            R.drawable.ic_face_black_24dp));
                } else {
                    //Loads the image in to circleImageView from the given URL
                    Glide.with(MainActivity.this)
                            .load(joke.photoUrl)
                            .into(viewHolder.circleImageView);
                }

                //OnClickListener for like button.
                //runTransaction method : Runs a transaction on the data at this location.
                //Parameters:
                //handler - An object to handle running the transaction
                viewHolder.likeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mFirebaseDatabaseReference.child(JOKES).child(joke.jokeKey).runTransaction(new Transaction.Handler() {
                            @Override
                            public Transaction.Result doTransaction(MutableData currentData) {
                                /*Transaction.Result doTransaction(MutableData currentData)
                                This method will be called, possibly multiple times, with the current data at this location.
                                It is responsible for inspecting that data and returning a (Transaction.Result) specifying either the desired new data at the location or that the transaction should be aborted.
                                Since this method may be called repeatedly for the same transaction, be extremely careful of any side effects
                                that may be triggered by this method. In addition, this method is called from within the Firebase library's run loop,
                                so care is also required when accessing data that may be in use by other threads in your application.


                                Parameters:
                                currentData - The current data at the location. Update this to the desired data at the location
                                Returns:
                                Either the new data, or an indication to abort the transaction*/

                                Joke joke = currentData.getValue(Joke.class);
                                if (joke == null) {
                                    return Transaction.success(currentData);
                                }

                                if (joke.likesGivenBy.containsKey(mFirebaseUser.getUid())) {
                                    Log.i("AndroidBash", "User has already Liked. So it can be considered as Unliked.");
                                    joke.likeCount = joke.likeCount - 1;
                                    joke.likesGivenBy.remove(mFirebaseUser.getUid());
                                } else {
                                    Log.i("AndroidBash", "User Liked");
                                    joke.likeCount = joke.likeCount + 1;
                                    joke.likesGivenBy.put(mFirebaseUser.getUid(), true);
                                }

                                // Set value and report transaction success
                                currentData.setValue(joke);
                                return Transaction.success(currentData);
                            }

                            @Override
                            public void onComplete(DatabaseError databaseError, boolean b,
                                                   DataSnapshot dataSnapshot) {
                                // Transaction completed
                                Log.d(TAG, "postTransaction:onComplete:" + databaseError);
                            }
                        });
                    }
                });

            }
        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int jokeCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the user is at the bottom of the list, scroll
                // to the bottom of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (jokeCount - 1) && lastVisiblePosition == (positionStart - 1))) {
                    mRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setAdapter(mFirebaseAdapter);


        mJokeEditText = (EditText) findViewById(R.id.post_joke_et);
        mJokeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int j, int k) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int j, int k) {
                if (charSequence.toString().trim().length() > 0) {
                    mPostButton.setEnabled(true);
                } else {
                    mPostButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mPostButton = (Button) findViewById(R.id.button);
        mPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Map<String, Boolean> likesGivenBy = new HashMap<>();
                //Key under which a joke will be stored. Firebase assigns a unique id for every new joke pushed.
                String key = mFirebaseDatabaseReference.child(JOKES).push().getKey();
                //Creates a new Joke object
                Joke joke = new Joke(mFirebaseUser.getUid(), mJokeEditText.getText().toString(), mUsername,
                        mPhotoUrl, key, likesGivenBy);
                //New Map will be created from a Joke object.
                Map<String, Object> values = joke.toMap();
                //Creates a new Map and puts the Map (joke object) which is to be stored in Firebase Database.
                Map<String, Object> childUpdates = new HashMap<>();
                childUpdates.put("/jokes/" + key, values);

                //updateChildren method inserts the Joke under "/jokes/{key}" node.
                mFirebaseDatabaseReference.updateChildren(childUpdates);
                //EditText filed will be set back "" once the joke is posted.
                mJokeEditText.setText("");
            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.sign_out_menu:
                mFirebaseAuth.signOut();
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                mFirebaseUser = null;
                mUsername = null;
                mPhotoUrl = null;
                startActivity(new Intent(this, SignInActivity.class));
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }


    public static class MyJokeViewHolder extends RecyclerView.ViewHolder {
        public TextView jokeTextView;
        public TextView jokeAuthorTextView;
        public CircleImageView circleImageView;
        public ImageButton likeButton;
        public TextView likeCount;

        public MyJokeViewHolder(View v) {
            super(v);
            jokeTextView = (TextView) itemView.findViewById(R.id.jokeTextView);
            jokeAuthorTextView = (TextView) itemView.findViewById(R.id.jokeAuthorTextView);
            circleImageView = (CircleImageView) itemView.findViewById(R.id.circleImageView);
            likeButton = (ImageButton) itemView.findViewById(R.id.like_button);
            likeCount = (TextView) itemView.findViewById(R.id.like_count);
        }
    }
}
