package com.applozic.mobicomkit.uiwidgets;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;


/**
 * Util class for creating bold custom textView with app specific font
 */

public class CustomTextViewBold extends android.support.v7.widget.AppCompatTextView {

    public CustomTextViewBold(Context context, AttributeSet attrs) {
        super(context, attrs);
        setTypeface(context);
    }

    public CustomTextViewBold(Context context) {
        super(context);
        setTypeface(context);
    }


    public CustomTextViewBold(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setTypeface(context);
    }

    private void setTypeface(Context context) {
        Typeface face = Typeface.createFromAsset(context.getAssets(), "fonts/NotoSans-Bold.ttf");
        this.setTypeface(face);
    }


}
