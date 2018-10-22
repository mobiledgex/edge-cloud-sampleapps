package com.mobiledgex.sdkdemo.camera;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.mobiledgex.sdkdemo.MainActivity;
import com.mobiledgex.sdkdemo.R;


/**
 * Shows dialog for entering guest name, and starting Face Recognition training.
 */
public class TrainGuestDialog extends DialogFragment {
    private TrainGuestDialogListener mListener;

    public interface TrainGuestDialogListener {
        void onSetGuestName(String guestName);
        void onCancelTrainGuestDialog();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            mListener = (TrainGuestDialogListener) getTargetFragment();
            Log.i("BDA5", "1.mListener="+mListener);
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
                .setPositiveButton(R.string.train_guest_start, null)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onCancelTrainGuestDialog();
                        TrainGuestDialog.this.getDialog().cancel();
                    }
                })
                .setCancelable(false)
                .setTitle(R.string.train_guest_title)
                .setMessage(R.string.train_guest_message)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        Log.i("BDA5", "onDismiss()");
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
                Log.i("BDA5", "guestName="+guestName);
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
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }
                mListener.onSetGuestName(guestName);
                TrainGuestDialog.this.getDialog().cancel();
            }
        });
    }
}
