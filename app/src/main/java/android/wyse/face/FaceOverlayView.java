package android.wyse.face;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera.Face;
import android.util.Log;
import android.view.View;
/*
 * Created by cis on 13/09/17.
 */

/**
 * This class is a simple View to display the faces.
 */
public class FaceOverlayView extends View {

    private Paint mPaint;
    private int mDisplayOrientation;
    private int mOrientation;
    private Face[] mFaces;

    public FaceOverlayView(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {
        // We want a green box around the face:
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.RED);
        mPaint.setAlpha(128);
       mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(10);


        Paint mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setDither(true);
        mTextPaint.setTextSize(10);
        mTextPaint.setColor(Color.YELLOW);
        mTextPaint.setStyle(Paint.Style.STROKE);
    }

    public void setFaces(Face[] faces) {
        mFaces = faces;
        invalidate();
    }

    private Rect[] rects;
    public void setFaces(Rect[] rects) {
        this.rects = rects;
        invalidate();
    }

    public void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    public void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mFaces != null && mFaces.length > 0) {
            Matrix matrix = new Matrix();
            prepareMatrix(matrix, false, mDisplayOrientation, getWidth(), getHeight());
            canvas.save();
            matrix.postRotate(mOrientation);
            canvas.rotate(-mOrientation);
            RectF rectF = new RectF();

            for (Face face : mFaces) {
                rectF.set(face.rect);
                matrix.mapRect(rectF);
                Log.d("ActualFace",rectF.left+","+rectF.top+","+rectF.right +","+rectF.bottom);
                canvas.drawRect(rectF.left-150 , rectF.top-50, rectF.right + 50, rectF.bottom-150 , mPaint);
               // canvas.drawText("Score " + face.score, rectF.right, rectF.top, mTextPaint);
               // Log.d("Score", "Score " + face.score);
            }
            canvas.restore();

        }else {
            if (rects!=null){
                Matrix matrix = new Matrix();
                prepareMatrix(matrix, false, mDisplayOrientation, getWidth(), getHeight());
                canvas.save();
                matrix.postRotate(mOrientation);
                canvas.rotate(-mOrientation);
                RectF rectF = new RectF();

                for (Rect face : rects) {
                    rectF.set(face);
                    matrix.mapRect(rectF);
                    Log.d("ActualFace",rectF.left+","+rectF.top+","+rectF.right +","+rectF.bottom);
                    canvas.drawRect(rectF.left, rectF.top, rectF.right , rectF.bottom , mPaint);
                    // canvas.drawText("Score " + face.score, rectF.right, rectF.top, mTextPaint);
                    // Log.d("Score", "Score " + face.score);
                }
                canvas.restore();
            }
        }
    }

    public static void prepareMatrix(Matrix matrix, boolean mirror, int displayOrientation,
                                     int viewWidth, int viewHeight) {
        // Need mirror for front camera.
        matrix.setScale(mirror ? -1 : 1, 1);
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        matrix.postRotate(displayOrientation);
        // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
        // UI coordinates range from (0, 0) to (width, height).
        matrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
        matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
    }

}
