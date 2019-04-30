package com.eriyaz.social.adapters.holders;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.eriyaz.social.R;
import com.eriyaz.social.adapters.CommentsByUserAdapter;
import com.eriyaz.social.model.UserComment;
import com.eriyaz.social.utils.FormatterUtil;

public class UserCommentsViewHolder extends RecyclerView.ViewHolder {

    Context context;
    private CommentsByUserAdapter.CallBack callBack;
    private TextView commentTextView;
    private TextView postTitleTextView;
    private TextView dateTextView;
    private TextView likesCounterTextView;
    private TextView reputationCounterTextView;
    private ImageView audioPlayImageView;
    private ImageView likesImageView;
    private CommentsByUserAdapter.CallBack callback;

    public UserCommentsViewHolder(View itemView, CommentsByUserAdapter.CallBack callBack, OnClickListener onClickListener) {
        super(itemView);
        this.context = itemView.getContext();
        this.callBack = callBack;
        commentTextView = itemView.findViewById(R.id.user_comment_text);
        //postTitleTextView = itemView.findViewById(R.id.postTitleTextView);
        dateTextView = itemView.findViewById(R.id.dateTextView);
        likesCounterTextView = itemView.findViewById(R.id.likeCounterTextView);
        reputationCounterTextView = itemView.findViewById(R.id.reputationsCountersTextView);
        audioPlayImageView = itemView.findViewById(R.id.audioPlayimageView);
        likesImageView = itemView.findViewById(R.id.likesImageView);

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = getAdapterPosition();
                if (onClickListener != null && position != RecyclerView.NO_POSITION) {
                    onClickListener.onItemClick(getAdapterPosition(), v);
                }
            }
        });

        audioPlayImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = getAdapterPosition();
                if (onClickListener != null && position != RecyclerView.NO_POSITION) {
                    onClickListener.onPlayClick(v, getAdapterPosition(), "Your");
                }
            }
        });
    }

    public void bindData(UserComment comment)
    {
        // Set the comment text
        commentTextView.setText(comment.getText());

        // Set Created date
        CharSequence date = FormatterUtil.getRelativeTimeSpanString(context, comment.getCreatedDate());
        System.out.println("Created Date "+date);
        dateTextView.setText(date);

        // Set Likes counter
        if (comment.getLikesCount() > 0) {
            String heartLabel = context.getResources().getQuantityString(R.plurals.likes_counter_format, comment.getLikesCount(), comment.getLikesCount());
            String likeCounterText = comment.getLikesCount() + " " + heartLabel;
            likesImageView.setVisibility(View.VISIBLE);
            SpannableString underlineLikeCounterText = new SpannableString(likeCounterText);
            underlineLikeCounterText.setSpan(new UnderlineSpan(), 0, underlineLikeCounterText.length(), 0);
            likesCounterTextView.setText(underlineLikeCounterText);
        }
        else {
            likesImageView.setVisibility(View.GONE);
            likesCounterTextView.setText("");
        }

        // For audio comment, display play icon
        if (comment.getAudioPath() != null && !comment.getAudioPath().isEmpty()) {
            audioPlayImageView.setVisibility(View.VISIBLE);
        } else {
            audioPlayImageView.setVisibility(View.GONE);
        }

        // Set Reputation points counter
        if (comment.getReputationPoints() > 0) {
            reputationCounterTextView.setVisibility(View.VISIBLE);
            reputationCounterTextView.setText(Html.fromHtml(String.format(context.getString(R.string.comment_reward_points), comment.getReputationPoints())));
        } else {
            reputationCounterTextView.setVisibility(View.GONE);
        }
    }

    public interface OnClickListener {
        void onItemClick(int position, View view);
        void onPlayClick(View view, int position, String authorName);
    }
}
