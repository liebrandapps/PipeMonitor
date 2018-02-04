package eu.liebrand.pipemonitor;

import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by mark on 19.02.17.
 */

class SeriesData implements Parcelable {

    private int [] values;
    private int spacing;
    private boolean hasxAxis, hasyAxis;
    private boolean hasTitle;
    private boolean hasTotals;
    private Rect title, xAxis, yAxis, chart;
    private String titleText;

    SeriesData(int[] values) {
        this.values=values;
        spacing = 0;
        hasxAxis=false;
        hasyAxis=false;
        hasTitle=false;
        hasTotals=false;
    }

    void adjustSize(int width, int height) {
        title = hasTitle? new Rect(0, 29, width, 0) : new Rect(0,0,0,0);
        yAxis= hasyAxis? new Rect(0, height, 9, title.top+1) : new Rect(0,0,0,0);
        xAxis= hasxAxis? new Rect(yAxis.right+1, height, width, height-10) : new Rect(0,1,0,1);
        chart=new Rect(yAxis.right+1, xAxis.bottom-1, width, title.top+1);
    }

    void setSpacing(int spacing) {
        this.spacing=spacing;
    }

    int getSpacing() {
        return spacing;
    }

    int [] getValues() {
        return values;
    }

    void setChartProps(boolean hasTitle, boolean hasxAxis, boolean hasyAxis) {
        this.hasTitle=hasTitle;
        this.hasxAxis=hasxAxis;
        this.hasyAxis=hasyAxis;
    }

    void setTitle(String title) {
        this.titleText=title;
    }

    String getTitleText() { return titleText; }

    boolean hasTitle() { return hasTitle;}
    boolean hasxAxis() { return hasxAxis;}
    boolean hasyAxis() { return hasyAxis;}
    Rect getTitleRect() { return title; }
    Rect getyAxisRect() { return yAxis; }
    Rect getxAxisRect() { return xAxis; }
    Rect getChartRect() { return chart; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeIntArray(this.values);
        dest.writeInt(this.spacing);
        dest.writeByte(this.hasxAxis ? (byte) 1 : (byte) 0);
        dest.writeByte(this.hasyAxis ? (byte) 1 : (byte) 0);
        dest.writeByte(this.hasTitle ? (byte) 1 : (byte) 0);
        dest.writeByte(this.hasTotals ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.title, flags);
        dest.writeParcelable(this.xAxis, flags);
        dest.writeParcelable(this.yAxis, flags);
        dest.writeParcelable(this.chart, flags);
        dest.writeString(this.titleText);
    }

    protected SeriesData(Parcel in) {
        this.values = in.createIntArray();
        this.spacing = in.readInt();
        this.hasxAxis = in.readByte() != 0;
        this.hasyAxis = in.readByte() != 0;
        this.hasTitle = in.readByte() != 0;
        this.hasTotals = in.readByte() != 0;
        this.title = in.readParcelable(Rect.class.getClassLoader());
        this.xAxis = in.readParcelable(Rect.class.getClassLoader());
        this.yAxis = in.readParcelable(Rect.class.getClassLoader());
        this.chart = in.readParcelable(Rect.class.getClassLoader());
        this.titleText = in.readString();
    }

    public static final Parcelable.Creator<SeriesData> CREATOR = new Parcelable.Creator<SeriesData>() {
        @Override
        public SeriesData createFromParcel(Parcel source) {
            return new SeriesData(source);
        }

        @Override
        public SeriesData[] newArray(int size) {
            return new SeriesData[size];
        }
    };
}
