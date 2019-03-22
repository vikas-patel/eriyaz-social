package com.eriyaz.social.adapters.holders;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.eriyaz.social.R;
import com.eriyaz.social.adapters.ProfileAdapter;

public class LeaderBoardHeaderViewHolder extends RecyclerView.ViewHolder {

    TextView rankHeader, authorNameHeader, pointsHeader, weeklyPointsHeader;
    public View view;
    private ProfileAdapter.Callback callback;
    private Context context;

    public LeaderBoardHeaderViewHolder(View itemView, final ProfileAdapter.Callback callback) {
        super(itemView);

        this.callback = callback;
        this.context = itemView.getContext();

        this.view=itemView;
        this.rankHeader=itemView.findViewById(R.id.rankHeader);
        this.authorNameHeader=itemView.findViewById(R.id.authorNameHeader);
        this.pointsHeader=itemView.findViewById(R.id.pointsHeader);
        this.weeklyPointsHeader=itemView.findViewById(R.id.weeklyPointsHeader);

        weeklyPointsHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Callback method to sort the list based on weekly rank
                callback.onHeaderClick("WeeklyRank");
            }
        });

        rankHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Callback method to sort the list based on rank
                callback.onHeaderClick("Rank");
            }
        });
    }
}
