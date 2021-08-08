package com.mobiledgex.matchingenginehelper;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link EventItem}.
 */
public class EventRecyclerViewAdapter extends RecyclerView.Adapter<EventRecyclerViewAdapter.ViewHolder> {

    private final List<EventItem> mValues;

    public EventRecyclerViewAdapter(List<EventItem> items) {
        mValues = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mTimestampView.setText(mValues.get(position).timestampText);
        holder.mContentView.setText(mValues.get(position).content);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
        if(position%2 == 0){
            holder.mView.setBackgroundColor(Color.parseColor("#FFFFFF"));
        } else {
            holder.mView.setBackgroundColor(Color.parseColor("#EEEEEE"));
        }
    }

    public void itemAdded() {
        int id = mValues.size();
        this.notifyItemInserted(id);
    }

    public void copyAllItemsAsText(View view) {
        // Gets a handle to the clipboard service.
        ClipboardManager clipboard = (ClipboardManager)
                view.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        // Creates a new text clip to put on the clipboard
        ClipData clip = ClipData.newPlainText("simple text", getAllItemsAsText());
        // Set the clipboard's primary clip.
        clipboard.setPrimaryClip(clip);
        Toast.makeText(view.getContext(), "Items copied to clipboard.", Toast.LENGTH_SHORT).show();
    }

    public String getAllItemsAsText() {
        StringBuilder sb = new StringBuilder();
        for (EventItem eventItem : mValues) {
            sb.append(eventItem.timestampText + " " + eventItem.content + "\n");
        }
        return sb.toString();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mTimestampView;
        public final TextView mContentView;
        public EventItem mItem;
        public ImageView mIconView;

        public ViewHolder(final View view) {
            super(view);
            mView = view;
            mTimestampView = view.findViewById(R.id.timestamp);
            mContentView = view.findViewById(R.id.content);
            mIconView = view.findViewById(R.id.imageView);

            //Long Press
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    copyAllItemsAsText(view);
                    return true;
                }
            });
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}