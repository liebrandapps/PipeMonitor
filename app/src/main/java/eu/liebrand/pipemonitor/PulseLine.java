package eu.liebrand.pipemonitor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by mark on 20.02.17.
 */

public class PulseLine extends View {

    private Paint paintNormal;
    private Paint paintPulse;
    private int lineWidth;
    private Rect rect;
    long lastTime;
    long lastBox;


    public PulseLine(Context context) {
        super(context);
        init(context);
    }

    public PulseLine(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PulseLine(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        paintNormal=new Paint();
        paintNormal.setARGB(255, 202, 202, 202);
        paintPulse=new Paint();
        paintPulse.setARGB(255, 48, 48, 48);
        rect=new Rect();
        lastBox=-1;
        lineWidth=15;
    }

    public void setLineWidh(int width) {
        lineWidth=width;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);
        super.onDraw(canvas);
        long now = android.os.SystemClock.uptimeMillis();
        if(lastBox==-1 || now - lastTime>900) {
            lastBox++;
            lastTime=now;
        }
        int bottom = (getHeight() - lineWidth) / 2;
        int idx=lineWidth / 2;
        int width=getWidth();
        int box=0;
        while (idx<width) {
            rect.set(idx, bottom+lineWidth, idx+lineWidth,bottom);
            canvas.drawRect(rect, box==lastBox? paintPulse : paintNormal);
            idx+=2*lineWidth;
            box++;
        }
        if(box==lastBox) {
            lastBox=-1;
        }
        postInvalidateDelayed(1000);
    }
}
