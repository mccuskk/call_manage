package co.kwest.www.callmanager.ui.fragment;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import co.kwest.www.callmanager.R;
import co.kwest.www.callmanager.adapter.CatiAdapter;
import co.kwest.www.callmanager.adapter.listener.OnItemClickListener;
import co.kwest.www.callmanager.ui.FABCoordinator;
import co.kwest.www.callmanager.ui.activity.MainActivity;
import co.kwest.www.callmanager.ui.fragment.base.AbsRecyclerViewFragment;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CatiFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CatiFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CatiFragment extends AbsRecyclerViewFragment implements
    FABCoordinator.OnFABClickListener,
    FABCoordinator.FABDrawableCoordination,
    OnItemClickListener {

  private LinearLayoutManager mLayoutManager;
  private CatiAdapter mAdapter;

  @BindView(R.id.serial_no)
  TextView mSerialNo;

  public CatiFragment() {
    // Required empty public constructor
  }

  @Override
  protected void onFragmentReady() {
    mLayoutManager = new LinearLayoutManager(getContext()) {
      @Override
      public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        super.onLayoutChildren(recycler, state);

      }
    };
    mRecyclerView.setLayoutManager(mLayoutManager);
    mAdapter = new CatiAdapter(getContext(), this);
    mRecyclerView.setAdapter(mAdapter);

    mSerialNo.setText(Build.getSerial());
  }

  @Override
  public void onItemClick(RecyclerView.ViewHolder holder, Object data) {
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_cati, container, false);
  }

  @Override
  public void onRightClick() {
    ((MainActivity) getActivity()).expandDialer(true);
  }

  @Override
  public void onLeftClick() {

  }

  @Override
  public int[] getIconsResources() {
    return new int[]{
        R.drawable.ic_dialpad_black_24dp,
        -1
    };
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  /**
   * This interface must be implemented by activities that contain this
   * fragment to allow an interaction in this fragment to be communicated
   * to the activity and potentially other fragments contained in that
   * activity.
   * <p>
   * See the Android Training lesson <a href=
   * "http://developer.android.com/training/basics/fragments/communicating.html"
   * >Communicating with Other Fragments</a> for more information.
   */
  public interface OnFragmentInteractionListener {
    // TODO: Update argument type and name
    void onFragmentInteraction(Uri uri);
  }

}
