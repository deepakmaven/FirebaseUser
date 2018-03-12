package com.mavencluster.firebaseuser.utility;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.mavencluster.firebaseuser.R;
import com.mavencluster.firebaseuser.myInterface.ImageDialogListener;

/**
 * Created by Deepak on 26/02/2018.
 * Class to build common dialog used in the project..
 */

public class DialogUtil {

    public static Dialog addImageDialog(Context context, final ImageDialogListener imageDialogListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        // Get the layout inflater
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_add_image, null);
        TextView tvCamera = view.findViewById(R.id.tv_from_camera);
        tvCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // send camera selection..
                imageDialogListener.onCameraSelection();
                ((Dialog) v.getTag()).dismiss();


            }
        });
        TextView tvGallery = view.findViewById(R.id.tv_from_gallery);
        tvGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // send gallery selection..
                imageDialogListener.onGallerySelection();
                ((Dialog) v.getTag()).dismiss();

            }
        });
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(view)
                // Add cancel action buttons..
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        builder.setTitle(R.string.select_image);
        Dialog dialog = builder.create();
        tvCamera.setTag(dialog);
        tvGallery.setTag(dialog);
        return dialog;
    }
}
