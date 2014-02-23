package com.xproger.arcanum;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

public class BubbleEdit extends EditText {

    public BubbleEdit(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
    public void onSelectionChanged(int start, int end) {

        CharSequence text = getText();
        if (text != null) {
            if (start != text.length() || end != text.length()) {
                setSelection(text.length(), text.length());
                return;
            }
        }

        super.onSelectionChanged(start, end);
    }

}
