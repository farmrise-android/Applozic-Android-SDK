package com.applozic.mobicomkit.uiwidgets;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

/**
 * Custom TextView Class
 */

public class CustomTextViewRegular extends android.support.v7.widget.AppCompatTextView {

    public CustomTextViewRegular(Context context) {
        super(context);
        setTypeface(context);
    }

    public CustomTextViewRegular(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setTypeface(context);
    }

    public CustomTextViewRegular(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setTypeface(context);
    }

    private void setTypeface(Context context) {
        Typeface face = Typeface.createFromAsset(context.getAssets(), "fonts/NotoSans-Regular.ttf");
        this.setTypeface(face);
    }


}
