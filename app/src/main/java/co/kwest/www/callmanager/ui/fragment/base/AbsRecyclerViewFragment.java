package co.kwest.www.callmanager.ui.fragment.base;

import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import co.kwest.www.callmanager.R;
import co.kwest.www.callmanager.ui.activity.MainActivity;

public abstract class AbsRecyclerViewFragment extends AbsBaseFragment {

    public @BindView(R.id.recycler_view) RecyclerView mRecyclerView;

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            activity.syncFABAndFragment();
        }
    }
}
