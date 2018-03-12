package com.mavencluster.firebaseuser.activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;

import android.content.Intent;

import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.FileProvider;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mavencluster.firebaseuser.R;
import com.mavencluster.firebaseuser.model.UserModel;
import com.mavencluster.firebaseuser.myInterface.ImageDialogListener;
import com.mavencluster.firebaseuser.utility.ConnectionUtil;
import com.mavencluster.firebaseuser.utility.Constant;
import com.mavencluster.firebaseuser.utility.DialogUtil;
import com.soundcloud.android.crop.Crop;

import java.io.File;
import java.util.ArrayList;

import static android.Manifest.permission_group.LOCATION;
import static com.google.android.gms.common.ConnectionResult.SERVICE_MISSING;
import static com.google.android.gms.common.ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED;

/**
 * Profile activity to Add/Update user data on Firebase database
 */
public class ProfileActivity extends AppCompatActivity implements View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        ImageDialogListener {

    public static final String TAG = ProfileActivity.class.getSimpleName();

    /**
     * Id to identity LOCATION permission request.
     */
    private static final int REQUEST_LOCATION_PERMISSION = 0;
    private final int PLACE_PICKER_REQUEST = 101;
    private final int REQUEST_CODE_CLICK_IMAGE = 102;
    private final int REQUEST_PLAY_SERVICES_RESOLUTION = 103;
    private final int REQUEST_ACCESS_MEDIA_PERMISSION = 104;

    private GoogleApiClient mGoogleApiClient;
    private FirebaseUser user;
    private DatabaseReference mDatabaseReference;
    private StorageReference mStorageReference;
    private ValueEventListener userDetailEventListener;
    private UserModel userModel;

    private EditText etFullName;
    private EditText etDOB;
    private EditText etAddress;
    private EditText etMobileNo;
    private ImageView ivProfile;
    private ImageView ivEditProfile;
    private Spinner spGender;
    private View mProgressView;
    private View mProfileView;
    private TextInputLayout tilAddress;
    private String sGender;
    private Double latitude, longitude;
    private ArrayList<String> genderList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Method to initialize views and variables
     */
    private void init() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        userModel = new UserModel();

        /*Initialize firebase user instance with current user..*/
        user = FirebaseAuth.getInstance().getCurrentUser();

        /*Get firebase database reference..*/
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mStorageReference = FirebaseStorage.getInstance().getReference();

        /*Create google API client to access play services..*/
        createGoogleApiClient();

        /*Initialize array list for gender. Better to define this list in XML file..*/
        genderList = new ArrayList<String>();
        genderList.add("Male");
        genderList.add("Female");
        genderList.add("Other");


        mProgressView = findViewById(R.id.progress_bar);
        mProfileView = findViewById(R.id.ll_profile_form);
        ivProfile = findViewById(R.id.iv_profile);
        ivEditProfile = findViewById(R.id.iv_edit_profile);
        ivEditProfile.setOnClickListener(this);
        etFullName = findViewById(R.id.et_name);
        etMobileNo = findViewById(R.id.et_mobile);
        etDOB = findViewById(R.id.et_dob);
        etAddress = findViewById(R.id.et_address);
        etAddress.setOnClickListener(this);
        tilAddress = findViewById(R.id.til_address);
        Button btnUpdate = findViewById(R.id.btn_update);
        btnUpdate.setOnClickListener(this);

