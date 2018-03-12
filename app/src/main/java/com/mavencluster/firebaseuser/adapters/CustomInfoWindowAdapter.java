package com.mavencluster.firebaseuser.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.mavencluster.firebaseuser.R;
import com.mavencluster.firebaseuser.activities.ProfileActivity;
import com.mavencluster.firebaseuser.model.UserModel;

/**
 * Created by Deepak on 05/03/2018.
 * Custom Adapter for info window on map.
 */

public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
    private Context context;

    public CustomInfoWindowAdapter(Context context) {
        this.context = context;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null);
        return getInfoWindowView(marker, view);
    }

    private View getInfoWindowView(Marker marker, View view) {
        UserModel userModel = (UserModel) marker.getTag();
        TextView tvFullName = view.findViewById(R.id.tv_full_name);
        TextView tvMobile = view.findViewById(R.id.tv_mobile);
        TextView tvAddress = view.findViewById(R.id.tv_address);
        ImageView ivProfile = view.findViewById(R.id.iv_profile);
        if (userModel != null) {
            tvFullName.setText(userModel.getFullName());
            tvMobile.setText(userModel.getMobileNumber());
            tvAddress.setText(userModel.getAddress());
            if (userModel.getProfilePic() != null) {
                RequestOptions requestOptions = new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL).error(R.drawable.img_no_image);
                Glide.with(context).load(userModel.getProfilePic()).apply(requestOptions).into(ivProfile);
            }
        }
        return view;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}
