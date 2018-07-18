package com.eriyaz.social.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import com.eriyaz.social.fragments.PostsByUserFragment;

/**
 * Created by vikas on 3/3/18.
 */

public class ProfileTabAdapter extends FragmentPagerAdapter {
    private PostsByUserFragment postsByUserFragment;
    private PostsByUserFragment ratedPostsByUserFragment;
    private String title[] = {"Recordings", "Ratings On Others"};
    private String userId;

    public ProfileTabAdapter(FragmentManager manager, String userId) {
        super(manager);
        this.userId = userId;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return PostsByUserFragment.newInstance(userId, PostsByUserFragment.POST_TYPE);
            case 1:
                return PostsByUserFragment.newInstance(userId, PostsByUserFragment.RATED_POST_TYPE);
            default:
                return null;
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment createdFragment = (Fragment) super.instantiateItem(container, position);
        // save the appropriate reference depending on position
        switch (position) {
            case 0:
                postsByUserFragment = (PostsByUserFragment) createdFragment;
                break;
            case 1:
                ratedPostsByUserFragment = (PostsByUserFragment) createdFragment;
                break;
        }
        return createdFragment;
    }

    @Override
    public int getCount() {
        return title.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return title[position];
    }

    public PostsByUserFragment getSelectedFragment(int position) {
        switch (position) {
            case 0:
                return postsByUserFragment;
            case 1:
                return ratedPostsByUserFragment;
        }
        return null;
    }
}
