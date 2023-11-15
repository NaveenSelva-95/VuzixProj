package com.text.recognition;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.google.mlkit.vision.text.Text;

/**unused class*/
public class TextGraphic extends GraphicOverlay.Graphic{

    public TextGraphic(GraphicOverlay overlay) {
        super(overlay);
    }

   private static final int TEXT_COLOR = Color.YELLOW;
    //private static final int TEXT_COLOR_GREEN = 0x6600FF00;
    private static final int TEXT_COLOR_GREEN = 0x66FFFF00;
    private static final float TEXT_SIZE = 40.0f;
    private static final float STROKE_WIDTH = 4.0f;

    private Paint rectPaint;
    private Paint textPaint;
    private Text.Element text;

    TextGraphic(GraphicOverlay overlay, Text.Element text) {
        super(overlay);

        this.text = text;

        rectPaint = new Paint();
        rectPaint.setColor(TEXT_COLOR_GREEN);
        rectPaint.setStyle(Paint.Style.FILL);
        //rectPaint.setStrikeThruText(true);
        //rectPaint.setStrokeWidth(STROKE_WIDTH);
        textPaint = new Paint();
        //*textPaint.setStrikeThruText(true);
        textPaint.setColor(TEXT_COLOR_GREEN);
        textPaint.setTextSize(TEXT_SIZE);
        // Redraw the overlay, as this graphic has been added.
        postInvalidate();
    }

    //** Draws the text block annotations for position, size, and raw value on the supplied canvas. *//*
    @Override
    public void draw(Canvas canvas) {
        if (text == null) {
            throw new IllegalStateException("Attempting to draw a null text.");
        }
        int TICK_COLOR = Color.GREEN;
        // Draws the bounding box around the TextBlock.
        RectF rect = new RectF(text.getBoundingBox());
        rect.left = translateX(rect.left);
        rect.top = translateY(rect.top);
        rect.right = translateX(rect.right);
        rect.bottom = translateY(rect.bottom);
       // canvas.drawRect(rect, rectPaint);


        float centerX = rect.right;

        float centerY = rect.top;


// Set up Paint for the tick mark

        Paint tickPaint = new Paint();

        tickPaint.setColor(TICK_COLOR);  // Set your desired color for the tick

        tickPaint.setStrokeWidth(5);     // Set the width of the tick lines


// Draw the tick mark with rotation

        float tickSize = 20;  // Adjust the size of the tick as needed

        float rotationDegrees = 30; // Adjust the rotation angle as needed


        canvas.save();  // Save the current canvas state

        canvas.rotate(rotationDegrees, centerX, centerY);  // Apply rotation


// Draw the vertical line of the tick

        canvas.drawLine(centerX, centerY - tickSize, centerX, centerY + tickSize, tickPaint);


// Draw the diagonal line of the tick

        canvas.drawLine(centerX - tickSize, centerY, centerX, centerY + tickSize, tickPaint);


        canvas.restore();
    }
}
