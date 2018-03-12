package com.mavencluster.firebaseuser.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mavencluster.firebaseuser.R;
import com.mavencluster.firebaseuser.adapters.CustomInfoWindowAdapter;
import com.mavencluster.firebaseuser.model.UserModel;
import com.mavencluster.firebaseuser.utility.Constant;

import java.util.ArrayList;

/**
 * User list activity to show all the users on Firebase database on Map with marker..
 */
public class UserListActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static final String TAG = UserListActivity.class.getSimpleName();

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabaseReference;
    private GoogleMap mMap;

    private ArrayList<Marker> markersList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);
        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_user_list_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                logout();
                return true;
            case R.id.action_profile:
                sentToProfileActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Method to sent on Profile activity from current activity
     */
    private void sentToProfileActivity() {
        Intent intent = new Intent(UserListActivity.this, ProfileActivity.class);
        startActivity(intent);
    }

    /**
     * Method to logout from User
     */
    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(UserListActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Method to initialize views and variables
     */
    private void init() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mAuth = FirebaseAuth.getInstance();
        addMapToActivity();
    }

    /**
     * This method create a SupportMapFragment instance and add to the activity
     */
    private void addMapToActivity() {
        SupportMapFragment mapFragment = new SupportMapFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.main, mapFragment);
        fragmentTransaction.commit();
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setMinZoomPreference(5);
        CustomInfoWindowAdapter customInfoWindowAdapter = new CustomInfoWindowAdapter(this);
        mMap.setInfoWindowAdapter(customInfoWindowAdapter);

        markersList = new ArrayList<>();

        fetchAllUserValue();

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (marker.isInfoWindowShown()) {
                    marker.hideInfoWindow();
                } else {
                    marker.showInfoWindow();
                }
                return true;
            }
        });
    }

    /**
     * This method will fetch all the registered users by getting users node reference
     * from Firebase database.
     */
    private void fetchAllUserValue() {
        /*value listener to add on users node reference..*/
        ValueEventListener usersDataListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "Inside on data change");
                // get users data and add markers on map...
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    UserModel userModel = ds.getValue(UserModel.class);
                    assert userModel != null;
                    //userModel.setUserID(ds.getKey());
                    addRemoveMarkers(userModel);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // show nothing..
                Log.d(TAG, "Inside on cancel");
            }
        };



        /*get database reference..*/
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        /*add value listener on users node..*/
        mDatabaseReference.child(Constant.USER_NODE_REF).addValueEventListener(usersDataListener);

    }

    /**
     * This method add and update markers on map using user details
     *
     * @param userModel UserModel class object.
     */
    private void addRemoveMarkers(UserModel userModel) {
        boolean newUser = true;
        // add and remove markers from the map...
        for (Marker marker : markersList) {
            assert (marker.getTag()) != null;
            if (userModel.getUserID().equals(((UserModel) marker.getTag()).getUserID())) {
                newUser = false;
                /*update the marker position*/
                marker.setPosition(new LatLng(userModel.getLatitude(), userModel.getLongitude()));
            }
        }
        if (newUser) {
            /*create new marker for the user and add on map and markers list..*/
            Marker marker = addNewMarker(userModel);
            if (marker != null) {
                marker.setTag(userModel);
                markersList.add(marker);
            }

        }
    }

    /**
     * This method adds new markers on map..
     *
     * @param userModel UserModel class object.
     * @return Marker object, marker added on map.
     */
    private Marker addNewMarker(UserModel userModel) {
        Marker marker;
        BitmapDescriptor markerIcon;
        /*check is it current user..*/
        if (userModel.getUserID().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            markerIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_current_user_loc);
            marker = mMap.addMarker(new MarkerOptions().icon(markerIcon).position(new LatLng(userModel.getLatitude(), userModel.getLongitude())));
            mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(userModel.getLatitude(), userModel.getLongitude())));
        } else {
            /*not the current user*/
            if (userModel.getLatitude() != null && userModel.getLongitude() != (null)) {
                markerIcon = BitmapDescriptorFactory.fromResource(R.drawable.ic_user_loc);
                marker = mMap.addMarker(new MarkerOptions().icon(markerIcon).position(new LatLng(userModel.getLatitude(), userModel.getLongitude())));
            } else {
                /*return null if user latitude and longitude are null..*/
                return null;
            }
        }

        return marker;
    }

}
