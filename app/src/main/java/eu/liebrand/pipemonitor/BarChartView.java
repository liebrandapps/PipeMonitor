package eu.liebrand.pipemonitor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;

import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by mark on 08.02.17.
 */

public class BarChartView extends View {

    private Paint paintSeries, paintAxis, paintBackground, paintTitle;
    private int min, max, zeroLine;
    private Vector<SeriesData> series;
    private int curSeries;
    private Rect rect, rectX, rectY, rectTitle;

    public BarChartView(Context context) {
        super(context);
        init(context);
    }

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BarChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }



    private void init(Context context) {
        paintAxis = new Paint();
        paintAxis.setColor(Color.WHITE);
        paintAxis.setStrokeWidth(5f);
        paintSeries = new Paint();
        paintSeries.setARGB(255, 29,26,168);
        paintBackground = new Paint();
        paintBackground.setARGB(255, 96,96,96);
        paintTitle = new Paint();
        paintTitle.setColor(Color.WHITE);
        paintTitle.setTextSize(24f);

        series= new Vector<>();
        curSeries=-1;
        rect=new Rect();
        rectX=new Rect();
        rectY=new Rect();
        rectTitle=new Rect();
    }

    /**
     *
     * @param values array of int values
     * @return idx in series buffer
     */
    public int addSeries(int [] values) {
        min=Integer.MAX_VALUE;
        max=Integer.MIN_VALUE;
        for(int v : values) {
            if(v<min) min=v;
            if(v>max) max=v;
        }
        int range=max;
        if (range==0) range=100;
        int [] nV=new int[values.length];
        int idx=0;
        for(int v : values) {
            nV[idx++]= v * 100/ range;
        }
        series.add(new SeriesData(nV));
        curSeries=series.size()-1;
        return curSeries;
    }

    public int addSeries(int [] values, String title) {
        return setTitle(addSeries(values), title);

    }

    public void restoreAllSeries(Bundle bundle, int id) {
        series.removeAllElements();
        int count=bundle.getInt(String.valueOf(id)+"seriesCount");
        for(int i=0; i<count; i++) {
            SeriesData s=bundle.getParcelable(String.valueOf(id) + "series" + String.valueOf(i));
            series.add(s);
        }
    }

    public void storeAllSeries(Bundle bundle, int id) {
        Enumeration<SeriesData> enm=series.elements();
        int idx=0;
        while(enm.hasMoreElements()) {
            SeriesData s=enm.nextElement();
            bundle.putParcelable(String.valueOf(id) + "series" + String.valueOf(idx++), s);
        }
        bundle.putInt(String.valueOf(id) + "seriesCount", idx);
    }

    public void setChartProps(int seriesIdx, boolean title, boolean xAxis, boolean yAxis) {
        if(seriesIdx<0) {
            Enumeration<SeriesData> enm=series.elements();
            while(enm.hasMoreElements()) {
                SeriesData sd = enm.nextElement();
                sd.setChartProps(title, xAxis, yAxis);
            }
        }
        else {
            SeriesData sd = series.elementAt(seriesIdx);
            sd.setChartProps(title, xAxis, yAxis);
        }
    }

    public int setTitle(int seriesIdx, String title) {
        if(seriesIdx<0) {
            Enumeration<SeriesData> enm=series.elements();
            while(enm.hasMoreElements()) {
                SeriesData sd = enm.nextElement();
                sd.setTitle(title);
            }
        }
        else {
            SeriesData sd = series.elementAt(seriesIdx);
            sd.setTitle(title);
        }
        return(seriesIdx);
    }

    public void setSpacing(int seriesIdx, int space) {
        if(seriesIdx<0) {
            Enumeration<SeriesData> enm=series.elements();
            while(enm.hasMoreElements()) {
                SeriesData sd = enm.nextElement();
                sd.setSpacing(space);
            }
        }
        else {
            SeriesData sd = series.elementAt(seriesIdx);
            sd.setSpacing(space);
        }
    }

    public int setCurrentSeries(int newSeries) {
        int tmp=curSeries;
        curSeries=newSeries;
        return tmp;
    }

    public int getCurrentSeries() {
        return curSeries;
    }

    public int getSeriesCount() {
        return series.size();
    }

    public void clear() {
        series.removeAllElements();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        Enumeration<SeriesData> enm=series.elements();
        while(enm.hasMoreElements()) {
            SeriesData s=enm.nextElement();
            s.adjustSize(w,h);
        }
    }

    void prepareCharts() {
        int w=getWidth();
        int h=getHeight();
        Enumeration<SeriesData> enm=series.elements();
        while(enm.hasMoreElements()) {
            SeriesData s=enm.nextElement();
            s.adjustSize(w,h);
        }
    }
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(canvas.getClipBounds(),paintBackground);
        if(series.size()==0) {

        }
        else {
            SeriesData sd = series.elementAt(curSeries);
            if(sd.hasTitle()) {
                canvas.drawText(sd.getTitleText(), (float)sd.getTitleRect().left+5, (float)sd.getTitleRect().top-5, paintTitle);
            }
            if(sd.hasyAxis()) {
                Rect r=sd.getyAxisRect();
                canvas.drawLine(r.left+7, r.top-7, r.left+7, r.bottom, paintAxis);
            }
            if(sd.hasxAxis()) {
                Rect r=sd.getxAxisRect();
                canvas.drawLine(r.left, r.bottom, r.right-5, r.bottom, paintAxis);
            }
            Rect r=sd.getChartRect();
            int chartHeight= r.top-r.bottom-5;
            int chartWidth = r.right - r.left;
            int[] values = sd.getValues();
            int spacing = sd.getSpacing();
            int barWidth = chartWidth / values.length;
            int spacePixel = barWidth * spacing / 100;
            int barPixel = barWidth - spacePixel;
            int ptrX = (spacePixel / 2) + r.left;
            for (int value : values) {
                int scaledValue=chartHeight - (value * chartHeight / 100);
                rect.set(ptrX, r.top, ptrX+barPixel, r.bottom + scaledValue);
                canvas.drawRect(rect, paintSeries);
                ptrX+=barWidth;
            }
        }
    }



    public int [] generateRandomData(int howMany) {
        int min=1;
        int max=200;
        int [] values=new int[howMany];
        for (int c=0; c<howMany; c++) {
            values[c] = ThreadLocalRandom.current().nextInt(min, max + 1);
        }
        return values;
    }
}
