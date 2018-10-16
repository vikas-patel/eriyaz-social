package com.eriyaz.social.photomovie;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;

import com.hw.photomovie.opengl.BitmapTexture;
import com.hw.photomovie.opengl.GLESCanvas;
import com.hw.photomovie.segment.BitmapInfo;
import com.hw.photomovie.segment.FitCenterSegment;
import com.hw.photomovie.util.AppResources;
import com.hw.photomovie.util.PhotoUtil;

/**
 * Created by huangwei on 2018/9/4 0004.
 */
public class FitCenterSubtitleSegment extends FitCenterSegment {

    protected RectF mSubtitleDstRect = new RectF();
    private BitmapInfo bitmapInfoSubtitle;
    private static final int MARGIN_BOTTOM_DP = 10;
    private int mMargin;
    private String subtitle = "RateMySinging App";
    public int textSizeSp = 30;

    public FitCenterSubtitleSegment(int duration) {
        super(duration);
    }

    @Override
    public void drawFrame(GLESCanvas canvas, float segmentProgress) {
        super.drawFrame(canvas, segmentProgress);
        drawSubtitle(canvas);
    }

    protected void drawSubtitle(GLESCanvas canvas) {
        float W = mViewportRect.width();
        float H = mViewportRect.height();
        float w = bitmapInfoSubtitle.srcShowRect.width();
        float h = bitmapInfoSubtitle.srcShowRect.height();
        mSubtitleDstRect.set((W - w - mMargin), H - h - mMargin, W - mMargin, H - mMargin);
        canvas.drawTexture(bitmapInfoSubtitle.bitmapTexture, bitmapInfoSubtitle.srcShowRect, mSubtitleDstRect);
    }

    @Override
    public void onPrepare() {
        super.onPrepare();
        prepareSubtitle();
    }

    public void prepareSubtitle() {
        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        float density = AppResources.getInstance().getAppRes().getDisplayMetrics().density;
        mMargin = (int) (density * MARGIN_BOTTOM_DP + 0.5f);
        int textSize = (int) (density * textSizeSp + 0.5f);
        textPaint.setTextSize(textSize);
//        textPaint.setColor(Color.parseColor("#ffcc0000"));
        Bitmap bitmap = genBitmapFromStr(subtitle, textPaint, density);
        bitmapInfoSubtitle = new BitmapInfo();
        bitmapInfoSubtitle.bitmapTexture = new BitmapTexture(bitmap);
        bitmapInfoSubtitle.bitmapTexture.setOpaque(false);
        bitmapInfoSubtitle.srcRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
        bitmapInfoSubtitle.srcShowRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
    }

    private Bitmap genBitmapFromStr(String str, TextPaint textPaint, float density) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        float STROKE_WIDTH_DP = 1.0f;
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        int h = (int) (Math.abs(fontMetrics.ascent) + Math.abs(fontMetrics.descent));
        int w = (int) textPaint.measureText(str);
        Bitmap bitmap = PhotoUtil.createBitmapOrNull(w, h, Bitmap.Config.ARGB_4444);
        if (bitmap == null) {
            return null;
        }
        Canvas canvas = new Canvas(bitmap);
        Rect background = new Rect(0, 0, w, h);
        textPaint.setColor(Color.BLACK);
        canvas.drawRect(background, textPaint);
        // 描外层
        textPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        textPaint.setColor(Color.WHITE);
        textPaint.setStrokeWidth(density * STROKE_WIDTH_DP + 0.5f);  // 描边宽度
        textPaint.setStyle(Paint.Style.FILL_AND_STROKE); //描边种类
        textPaint.setFakeBoldText(true); // 外层text采用粗体
//        textPaint.setShadowLayer(10, 0, 0, Color.WHITE); //字体的阴影效果，可以忽略
        canvas.drawText(str, 0, Math.abs(fontMetrics.ascent), textPaint);

        // 描内层，恢复原先的画笔
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setStrokeWidth(0);
        textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        textPaint.setFakeBoldText(false);
        textPaint.setShadowLayer(0, 0, 0, 0);
        canvas.drawText(str, 0, Math.abs(fontMetrics.ascent), textPaint);

        return bitmap;
    }
}
