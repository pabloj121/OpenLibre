package de.dorianscholz.openlibre.ui;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import de.dorianscholz.openlibre.R;

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter extends SmartFragmentStatePagerAdapter {

    SectionsPagerAdapter(FragmentManager fragmentManager, Context context) {
        super(fragmentManager, context);
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        if (position == mContext.getResources().getInteger(R.integer.viewpager_page_show_scan))
                return DataPlotFragment.newInstance();
        else if (position == mContext.getResources().getInteger(R.integer.viewpager_page_fragment_log))
                return LogFragment.newInstance();
        else if (position == mContext.getResources().getInteger(R.integer.viewpager_page_fragment_agenda))
            return AgendaFragment.newInstance();
        return null;
    }

    @Override
    public int getCount() {
        // Show 3 total pages. (Now with the agenda fragment !)
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position == mContext.getResources().getInteger(R.integer.viewpager_page_show_scan))
            return mContext.getResources().getString(R.string.fragment_title_scan);
        else if (position == mContext.getResources().getInteger(R.integer.viewpager_page_fragment_log))
            return mContext.getResources().getString(R.string.fragment_title_log);
        else if (position == mContext.getResources().getInteger(R.integer.viewpager_page_fragment_agenda))
            return mContext.getResources().getString(R.string.fragment_title_agenda);
        return null;
    }
}
