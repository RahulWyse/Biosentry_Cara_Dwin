package android.wyse.face.adaptors;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.wyse.face.CaraManager;
import android.wyse.face.R;
import android.wyse.face.Utility;
import android.wyse.face.models.UsersModel;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;


public class UserViewAdaptor extends RecyclerView.Adapter<UserViewAdaptor.ViewHolder> {

    private ArrayList<UsersModel> usersModels;
    private Context context;
    private BtnCallBack btnCallBack;

    public UserViewAdaptor(Context context, ArrayList<UsersModel> usersModels, BtnCallBack btnCallBack) {
        this.usersModels = usersModels;
        this.context = context;
        this.btnCallBack = btnCallBack;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.user_list, parent, false);
        ViewHolder viewHolder = new ViewHolder(listItem);
        return viewHolder;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return usersModels.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }


    @Override
    public void onBindViewHolder(@NonNull final UserViewAdaptor.ViewHolder holder, final int position) {

        if (usersModels.size() > 0) {
            try {
                final UsersModel usersModel = usersModels.get(position);
                holder.userId.setText(usersModel.getUserid());
                holder.userName.setText(usersModel.getName());


                holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        CaraManager.getInstance().setFadeAnimation(v);

                        showMessageDia(holder.itemView.getContext(), usersModel.getUserid(),
                                "PERFOM ACTION",
                                "Please select action to perform on selected user " + usersModel.getUserid(),
                                "error",position);

                        return false;
                    }
                });
            } catch (Exception er) {
            }
        }
    }


    private void showMessageDia(Context context, String userId, String message, String subtext, final String type,int pos) {

        try {
            final Dialog alertDialog = new Dialog(context);
            alertDialog.setCancelable(false);
            alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            alertDialog.setContentView(R.layout.custom_dialog_layout);
            TextView tv = alertDialog.findViewById(R.id.alerttext);
            TextView background = alertDialog.findViewById(R.id.textView22);
            TextView yourmessage = alertDialog.findViewById(R.id.yourmessage);
            Button allowBtn = alertDialog.findViewById(R.id.allowBtn);
            Button denyBtn = alertDialog.findViewById(R.id.denyBtn);
            if (type.equals("error")) {
                background.setBackgroundColor(context.getResources().getColor(R.color.red));
            } else {
                background.setBackgroundColor(context.getResources().getColor(R.color.green));
            }

            tv.setText(message);
            yourmessage.setText(subtext);

            denyBtn.setText("CANCEL");

            allowBtn.setText("REMOVE");
            allowBtn.setTextColor(context.getResources().getColor(R.color.red));


            allowBtn.setOnClickListener(v -> {
                btnCallBack.onClick(userId,pos);
                usersModels.remove(pos);
                notifyDataSetChanged();
                alertDialog.dismiss();
            });
            denyBtn.setOnClickListener(v -> alertDialog.dismiss());
            alertDialog.show();
        } catch (Exception er) {
            Utility.printStack(er);
        }

    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView userId;
        TextView userName;

        //  public RelativeLayout relativeLayout;
        public ViewHolder(View rowView) {
            super(rowView);
            userId = rowView.findViewById(R.id.user_id);
            userName = rowView.findViewById(R.id.user_name);
        }

    }

}
