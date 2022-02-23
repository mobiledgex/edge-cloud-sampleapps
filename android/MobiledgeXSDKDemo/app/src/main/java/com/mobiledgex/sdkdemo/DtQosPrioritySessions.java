package com.mobiledgex.sdkdemo;

import static distributed_match_engine.AppClient.QosSessionProfile.QOS_NO_PRIORITY;
import static distributed_match_engine.AppClient.QosSessionProfile.UNRECOGNIZED;

import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.mobiledgex.matchingenginehelper.MatchingEngineHelper;

import java.util.ArrayList;
import java.util.List;

import distributed_match_engine.AppClient;

/**
 * QoS for stable latency/throughput API. See:
 * https://staging-portal.hubraum.opsfactory.dev/de/products/617bd0928431ba00019948f4/documentation
 * https://staging-portal.hubraum.opsfactory.dev/de/products/617dda988431ba00019948ff/documentation
 */
public class DtQosPrioritySessions {
    private static final String TAG = "DtQosPrioritySessions";
    private static MainActivity mActivity;
    private static MatchingEngineHelper meHelper;

    protected static void createPrioritySession(MainActivity activity, MatchingEngineHelper helper) {
        mActivity = activity;
        meHelper = helper;
        Log.i(TAG, "meHelper.mQosSessionId="+meHelper.mQosSessionId+" meHelper.mQosProfileName="+meHelper.mQosProfileName);
        // Create dialog with QOS profile names.
        List<String> items = new ArrayList<>();
        for (AppClient.QosSessionProfile profileName : AppClient.QosSessionProfile.values()) {
            if (!profileName.equals(QOS_NO_PRIORITY) && !profileName.name().equals(UNRECOGNIZED)) {
                items.add(profileName.name());
            }
        }
        Log.i(TAG, "items="+items);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("Update Session Priority");

        final ArrayAdapter<String> arrayAdapterItems = new ArrayAdapter<String>(
                mActivity.getApplicationContext(), android.R.layout.simple_list_item_single_choice, items);
        View content = mActivity.getLayoutInflater().inflate(R.layout.dt_qos_session_dialog, null);
        EditText textSesId = content.findViewById(R.id.text_ses_id);
        textSesId.setTextIsSelectable(true);
        EditText textProfile = content.findViewById(R.id.text_profile_name);
        textProfile.setTextIsSelectable(true);
        EditText textDuration = content.findViewById(R.id.text_duration);
        TextView tvMessage = content.findViewById(R.id.tv_msg);
        tvMessage.setText("Choose new QOS profile for session:");
        final ListView lvItems = content.findViewById(R.id.lv_items);
        lvItems.setAdapter(arrayAdapterItems);
        lvItems.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        builder.setView(content);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        content.findViewById(R.id.button_cancel).setOnClickListener(view -> {
            alertDialog.cancel();
        });

        Button buttonDelSession = content.findViewById(R.id.button_delete);
        buttonDelSession.setOnClickListener(view -> {
            meHelper.qosPrioritySessionDeleteInBackground();
            alertDialog.cancel();
        });

        Button buttonSesDetails = content.findViewById(R.id.button_details);
        buttonSesDetails.setOnClickListener(view -> {
            String msg = meHelper.getQosPrioritySessionDetails();
            mActivity.showMessage(msg);
            alertDialog.cancel();
        });

        if (meHelper.mQosSessionId != null && !meHelper.mQosSessionId.isEmpty()) {
            textSesId.setText(meHelper.mQosSessionId);
        } else {
            buttonDelSession.setEnabled(false);
            buttonSesDetails.setEnabled(false);
        }
        if (meHelper.mQosProfileName != null && !meHelper.mQosProfileName.isEmpty()) {
            textProfile.setText(meHelper.mQosProfileName);
        }

        lvItems.setOnItemClickListener((parent, view, which, id1) -> {
            String selectedItemText = items.get(which);
            String requestBody = "";
            int duration = Integer.parseInt(textDuration.getText().toString());
            meHelper.qosPrioritySessionCreateInBackground(selectedItemText, duration);
            alertDialog.dismiss();
        });
    }
}
