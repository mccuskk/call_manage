package co.kwest.www.callmanager.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.kwest.www.callmanager.R;
import co.kwest.www.callmanager.adapter.listener.OnItemClickListener;
import co.kwest.www.callmanager.database.AppDatabase;
import co.kwest.www.callmanager.database.DataRepository;
import co.kwest.www.callmanager.database.entity.CGroupAndItsContacts;
import co.kwest.www.callmanager.database.entity.Contact;
import co.kwest.www.callmanager.util.Utilities;

public class CatiAdapter extends RecyclerView.Adapter<CatiAdapter.CatiHolder> {

  private Context mContext;
  private OnItemClickListener mOnItemClickListener;


  public CatiAdapter(Context context,
                     OnItemClickListener onItemClickListener) {

    mContext = context;
    mOnItemClickListener = onItemClickListener;

  }


  @NonNull
  @Override
  public CatiAdapter.CatiHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(mContext).inflate(R.layout.item_contact, parent, false);
    CatiAdapter.CatiHolder holder = new CatiAdapter.CatiHolder(view);
    return holder;
  }

  @Override
  public void onBindViewHolder(@NonNull CatiAdapter.CatiHolder holder, int position) {

  }

  public int getItemCount() {
    return 0;
  }


  class CatiHolder extends RecyclerView.ViewHolder {

    public CatiHolder(@NonNull View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }

}
