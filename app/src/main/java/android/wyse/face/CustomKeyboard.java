package android.wyse.face;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;

import java.util.ArrayList;

interface KeyboardCallbacks{
         void onKeyPress(String s, String type);
}

class CustomKeyboard {

    private Context context;
    LayoutInflater inflater;
    private Button btn1;
    private Button btn2;
    private Button btn3;
    private Button btn4;
    private Button btn5;
    private Button btn6;
    private Button btn7;
    private Button btn8;
    private Button btn9;
    private Button btn10;
    private Button btn11;
    private Button btn12;

    private ArrayList charSeq;
    private int btncolor=Color.BLACK;
    private int bgcolor=Color.LTGRAY;
    private View keyboardbg;

    public CustomKeyboard(final Typeface typeface,final Context context, final View keyboardview, final KeyboardCallbacks callbacks){
        this.context=context;

        charSeq=new ArrayList();
        keyboardbg=keyboardview.findViewById(R.id.keyboardbg);

        View.OnClickListener btnlistner= v -> {
            setFadeAnimation(v);
            Button view;
            switch (v.getId()){
                case R.id.deletebtn:
                    callbacks.onKeyPress(v.getId()+"","del");
                    break;
                case R.id.okbtn:
                    view=(Button)v;
                    callbacks.onKeyPress(v.getId()+"","done");
                    break;
                    default:
                        view=(Button)v;
                        charSeq.add(view.getText().toString());
                        callbacks.onKeyPress(view.getText().toString(),"key");
                        break;
            }
        };

        btn1=(Button) keyboardview.findViewById(R.id.btn1);
        btn2=(Button) keyboardview.findViewById(R.id.btn2);
        btn3=(Button) keyboardview.findViewById(R.id.btn3);
        btn4=(Button) keyboardview.findViewById(R.id.btn4);
        btn5=(Button) keyboardview.findViewById(R.id.btn5);
        btn6=(Button) keyboardview.findViewById(R.id.btn6);
        btn7=(Button) keyboardview.findViewById(R.id.btn7);
        btn8=(Button) keyboardview.findViewById(R.id.btn8);
        btn9=(Button) keyboardview.findViewById(R.id.btn9);
        btn10=(Button) keyboardview.findViewById(R.id.btn10);
        btn11=(Button) keyboardview.findViewById(R.id.deletebtn);
        btn12=(Button) keyboardview.findViewById(R.id.okbtn);
        btn1.setOnClickListener(btnlistner);
        btn2.setOnClickListener(btnlistner);
        btn3.setOnClickListener(btnlistner);
        btn4.setOnClickListener(btnlistner);
        btn5.setOnClickListener(btnlistner);
        btn6.setOnClickListener(btnlistner);
        btn7.setOnClickListener(btnlistner);
        btn8.setOnClickListener(btnlistner);
        btn9.setOnClickListener(btnlistner);
        btn10.setOnClickListener(btnlistner);
        btn11.setOnClickListener(btnlistner);
        btn12.setOnClickListener(btnlistner);

        if (typeface!=null) {
            btn1.setTypeface(typeface);
            btn2.setTypeface(typeface);
            btn3.setTypeface(typeface);
            btn4.setTypeface(typeface);
            btn5.setTypeface(typeface);
            btn6.setTypeface(typeface);
            btn7.setTypeface(typeface);
            btn8.setTypeface(typeface);
            btn9.setTypeface(typeface);
            btn10.setTypeface(typeface);
        }


    }

    private void setFadeAnimation(View view) {
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator()); //add this
        fadeIn.setDuration(500);

        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator()); //and this
        fadeOut.setStartOffset(500);
        fadeOut.setDuration(500);

        view.startAnimation(fadeOut);
        view.startAnimation(fadeIn);
    }

    public void setButtonColors(int color){
        btn1.setTextColor(color);
        btn2.setTextColor(color);
        btn3.setTextColor(color);
        btn4.setTextColor(color);
        btn5.setTextColor(color);
        btn6.setTextColor(color);
        btn7.setTextColor(color);
        btn8.setTextColor(color);
        btn9.setTextColor(color);
        btn10.setTextColor(color);
    }
    public void setBgColor(int color){
        if (keyboardbg!=null) {
            keyboardbg.setBackgroundColor(color);
        }
    }

}
