package co.kwest.www.callmanager.adapter;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import co.kwest.www.callmanager.ui.fragment.CatiFragment;
import co.kwest.www.callmanager.ui.fragment.ContactsFragment;
import co.kwest.www.callmanager.ui.fragment.RecentsFragment;

public class CustomPagerAdapter extends FragmentPagerAdapter {

    // -- Constants -- //
    private final static int NUM_ITEMS = 3;
    // Fragment titles
    private final static String FIRST_POSITION_TITLE = "Recent";
    private final static String SECOND_POSITION_TITLE = "CATI";
    private final static String THIRD_POSITION_TITLE = "Contacts";

    /**
     * Constructor
     *
     * @param fragmentManager
     */
    public CustomPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
    }

    /**
     * Returns the amount of pages
     *
     * @return
     */
    @Override
    public int getCount() {
        return NUM_ITEMS;
    }

    /**
     * Returns an item by its position
     *
     * @param position
     * @return Fragment
     */
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new RecentsFragment();
            case 1:
                return new CatiFragment();
            case 2:
                return new ContactsFragment();
            default:
                return null;
        }
    }

    /**
     * Returns the pages title by his position
     *
     * @param position
     * @return String
     */
    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return FIRST_POSITION_TITLE;
            case 1:
                return SECOND_POSITION_TITLE;
            case 2:
                return THIRD_POSITION_TITLE;
            default:
                return null;
        }
    }
}
