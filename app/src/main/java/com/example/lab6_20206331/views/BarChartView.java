package com.example.lab6_20206331.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class BarChartView extends View {

    private List<BarData> barDataList = new ArrayList<>();
    private Paint barPaint;
    private Paint textPaint;
    private Paint linePaint;
    private Paint valuePaint;

    public static class BarData {
        public String label;
        public float ingresos;
        public float egresos;

        public BarData(String label, float ingresos, float egresos) {
            this.label = label;
            this.ingresos = ingresos;
            this.egresos = egresos;
        }
    }

    public BarChartView(Context context) {
        super(context);
        init();
    }

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(32);
        textPaint.setTextAlign(Paint.Align.CENTER);

        valuePaint.setColor(Color.BLACK);
        valuePaint.setTextSize(28);
        valuePaint.setTextAlign(Paint.Align.CENTER);

        linePaint.setColor(Color.GRAY);
        linePaint.setStrokeWidth(2);
    }

    public void setData(List<BarData> data) {
        this.barDataList = data;
        invalidate();
    }

    // Método especializado para datos de resumen financiero
    public void setFinancialData(float ingresos, float egresos) {
        barDataList.clear();

        // Tres barras: Ingresos, Egresos, Consolidado
        barDataList.add(new BarData("Ingresos", ingresos, 0));
        barDataList.add(new BarData("Egresos", 0, egresos));
        barDataList.add(new BarData("Total", ingresos + egresos, 0)); // Consolidado

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (barDataList.isEmpty()) {
            drawNoDataMessage(canvas);
            return;
        }

        int width = getWidth();
        int height = getHeight();
        int padding = 100;
        int chartHeight = height - padding * 2;
        int chartWidth = width - padding * 2;

        // Encuentra el valor máximo para escalar
        float maxValue = 0;
        for (BarData data : barDataList) {
            float totalValue = data.ingresos + data.egresos;
            maxValue = Math.max(maxValue, totalValue);
        }

        if (maxValue == 0) {
            drawNoDataMessage(canvas);
            return;
        }

        // Dibuja los ejes
        drawAxes(canvas, padding, width, height);

        // Dibuja las barras
        float barWidth = (float) chartWidth / (barDataList.size() * 2); // Espaciado mejorado
        DecimalFormat formatter = new DecimalFormat("#,##0");

        for (int i = 0; i < barDataList.size(); i++) {
            BarData data = barDataList.get(i);
            float x = padding + i * barWidth * 1.8f; // Mejor espaciado entre barras

            if (data.label.equals("Ingresos")) {
                // Barra de ingresos (verde)
                barPaint.setColor(Color.parseColor("#4CAF50"));
                float ingresosHeight = (data.ingresos / maxValue) * chartHeight;
                canvas.drawRect(x, height - padding - ingresosHeight,
                        x + barWidth, height - padding, barPaint);

                // Valor encima de la barra
                if (data.ingresos > 0) {
                    canvas.drawText("S/. " + formatter.format(data.ingresos),
                            x + barWidth/2, height - padding - ingresosHeight - 10, valuePaint);
                }

            } else if (data.label.equals("Egresos")) {
                // Barra de egresos (rojo)
                barPaint.setColor(Color.parseColor("#F44336"));
                float egresosHeight = (data.egresos / maxValue) * chartHeight;
                canvas.drawRect(x, height - padding - egresosHeight,
                        x + barWidth, height - padding, barPaint);

                // Valor encima de la barra
                if (data.egresos > 0) {
                    canvas.drawText("S/. " + formatter.format(data.egresos),
                            x + barWidth/2, height - padding - egresosHeight - 10, valuePaint);
                }

            } else if (data.label.equals("Total")) {
                // Barra consolidada (azul)
                barPaint.setColor(Color.parseColor("#2196F3"));
                float totalHeight = (data.ingresos / maxValue) * chartHeight; // data.ingresos contiene el total
                canvas.drawRect(x, height - padding - totalHeight,
                        x + barWidth, height - padding, barPaint);

                // Valor encima de la barra
                if (data.ingresos > 0) {
                    canvas.drawText("S/. " + formatter.format(data.ingresos),
                            x + barWidth/2, height - padding - totalHeight - 10, valuePaint);
                }
            }

            // Etiqueta debajo de la barra
            canvas.drawText(data.label, x + barWidth/2, height - padding + 50, textPaint);
        }

        // Leyenda
        drawLegend(canvas, width, padding);
    }

    private void drawNoDataMessage(Canvas canvas) {
        String message = "Sin datos para mostrar";
        canvas.drawText(message, getWidth() / 2f, getHeight() / 2f, textPaint);
    }

    private void drawAxes(Canvas canvas, int padding, int width, int height) {
        // Eje Y
        canvas.drawLine(padding, padding, padding, height - padding, linePaint);
        // Eje X
        canvas.drawLine(padding, height - padding, width - padding, height - padding, linePaint);
    }

    private void drawLegend(Canvas canvas, int width, int padding) {
        Paint legendPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        legendPaint.setTextSize(24);
        legendPaint.setColor(Color.BLACK);
        legendPaint.setTextAlign(Paint.Align.LEFT);

        int legendX = width - 200;
        int legendY = padding + 20;

        // Leyenda Ingresos
        barPaint.setColor(Color.parseColor("#4CAF50"));
        canvas.drawRect(legendX, legendY, legendX + 20, legendY + 20, barPaint);
        canvas.drawText("Ingresos", legendX + 30, legendY + 15, legendPaint);

        // Leyenda Egresos
        barPaint.setColor(Color.parseColor("#F44336"));
        canvas.drawRect(legendX, legendY + 35, legendX + 20, legendY + 55, barPaint);
        canvas.drawText("Egresos", legendX + 30, legendY + 50, legendPaint);

        // Leyenda Consolidado
        barPaint.setColor(Color.parseColor("#2196F3"));
        canvas.drawRect(legendX, legendY + 70, legendX + 20, legendY + 90, barPaint);
        canvas.drawText("Total", legendX + 30, legendY + 85, legendPaint);
    }
}