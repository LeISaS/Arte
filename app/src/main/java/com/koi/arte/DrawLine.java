package com.koi.arte;


import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.support.v7.app.AlertDialog;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

/**
 * Created by 윤상윤 on 2017-07-17.
 */

public class DrawLine extends View
{
    public Paint   paint = null;   //그리기 조건 기억
    public Bitmap  bitmap = null;  //도화지
    public Canvas  canvas = null;  //붓
    public Path    path;
    public float   PointX;
    public float   PointY;
    public int CurWidth;

    public DrawLine(Context context, Rect rect)
    {
        this(context);

        bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        path = new Path();
    }

    @Override
    public void onDraw(Canvas canvas)
    {
        if(bitmap != null)
        {
            canvas.drawBitmap(bitmap, 0, 0, null);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)      //그림그리는거
    {

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
            {
                path.reset();
                path.moveTo(x, y);
                PointX = x;
                PointY = y;
                return true;
            }
            case MotionEvent.ACTION_MOVE:
            {
                float dx = Math.abs(x - PointX);
                float dy = Math.abs(y - PointY);

                if (dx >= 4 || dy >= 4)
                {

                    canvas.drawPath(path, paint);

                    path.quadTo(PointX, PointY, x, y);
                    PointX = x;
                    PointY = y;

                    invalidate();
                }

                return true;
            }
        }
        return false;
    }

    public void setWidth()  //펜 굵기 설정
    {
        final EditText editText=new EditText(MainActivity.context);
        AlertDialog.Builder ad=new AlertDialog.Builder(MainActivity.context);
        ad.setTitle("1~100사이의 정수");
        ad.setView(editText);
        ad.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            // Event
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String tmpText = editText.getText().toString();
                int width = 0;
                if (!(tmpText.equals(""))) {
                    width = Integer.parseInt(tmpText);
                    if (width >=1 || width <= 100) {
                        CurWidth=width;
                    }
                    CurWidth = width;
                } else {
                    CurWidth = 5;
                }

                paint.setStrokeWidth(CurWidth);
            }
        });
        ad.show();
    }

    public void setLineColor(int color) //펜색상 세팅
    {
        paint = new Paint();
        paint.setColor(color);

        paint.setAlpha(255);
        paint.setDither(true);
        paint.setStrokeWidth(CurWidth);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
    }

    public void setEraser()     //지우개 설정
    {
        paint = new Paint();
        paint.setColor(Color.WHITE);

        paint.setAlpha(255);
        paint.setDither(true);
        paint.setStrokeWidth(CurWidth);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
    }

    public DrawLine(Context context)
    {
        super(context);
    }
}
