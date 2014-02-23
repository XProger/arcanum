package com.xproger.arcanum;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnDismissListener;
import android.view.View;

public class PopupMenuAB extends PopupMenu {

    public PopupMenuAB(Context context, View anchor, boolean showIcons) {
        super(context, anchor);       
        if (showIcons) {
			try {
			    Field[] fields = PopupMenu.class.getDeclaredFields();
			    for (Field field : fields) {
			        if ("mPopup".equals(field.getName())) {
			            field.setAccessible(true);
			            Object menuPopupHelper = field.get(this);
			            Class<?> classPopupHelper = Class.forName(menuPopupHelper
			                    .getClass().getName());
			            Method setForceIcons = classPopupHelper.getMethod(
			                    "setForceShowIcon", boolean.class);
			            setForceIcons.invoke(menuPopupHelper, true);
			            break;
			        }
			    }
			} catch (Exception e) {}        
        }
    }
}
