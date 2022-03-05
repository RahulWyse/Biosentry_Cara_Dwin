package android.wyse.face.adaptors;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.wyse.face.models.SelectorTypes;

import java.util.ArrayList;


public class CustomArrayAdaptor extends ArrayAdapter<SelectorTypes> {


    private ArrayList<SelectorTypes> vendorCategories;
    public CustomArrayAdaptor(Context context, int textViewResourceId, ArrayList<SelectorTypes> vendorCategories) {
        super(context, textViewResourceId, vendorCategories);
      //  this.context = context;
        this.vendorCategories = vendorCategories;
    }

    @Override
    public int getCount(){
        if (vendorCategories!=null) {
            return vendorCategories.size();
        }
        return 0;
    }

    @Override
    public SelectorTypes getItem(int position){
        return vendorCategories.get(position);
    }

    @Override
    public long getItemId(int position){
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // I created a dynamic TextView here, but you can reference your own  custom layout for each spinner item
        TextView label = (TextView) super.getView(position, convertView, parent);
        label.setTextColor(Color.BLACK);
        label.setText(vendorCategories.get(position).getCatName());
        // And finally return your dynamic (or custom) view for each spinner item
        return label;
    }

    // And here is when the "chooser" is popped up
    // Normally is the same view, but you can customize it if you want
    @Override
    public View getDropDownView(int position, View convertView,  ViewGroup parent) {
        TextView label = (TextView) super.getDropDownView(position, convertView, parent);
        label.setTextColor(Color.BLACK);
        label.setText(vendorCategories.get(position).getCatName());
        return label;
    }

}