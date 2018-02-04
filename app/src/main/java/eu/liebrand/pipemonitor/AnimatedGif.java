package eu.liebrand.pipemonitor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.util.AttributeSet;
import android.view.View;

import java.io.InputStream;

/**
 * Created by mark on 31.01.17.
 */

public class AnimatedGif extends View {
        public Movie mMovie;
        public long movieStart;
        private int gifId;

        public AnimatedGif(Context context) {
            super(context);
        }

        public AnimatedGif(Context context, AttributeSet attrs) {
            super(context, attrs);
            initializeView(attrs.getAttributeResourceValue("http://schemas.android.com/apk/res-auto", "src", 0));
        }

        public AnimatedGif(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            initializeView(attrs.getAttributeResourceValue("http://schemas.android.com/apk/res-auto", "src", 0));
        }

        private void initializeView(final int id) {
            if (!isInEditMode()) {
                InputStream is = getContext().getResources().openRawResource(id);
                mMovie = Movie.decodeStream(is);
                this.gifId = id;
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(Color.TRANSPARENT);
            super.onDraw(canvas);
            long now = android.os.SystemClock.uptimeMillis();

            if (movieStart == 0) {
                movieStart = now;
            }

            if (mMovie != null) {
                int relTime = (int) ((now - movieStart) % mMovie.duration());
                mMovie.setTime(relTime);
                mMovie.draw(canvas, getWidth() - mMovie.width(), getHeight() - mMovie.height());
                //mMovie.draw(canvas, mMovie.width(), mMovie.height());
                this.invalidate();
            }
        }

        public void setGIFResource(int resId) {
            this.gifId = resId;
            initializeView(this.gifId);
        }

        public int getGIFResource() {
            return this.gifId;
        }

}