        spGender = findViewById(R.id.sp_gender);
        /*Using default Array Adapter to show gender list in spinner*/
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>
                (this, R.layout.layout_spinner, genderList);
        spinnerArrayAdapter.setDropDownViewResource(R.layout.layout_spinner);
        /*set adapter on spinner..*/
        spGender.setAdapter(spinnerArrayAdapter);
        spGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                /*on user selection change gender value*/
                sGender = genderList.get(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                /*do nothing if user do not select any value*/

            }
        });

        /* Add value listener to get updated value of user and show in profile view..*/
        userDetailEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                showProgress(false);
                userModel = dataSnapshot.getValue(UserModel.class);
                if (userModel != null) {
                    etFullName.setText(userModel.getFullName());
                    etMobileNo.setText(userModel.getMobileNumber());
                    etDOB.setText(userModel.getDateOfBirth());
                    etAddress.setText(userModel.getAddress());
                    latitude = userModel.getLatitude();
                    longitude = userModel.getLongitude();
                    sGender = userModel.getGender();
                    for (int i = 0; i < genderList.size(); i++) {
                        if (genderList.get(i).equals(sGender)) {
                            /*previous value matched with gander list. set selected value in spinner*/
                            spGender.setSelection(i);
                        }
                    }
                    if (userModel.getProfilePic() != null && !userModel.getProfilePic().isEmpty()) {
                        Glide.with(ProfileActivity.this).load(userModel.getProfilePic()).apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL).error(R.drawable.img_no_image)).into(ivProfile);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showProgress(false);
                /*Do nothing */
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        /*set listener on user node in firebase database..*/
        if (user != null) {
            mDatabaseReference.child(Constant.USER_NODE_REF).child(user.getUid()).addValueEventListener(userDetailEventListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        /* remove listener on user node in firebase database..*/
        if (user != null) {
            mDatabaseReference.child(Constant.USER_NODE_REF).child(user.getUid()).removeEventListener(userDetailEventListener);
        }
    }

    /**
     * Check google play service is installed or updated..
     */
    private boolean checkGoogleService() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
        return resultCode == ConnectionResult.SUCCESS;
    }

    /**
     * This method create google api client and connect it..
     */
    private void createGoogleApiClient() {
        if (checkGoogleService()) {
            // play service is installed and updated on phone..
            if (mGoogleApiClient == null)
                mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API).build();
            mGoogleApiClient.connect();
        } else {
            // play service is not installed or  updated on phone..
            Snackbar.make(mProfileView, R.string.error_no_play_services, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            // sent to play store to update google play services..
                            sentToGooglePlayServiceUpdateScreen();
                        }
                    });
        }
    }

    /**
     * This method sent user to play store for google play service installation or update..
     */
    private void sentToGooglePlayServiceUpdateScreen() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (googleApiAvailability.isUserResolvableError(resultCode)) {
            switch (resultCode) {

                case SERVICE_VERSION_UPDATE_REQUIRED:
                    /*service update required*/
                    googleApiAvailability.showErrorDialogFragment(ProfileActivity.this, resultCode, REQUEST_PLAY_SERVICES_RESOLUTION);
                    break;
                case SERVICE_MISSING:
                    /*service is missing*/
                    googleApiAvailability.showErrorDialogFragment(ProfileActivity.this, resultCode, REQUEST_PLAY_SERVICES_RESOLUTION);
                    break;
            }

        }
    }

    /**
     * This method checks access permission for location and request permission from user if not.
     */
    private boolean mayRequestLocation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(LOCATION)) {
            Snackbar.make(mProfileView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{LOCATION}, REQUEST_LOCATION_PERMISSION);
                        }
                    });
        } else {
            requestPermissions(new String[]{LOCATION}, REQUEST_LOCATION_PERMISSION);
        }
        return false;
    }

    /**
     * This method checks access permission for external storage and request permission user if not.
     */
    private boolean mayRequestWritePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Snackbar.make(mProfileView, R.string.permission_rationale_media_access, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_ACCESS_MEDIA_PERMISSION);
                        }
                    });
        } else {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_ACCESS_MEDIA_PERMISSION);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation();
            }
        }
        if (requestCode == REQUEST_ACCESS_MEDIA_PERMISSION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addImageDialog();
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(data, this);
                etAddress.setText(place.getAddress());
                latitude = place.getLatLng().latitude;
                longitude = place.getLatLng().longitude;
            }
        } else if (requestCode == Crop.REQUEST_PICK && resultCode == RESULT_OK) {
            beginCrop(data.getData());
        } else if (requestCode == REQUEST_CODE_CLICK_IMAGE && resultCode == RESULT_OK) {
            beginCrop(Uri.fromFile(new File(getExternalCacheDir(), "cropped.jpg")));
        } else if (requestCode == Crop.REQUEST_CROP) {
            handleCrop(resultCode, data);
        }
    }

    /**
     * Method to start image crop..
     *
     * @param source Uri to image to be cropped.
     */
    private void beginCrop(Uri source) {
        new File(getExternalCacheDir(), "cropped.jpg").deleteOnExit();
        Uri destination = Uri.fromFile(new File(getExternalCacheDir(), "cropped.jpg"));
        Crop.of(source, destination).asSquare().start(this);
    }

    /**
     * Method to handle intent return from crop activity
     *
     * @param resultCode Integer type result code to recognize the request
     * @param result     Intent returned by crop activity
     */
    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            uploadProfile(getRealPathFromURI(Crop.getOutput(result)));
        } else if (resultCode == Crop.RESULT_ERROR) {
            Snackbar.make(mProfileView, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();

        }
    }

    /**
     * This method get user last known location using fused location provider API..
     */
    private void getUserLocation() {
        if (mayRequestLocation()) {
            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                @SuppressLint("MissingPermission") // We have already checked location permission..
                        Location userLocation = LocationServices.getFusedLocationProviderClient(this).getLastLocation().getResult();
                if (userLocation != null) {
                    latitude = userLocation.getLatitude();
                    longitude = userLocation.getLongitude();
                }
            } else {
                createGoogleApiClient();
            }
        }
    }

    /**
     * This method open default place selector provided by google place API to select address..
     */
    private void openPlaceSelector() {
        try {
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesNotAvailableException e) {
            Snackbar.make(mProfileView, R.string.error_no_play_services, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            // sent to play store for play service install
                            sentToGooglePlayServiceUpdateScreen();
                        }
                    });
        } catch (GooglePlayServicesRepairableException e) {
            Snackbar.make(mProfileView, R.string.error_no_play_services, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            // sent to play store for play service update
                            sentToGooglePlayServiceUpdateScreen();
                        }
                    });
        }
    }

    /**
     * This method validate and update user detail on firebase server
     */
    private void updateUserDetail() {
        etFullName.setError(null);
        etMobileNo.setError(null);
        etAddress.setError(null);
        etDOB.setError(null);

        boolean cancel = false;
        View focusView = null;

        String fullName = etFullName.getText().toString().trim();
        String mobileNo = etMobileNo.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String dob = etDOB.getText().toString().trim();

        if (TextUtils.isEmpty(address)) {
            cancel = true;
            focusView = etAddress;
            tilAddress.setError(getString(R.string.error_field_required));
        }
        if (TextUtils.isEmpty(dob)) {
            cancel = true;
            focusView = etDOB;
            etDOB.setError(getString(R.string.error_field_required));
        }
        if (TextUtils.isEmpty(mobileNo)) {
            cancel = true;
            focusView = etMobileNo;
            etMobileNo.setError(getString(R.string.error_field_required));
        }
        if (TextUtils.isEmpty(fullName)) {
            cancel = true;
            focusView = etFullName;
            etFullName.setError(getString(R.string.error_field_required));
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            if(userModel==null){
                userModel = new UserModel();
            }
            /*Validation success. Update values on Firebase database...*/
            userModel.setUserID(user.getUid());
            userModel.setFullName(fullName);
            userModel.setMobileNumber(mobileNo);
            userModel.setAddress(address);
            userModel.setDateOfBirth(dob);
            userModel.setLatitude(latitude);
            userModel.setLongitude(longitude);
            userModel.setGender(sGender);

            if (ConnectionUtil.isInternetOn(this)) {
                showProgress(true);
                //update profile...
                mDatabaseReference.child(Constant.USER_NODE_REF).child(user.getUid()).setValue(userModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        /*Update successful
                         * Show user success message..*/
                        showProgress(false);
                        Snackbar.make(mProfileView, R.string.profile_update_success, Snackbar.LENGTH_LONG).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        /*Update failed
                         * Show user failure message..*/
                        showProgress(false);
                        Snackbar.make(mProfileView, R.string.profile_update_fail, Snackbar.LENGTH_LONG).show();
                    }
                });
            } else {
                // check internet setting
                Snackbar.make(mProfileView, R.string.no_internet_connection, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void addImageDialog() {
        if (mayRequestWritePermission()) {
            Dialog dialog = DialogUtil.addImageDialog(ProfileActivity.this, this);
            dialog.show();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_update:
                // validate and update user detail..
                updateUserDetail();
                break;
            case R.id.et_address:
                openPlaceSelector();
                break;
            case R.id.iv_edit_profile:
                // select new image..
                addImageDialog();
                break;
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.

        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mProfileView.setVisibility(show ? View.GONE : View.VISIBLE);
        mProfileView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProfileView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        getUserLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, R.string.google_api_connection_suspended, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, R.string.google_api_connection_failed, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraSelection() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        File file = new File(getExternalCacheDir(), "cropped.jpg");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this, getString(R.string.file_provider_authority), file));
        startActivityForResult(intent, REQUEST_CODE_CLICK_IMAGE);
    }

    @Override
    public void onGallerySelection() {
        Crop.pickImage(ProfileActivity.this);
    }

    /**
     * Method to get file path in string from URI
     *
     * @param contentURI URI to get path from.
     */
    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    /**
     * Method to upload image file on firebase storage..
     *
     * @param filePath String filepath of file to be upload
     */
    private void uploadProfile(String filePath) {
        if (ConnectionUtil.isInternetOn(this)) {
            if (filePath != null) {
                File imageFile = new File(filePath);
                showProgress(true);
                // Send image to fire base storage and save url in database..
                Uri file = Uri.fromFile(imageFile);
                StorageReference sImageRef = mStorageReference.child(Constant.IMAGES + "/" + FirebaseAuth.getInstance().getCurrentUser() + System.currentTimeMillis() + ".jpg");
                // Create the file metadata
                StorageMetadata metadata = new StorageMetadata.Builder().setContentType("image/jpg").build();
                UploadTask uploadTask = sImageRef.putFile(file, metadata);

                // Register observers to listen for when the download is done or if it fails
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        showProgress(false);
                        // Handle unsuccessful uploads
                        Snackbar.make(mProfileView, R.string.error_in_getting_image, Snackbar.LENGTH_LONG).show();
                        exception.printStackTrace();
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        addUrlToUserData(downloadUrl);
                    }
                });
            } else {
                Snackbar.make(mProfileView, R.string.error_in_getting_image, Snackbar.LENGTH_LONG).show();
            }
        } else {
            Snackbar.make(mProfileView, R.string.no_internet_connection, Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * Method add download url to user data on Firebase database.
     *
     * @param url Image download uri..
     */
    private void addUrlToUserData(Uri url) {
        showProgress(true);
        mDatabaseReference.child(Constant.USER_NODE_REF).child(user.getUid()).child(Constant.PROFILE_PIC).setValue(url.toString()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // show message to user..
                showProgress(false);
                Snackbar.make(mProfileView, R.string.image_upload_success, Snackbar.LENGTH_LONG).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // show failue message to user..
                showProgress(false);
                e.printStackTrace();
                Snackbar.make(mProfileView, R.string.error_in_getting_image, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
