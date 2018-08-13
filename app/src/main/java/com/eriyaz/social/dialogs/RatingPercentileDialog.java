/*
 *
 * Copyright 2017 Rozdoum
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.eriyaz.social.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eriyaz.social.R;
import com.eriyaz.social.activities.BaseAlertDialogBuilder;
import com.eriyaz.social.activities.ProfileActivity;
import com.eriyaz.social.activities.RatingsChartActivity;
import com.eriyaz.social.utils.RatingUtil;

import org.w3c.dom.Text;

/**
 * Created by alexey on 12.05.17.
 */

public class RatingPercentileDialog extends DialogFragment {
    public static final String TAG = RatingPercentileDialog.class.getSimpleName();
    public static final String NORMALIZED_RATING_KEY = "RatingPercentiileDialog.NORMALIZED_RATING_KEY";
    public static final String ACTUAL_RATING_KEY = "RatingPercentiileDialog.ACTUAL_RATING_KEY";
    public static final String RATER_NAME_KEY = "RatingPercentiileDialog.RATER_NAME_KEY";
    public static final String RATER_ID_KEY = "RatingPercentiileDialog.RATER_ID_KEY";

    private int normalizedRating;
    private int actualRating;
    private int averageRating;
    private String raterName;
    private String raterId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        normalizedRating = getArguments().getInt(NORMALIZED_RATING_KEY);
        actualRating = getArguments().getInt(ACTUAL_RATING_KEY);
        raterId = getArguments().getString(RATER_ID_KEY);
        raterName = getArguments().getString(RATER_NAME_KEY);
        if (raterName == null || raterName.isEmpty()) {
            raterName = "his/her";
        }
        averageRating = Math.round(actualRating*10/normalizedRating);
        super.onCreate(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.dialog_rating_percentile, null);

        TextView normalizedRatingTextView = view.findViewById(R.id.normalizedRatingTextView);
        normalizedRatingTextView.setText(Html.fromHtml(String.format(getString(R.string.normalized_rating_text), normalizedRating)));

        TextView actualRatingTextView = view.findViewById(R.id.actualRatingTextView);
        actualRatingTextView.setText(Html.fromHtml(String.format(getString(R.string.actual_rating_text), actualRating)));

        final TextView averageRatingTextView = view.findViewById(R.id.avgRatingTextView);
        averageRatingTextView.setText(Html.fromHtml(String.format(getString(R.string.average_rating_text), raterName, averageRating)));

//        final TextView averageRatingValueTextView = view.findViewById(R.id.avgRatingValueTextView);
//        averageRatingValueTextView.setText(Html.fromHtml(String.format(getString(R.string.average_rating_link), averageRating)));

        averageRatingTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // open profile activity
                Intent intent = new Intent(getActivity(), ProfileActivity.class);
                intent.putExtra(ProfileActivity.USER_ID_EXTRA_KEY, raterId);
                intent.putExtra(ProfileActivity.RATING_TAB_DEFAULT_EXTRA_KEY, true);
                startActivity(intent);
            }
        });

        final TextView avgAllRecordingsTextView = view.findViewById(R.id.avgAllRecordingsTextView);

        final TextView normalizedRatingCalculationTextView = view.findViewById(R.id.normalizedRatingCalculationTextView);
        normalizedRatingCalculationTextView.setText(Html.fromHtml(String.format(getString(R.string.normalized_rating_calculation_text), actualRating, averageRating, normalizedRating)));

        String percentile = RatingUtil.getRatingPercentile(normalizedRating);
        TextView percentileTextView = view.findViewById(R.id.percentileTextView);
        percentileTextView.setText(Html.fromHtml(String.format(getString(R.string.percentile_text), percentile)));

        TextView ratingChartTextView = view.findViewById(R.id.ratingChartLinkTextView);
        ratingChartTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // open rating chart
                Intent intent = new Intent(getActivity(), RatingsChartActivity.class);
                startActivity(intent);
            }
        });

        final LinearLayout calculationLayout = view.findViewById(R.id.calculationLayout);

        final TextView calculationLinkTextView = view.findViewById(R.id.calculationLinkTextView);
        if (actualRating == normalizedRating) {
            averageRatingTextView.setVisibility(View.GONE);
            normalizedRatingCalculationTextView.setVisibility(View.GONE);
            avgAllRecordingsTextView.setVisibility(View.GONE);
        }
        calculationLinkTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calculationLayout.setVisibility(View.VISIBLE);
                calculationLinkTextView.setVisibility(View.GONE);
                // don't show calculation
            }
        });

        AlertDialog.Builder builder = new BaseAlertDialogBuilder(getActivity());
        builder.setView(view)
//                .setTitle("Percentile Calculation")
//                .setNegativeButton(R.string.button_title_cancel, null)
                .setPositiveButton("Close", null);

        return builder.create();
    }
}
