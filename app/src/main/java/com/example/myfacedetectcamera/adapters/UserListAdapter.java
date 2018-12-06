package com.example.myfacedetectcamera.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.myfacedetectcamera.R;
import com.example.myfacedetectcamera.model.UserModel;

import java.util.List;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.MyViewHolder> {

    private List<UserModel> mData;

    public UserListAdapter(List<UserModel> mData) {
        this.mData = mData;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View inflate = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_list_user, viewGroup, false);
        return new MyViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, final int i) {
        final UserModel userModel = mData.get(i);
        myViewHolder.tv_user_name.setText(userModel.getUserName());
        myViewHolder.btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDeleteListener != null){
                    mDeleteListener.delete(userModel, i);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        TextView tv_user_name;
        Button btn_delete;

        MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_user_name = itemView.findViewById(R.id.tv_user_name);
            btn_delete = itemView.findViewById(R.id.btn_delete);
        }
    }

    public void removeData(int position) {
        mData.remove(position);
        notifyItemRemoved(position);
    }

    private OnDeleteClickListener mDeleteListener;
    public void setOnDeleteClickListener(OnDeleteClickListener listener){
        this.mDeleteListener = listener;
    }

    public interface OnDeleteClickListener{
        void delete(UserModel model, int pos);
    }
}
