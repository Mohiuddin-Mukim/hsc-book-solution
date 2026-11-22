package bd.hscsolution.student.ui;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

public final class ZoomImageView extends AppCompatImageView {
    private final Matrix matrix = new Matrix();
    private final float[] values = new float[9];
    private ScaleGestureDetector detector;
    private float scale = 1f;
    private float lastX, lastY;
    private boolean dragging;

    public ZoomImageView(Context c) { super(c); init(); }
    public ZoomImageView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        setScaleType(ScaleType.MATRIX);
        detector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(@NonNull ScaleGestureDetector d) {
                float next = Math.max(1f, Math.min(4f, scale * d.getScaleFactor()));
                float factor = next / scale;
                scale = next;
                matrix.postScale(factor, factor, d.getFocusX(), d.getFocusY());
                setImageMatrix(matrix);
                return true;
            }
        });
        setOnTouchListener((v,e) -> {
            detector.onTouchEvent(e);
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastX=e.getX(); lastY=e.getY(); dragging=true; return true;
                case MotionEvent.ACTION_MOVE:
                    if (!detector.isInProgress() && scale > 1f) {
                        float dx=e.getX()-lastX, dy=e.getY()-lastY;
                        matrix.postTranslate(dx,dy); setImageMatrix(matrix);
                    }
                    lastX=e.getX(); lastY=e.getY(); return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    dragging=false; return true;
            }
            return true;
        });
    }

    @Override public void setImageDrawable(Drawable d) {
        super.setImageDrawable(d);
        matrix.reset(); scale=1f; setImageMatrix(matrix);
    }
}
