package com.example.lab6_20206331.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PieChartView extends View {

    private List<PieSlice> slices = new ArrayList<>();
    private Paint paint;
    private RectF rectF;

    public static class PieSlice {
        public float value;
        public int color;
        public String label;

        public PieSlice(float value, int color, String label) {
            this.value = value;
            this.color = color;
            this.label = label;
        }
    }

    public PieChartView(Context context) {
        super(context);
        init();
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectF = new RectF();
    }

    public void setData(List<PieSlice> data) {
        this.slices = data;
        invalidate(); // Redibuja la vista
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (slices.isEmpty()) return;

        // Calcula el total
        float total = 0;
        for (PieSlice slice : slices) {
            total += slice.value;
        }

        // Configura el área del círculo
        int size = Math.min(getWidth(), getHeight());
        int padding = 50;
        rectF.set(padding, padding, size - padding, size - padding);

        // Dibuja cada slice
        float startAngle = 0;
        for (PieSlice slice : slices) {
            float sweepAngle = (slice.value / total) * 360;

            paint.setColor(slice.color);
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint);

            startAngle += sweepAngle;
        }

        // Dibuja las etiquetas
        drawLabels(canvas, total);
    }

    private void drawLabels(Canvas canvas, float total) {
        paint.setColor(Color.BLACK);
        paint.setTextSize(40);

        float startAngle = 0;
        for (PieSlice slice : slices) {
            float sweepAngle = (slice.value / total) * 360;
            float middleAngle = startAngle + sweepAngle / 2;

            // Calcula la posición del texto
            float radius = rectF.width() / 2 * 0.7f;
            float x = rectF.centerX() + (float) (radius * Math.cos(Math.toRadians(middleAngle)));
            float y = rectF.centerY() + (float) (radius * Math.sin(Math.toRadians(middleAngle)));

            // Dibuja el porcentaje
            String percentage = String.format("%.1f%%", (slice.value / total) * 100);
            canvas.drawText(percentage, x, y, paint);

            startAngle += sweepAngle;
        }
    }
}