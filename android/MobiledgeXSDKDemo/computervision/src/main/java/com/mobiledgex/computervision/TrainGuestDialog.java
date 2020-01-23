/**
 * Copyright 2019 MobiledgeX, Inc. All rights and licenses reserved.
 * MobiledgeX, Inc. 156 2nd Street #408, San Francisco, CA 94105
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobiledgex.computervision;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;


/**
 * Shows dialog for entering guest name, and starting Face Recognition training. Dialog can also
 * be used for entering a guest name whose data should be deleted from the training server.
 */
public class TrainGuestDialog extends DialogFragment {
    private static final String TAG = "TrainGuestDialog";
    public static final int RC_START_TRAINING = 1;
    public static final int RC_REMOVE_DATA = 2;
    private TrainGuestDialogListener mListener;
    private int mMessage;
    private int mRequestCode;

    public void setRequestCode(int rc) {
        mRequestCode = rc;
        if(mRequestCode == RC_START_TRAINING) {
            mMessage = R.string.train_guest_message;
        } else if(mRequestCode == RC_REMOVE_DATA) {
            mMessage = R.string.delete_guest_message;
        } else {

        }
    }

    public interface TrainGuestDialogListener {
        void onSetGuestName(String guestName, int requestCode);
        void onCancelTrainGuestDialog();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            mListener = (TrainGuestDialogListener) getTargetFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException("Calling Fragment must implement TrainGuestDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_train_guest, null))
                // Add action buttons
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onCancelTrainGuestDialog();
                        TrainGuestDialog.this.getDialog().cancel();
                    }
                })
                .setCancelable(false)
                .setTitle(R.string.train_guest_title)
                .setMessage(mMessage)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        Log.i(TAG, "onDismiss()");
                    }
                });
        return builder.create();
    }
    @Override
    public void onResume() {
        super.onResume();

        // Override the click listener that dismisses the dialog by default.
        AlertDialog dialog = (AlertDialog) getDialog();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Is this a valid name that can be used as a directory name on the server?
                EditText editText = getDialog().findViewById(R.id.editTextGuestName);
                String guestName = editText.getText().toString();
                Log.i(TAG, "guestName="+guestName);
                String errorMessage = null;
                String[] reservedChars = {"|", "\\", "?", "*", "<", "\"", ":", ">"};
                if(guestName.isEmpty()) {
                    errorMessage = "Please enter a name.";
                }
                for(String c :reservedChars) {
                    if(guestName.indexOf(c) >= 0) {
                        errorMessage = "Illegal character \""+c+"\". Please try again.";
                    }
                }
                if(errorMessage != null) {
                    new android.support.v7.app.AlertDialog.Builder(getContext())
                            .setTitle("Error")
                            .setMessage(errorMessage)
                            .setPositiveButton(R.string.ok, null)
                            .show();
                    return;
                }
                mListener.onSetGuestName(guestName, mRequestCode);
                TrainGuestDialog.this.getDialog().cancel();
            }
        });
    }
}
