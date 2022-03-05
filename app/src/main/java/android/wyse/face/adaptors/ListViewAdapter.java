package android.wyse.face.adaptors;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.wyse.face.R;

import java.util.ArrayList;


public class ListViewAdapter extends BaseAdapter
{
	//private  Activity context;
	private ArrayList<UserIdNameModel> userIdNameModels;
	private LayoutInflater inflater;

	public ListViewAdapter(Activity context, ArrayList<UserIdNameModel> userIdNameModels) {
		super();
		//this.context = context;
		inflater =  context.getLayoutInflater();
		this.userIdNameModels=userIdNameModels;
	}

	public int getCount() {
		// TODO Auto-generated method stub
		return userIdNameModels.size();
	}

	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	private class ViewHolder {
		TextView txtViewTitle;
		TextView txtViewDescription;
	}

	public View getView(int position, View convertView, ViewGroup parent)
	{
		// TODO Auto-generated method stub
		ViewHolder holder;

		if (convertView == null)
		{
			convertView = inflater.inflate(R.layout.user_list, null);
			holder = new ViewHolder();
			holder.txtViewTitle = convertView.findViewById(R.id.user_id);
			holder.txtViewDescription = convertView.findViewById(R.id.user_name);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		if (userIdNameModels.size()>position && userIdNameModels.size()>0) {
			holder.txtViewTitle.setText(userIdNameModels.get(position).getUserId());
			holder.txtViewDescription.setText(userIdNameModels.get(position).getUserName());
		}

		return convertView;
	}



}
