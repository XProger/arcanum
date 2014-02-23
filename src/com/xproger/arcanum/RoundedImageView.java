package com.xproger.arcanum;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.FillType;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.widget.ImageView;

public class RoundedImageView extends ImageView {
    private Path path;
    private int radius;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    
	public RoundedImageView(Context context) {
		super(context);
		init();
	}
	
	public RoundedImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public RoundedImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
    private void init() {
    //	setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    	ViewCompat.setLayerType(this, ViewCompat.LAYER_TYPE_SOFTWARE, null);
    	paint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
    	setRadius(5);
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w != oldw || h != oldh) {
            path = new Path();
            path.addRoundRect(new RectF(0, 0, w, h), radius, radius, Path.Direction.CW);
            path.setFillType(FillType.INVERSE_WINDING);
        }
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
        path = new Path();
        path.addRoundRect(new RectF(0, 0, getWidth(), getHeight()), radius, radius, Path.Direction.CW);
    }    
	
	@Override
	protected void onDraw(Canvas canvas) {
		if (canvas.isOpaque())
			canvas.saveLayerAlpha(0, 0, canvas.getWidth(), canvas.getHeight(), 255, Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);
		
		//canvas.clipPath(path);
		super.onDraw(canvas);	
		
		canvas.drawPath(path, paint);
	}
}