package com.mobiledgex.matchingenginehelper;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.DialogInterface;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.mobiledgex.matchingenginehelper.EventItem.EventType.ERROR;
import static com.mobiledgex.matchingenginehelper.EventItem.EventType.INFO;

public class EventLogViewer implements PopupMenu.OnMenuItemClickListener {
    private static final String TAG = "EventLogViewer";
    private Activity mActivity;
    public List<EventItem> mEventItemList = new ArrayList<>();
    private RecyclerView mEventsRecyclerView;
    private EventRecyclerViewAdapter mEventRecyclerViewAdapter;
    private FloatingActionButton mLogExpansionButton;
    private boolean isLogExpanded = false;
    private int mLogViewHeight;
    public boolean mAutoExpand = true;

    public EventLogViewer(Activity activity, FloatingActionButton button, RecyclerView recyclerView) {
        mActivity = activity;
        mEventsRecyclerView = recyclerView;
        mLogExpansionButton = button;
        mLogViewHeight = (int) (mActivity.getResources().getDisplayMetrics().heightPixels*.4);
        setupLogViewer();
    }

    protected void setupLogViewer() {
        final LinearLayoutManager layout = new LinearLayoutManager(mEventsRecyclerView.getContext());
        mEventsRecyclerView.setLayoutManager(layout);
        mEventRecyclerViewAdapter = new EventRecyclerViewAdapter(mEventItemList);
        mEventRecyclerViewAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                layout.smoothScrollToPosition(mEventsRecyclerView, null, mEventRecyclerViewAdapter.getItemCount());
            }

        });
        mEventsRecyclerView.setAdapter(mEventRecyclerViewAdapter);
        mEventsRecyclerView.getLayoutParams().height = 0;
        mEventsRecyclerView.requestLayout();

        mLogExpansionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "mEventsRecyclerView.getLayoutParams().height="+mEventsRecyclerView.getLayoutParams().height);
                Log.i(TAG, "mLogExpansionButton.getHeight()="+mLogExpansionButton.getHeight());
                Log.i(TAG, "isLogExpanded="+isLogExpanded);
                layout.smoothScrollToPosition(mEventsRecyclerView, null, mEventRecyclerViewAdapter.getItemCount());
                if(!isLogExpanded){
                    logViewAnimate(0, mLogViewHeight);
                    isLogExpanded = true;
                }
                else{
                    logViewAnimate(mLogViewHeight, 0);
                    isLogExpanded = false;
                }
            }
        });
        mLogExpansionButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                PopupMenu popup = new PopupMenu(mActivity, v);
                popup.setOnMenuItemClickListener(EventLogViewer.this);
                popup.inflate(R.menu.event_viewer_popup);
                popup.show();
                popup.getMenu().findItem(R.id.action_elv_auto_expand).setChecked(mAutoExpand);
                return false;
            }
        });
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_elv_clear) {
            clearAllLogs();
        } else if (item.getItemId() == R.id.action_elv_copy) {
            mEventRecyclerViewAdapter.copyAllItemsAsText(mEventsRecyclerView);
        } else if (item.getItemId() == R.id.action_elv_auto_expand) {
            mAutoExpand = !mAutoExpand;
            item.setChecked(mAutoExpand);
        }
        return true;
    }

    private void clearAllLogs() {
        new androidx.appcompat.app.AlertDialog.Builder(mActivity)
                .setTitle(R.string.verify_clear_logs_title)
                .setMessage(R.string.verify_clear_logs_message)
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mEventItemList.clear();
                        mEventRecyclerViewAdapter.notifyDataSetChanged();
                        Toast.makeText(mActivity, "Log viewer cleared", Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    /**
     * Calling this method indicates the initial logs that the activity always shows are complete.
     * Start a timer to hide the logs. Note that the logviewer will automatically be expanded if
     * additional logs are displayed.
     */
    public void initialLogsComplete() {
        Log.i(TAG, "initialLogsComplete()");
        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (isLogExpanded) {
                    logViewAnimate(mLogViewHeight, 0);
                    isLogExpanded = false;
                }
            }
        }, 3000);
    }

    protected void logViewAnimate(final int start, final int end) {
        Log.i(TAG, "logViewAnimate start="+start+" end="+end);
        if (mActivity == null) {
            Log.e(TAG, "logViewAnimate called after Activity has gone away.");
            return;
        }
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ValueAnimator va = ValueAnimator.ofInt(start, end);
                va.setDuration(500);
                va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator animation) {
                        if (animation == null) {
                            Log.e(TAG, "onAnimationUpdate called with null animation.");
                            return;
                        }
                        Integer value = (Integer) animation.getAnimatedValue();
                        mEventsRecyclerView.getLayoutParams().height = value.intValue();
                        mEventsRecyclerView.requestLayout();
                    }
                });
                va.start();
            }
        });
    }

    public void addEventItem(EventItem.EventType type, String text) {
        if (!isLogExpanded) {
            if (mAutoExpand) {
                logViewAnimate(0, mLogViewHeight);
                isLogExpanded = true;
            }
        }

        mEventItemList.add(new EventItem(type, text));
        if (mActivity != null) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mEventRecyclerViewAdapter.itemAdded();
                }
            });
        }
    }

    public void showMessage(String text) {
        addEventItem(INFO, text);
    }

    public void showError(String text) {
        addEventItem(ERROR, text);
    }
}
