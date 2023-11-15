package com.text.recognition;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.text.Text;

import java.util.HashSet;
import java.util.Set;

public class GraphicOverlay extends View {

    private final Object lock = new Object();
    private int previewWidth;
    private float widthScaleFactor = 1.0f;
    private int previewHeight;
    private float heightScaleFactor = 1.0f;
    private Set<Graphic> graphics = new HashSet<>();
    private Text text;
    private Paint paint;
    private String highlightString;
    private static final int TEXT_COLOR = Color.TRANSPARENT;

    private static final int TEXT_COLOR_GREEN = 0x8000FF00;
    private static final float TEXT_SIZE = 44.0f;
    private static final float STROKE_WIDTH = 0.0f;
    private Paint rectPaint;
    private Paint textPaint;
    private int TICK_COLOR = Color.GREEN;
    private int facing = CameraSource.CAMERA_FACING_BACK;
    private ImageView tickMarkImageView;
    private Context mContext;
    private static final float MIN_CONFIDENCE = 0.8f;
    private String[] filteredWords;

    /**
     * Base class for a custom graphics object to be rendered within the graphic overlay. Subclass
     * this and implement the {@link Graphic#draw(Canvas)} method to define the graphics element. Add
     * instances to the overlay using {@link GraphicOverlay#add(Graphic)}.
     */
    public abstract static class Graphic {
        private GraphicOverlay overlay;

        public Graphic(GraphicOverlay overlay) {
            this.overlay = overlay;
        }

        /**
         * Draw the graphic on the supplied canvas. Drawing should use the following methods to convert
         * to view coordinates for the graphics that are drawn:
         *
         * <ol>
         *   <li>{@link Graphic#scaleX(float)} and {@link Graphic#scaleY(float)} adjust the size of the
         *       supplied value from the preview scale to the view scale.
         *   <li>{@link Graphic#(float)} and {@link Graphic#translateY(float)} adjust the
         *       coordinate from the preview's coordinate system to the view coordinate system.
         * </ol>
         *
         * @param canvas drawing canvas
         */
        public abstract void draw(Canvas canvas);

        /**
         * Adjusts a horizontal value of the supplied value from the preview scale to the view scale.
         */
        public float scaleX(float horizontal) {
            return horizontal * overlay.widthScaleFactor;
        }

        /** Adjusts a vertical value of the supplied value from the preview scale to the view scale. */
        public float scaleY(float vertical) {
            return vertical * overlay.heightScaleFactor;
        }

        /** Returns the application context of the app. */
        public Context getApplicationContext() {
            return overlay.getContext().getApplicationContext();
        }

        /**
         * Adjusts the x coordinate from the preview's coordinate system to the view coordinate system.
         */
        public float translateX(float x) {
            if (overlay.facing == CameraSource.CAMERA_FACING_FRONT) {
                return overlay.getWidth() - scaleX(x);
            } else {
                return scaleX(x);
            }
        }

        /**
         * Adjusts the y coordinate from the preview's coordinate system to the view coordinate system.
         */
        public float translateY(float y) {
            return scaleY(y);
        }

        public void postInvalidate() {
            overlay.postInvalidate();
        }
    }



    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        init();
    }

    /** Removes all graphics from the overlay. */
    public void clear() {
        synchronized (lock) {
            graphics.clear();
        }
        postInvalidate();
    }

    /** Adds a graphic to the overlay. */
    public void add(Graphic graphic) {
        synchronized (lock) {
            graphics.add(graphic);
        }
        postInvalidate();
    }

    public void setText(Text text, String highlighted, String[] filteredWords) {

        this.text = text;
        this.highlightString = highlighted;
        this.filteredWords = filteredWords;
        /*if (highlightString.contains(" ")){
            Log.d(LiveTextRecognitionActivity.class.getSimpleName(), "Yes, string contains space...");
        }else {
            Log.d(LiveTextRecognitionActivity.class.getSimpleName(), "No, string doesn't contains space...");
        }*/
        Log.d(GraphicOverlay.class.getSimpleName(), "Result String : "+ highlightString);
        invalidate(); // Trigger a redraw when the text changes
    }

    /** Removes a graphic from the overlay. */
    public void remove(Graphic graphic) {
        synchronized (lock) {
            graphics.remove(graphic);
        }
        postInvalidate();
    }

    /**
     * Sets the camera attributes for size and facing direction, which informs how to transform image
     * coordinates later.
     */
    public void setCameraInfo(int previewWidth, int previewHeight) {
        synchronized (lock) {
            this.previewWidth = previewWidth;
            this.previewHeight = previewHeight;
            overlayWidth = previewWidth - 150;
            overlayHeight = previewHeight + 150;
            //this.facing = facing;
        }
        postInvalidate();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(TEXT_COLOR_GREEN);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(4.0f);

        rectPaint = new Paint();

        rectPaint.setColor(TEXT_COLOR);

        rectPaint.setStyle(Paint.Style.STROKE);

        rectPaint.setStrokeWidth(STROKE_WIDTH);


    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (text != null) {
            for (String textToHighlight : filteredWords) {
                for (Text.TextBlock block : text.getTextBlocks()) {
                    for (Text.Line line : block.getLines()) {
                        if (!line.getText().isEmpty() && line.getText().length() != 0){
                            String lineText = line.getText().toLowerCase();
                            String[] words = lineText.split("[-\n\\s]+");
                            for (int i = 0; i < words.length; i++) {
                                String word = words[i].trim();
                                if (!word.isEmpty() && similarity(words[i].toLowerCase(), textToHighlight.toLowerCase()) > 0.5) {
                                    try {
                                        Text.Element element = line.getElements().get(i);
                                        Log.d(GraphicOverlay.class.getSimpleName(), "Highlighting element: " + element.getText());

                                        Rect boundingBox = element.getBoundingBox();
                                        float top = translateY(boundingBox.top);
                                        float right = translateX(boundingBox.right);

//                                        Path tickPath = new Path();
//                                        tickPath.moveTo(top - 30.0f, right);
//                                        tickPath.lineTo(top, right + 30.0f);
//                                        tickPath.lineTo(top + 30.0f, right - 30.0f);
//                                        canvas.drawPath(tickPath, paint);

                                        // Draws the bounding box around the TextBlock.
                                        RectF rect = new RectF(element.getBoundingBox());
                                        rect.left = translateX(rect.left);
                                        rect.top = translateY(rect.top);
                                        rect.right = translateX(rect.right);
                                        rect.bottom = translateY(rect.bottom);
                                        // canvas.drawRect(rect, rectPaint);


                                        float centerX = rect.right;

                                        float centerY = rect.top;
                                        Paint tickPaint = new Paint();

                                        tickPaint.setColor(TICK_COLOR);  // Set your desired color for the tick

                                        tickPaint.setStrokeWidth(5);     // Set the width of the tick lines


// Draw the tick mark with rotation

                                        float tickSize = 20;  // Adjust the size of the tick as needed

                                        float rotationDegrees = 30; // Adjust the rotation angle as needed


                                        canvas.save();  // Save the current canvas state

//                                        canvas.rotate(rotationDegrees, centerX, centerY);  // Apply rotation


// Draw the vertical line of the tick

                                        canvas.drawRect(rect,paint);

//                                        canvas.drawLine(centerX, centerY - tickSize, centerX, centerY + tickSize, tickPaint);


// Draw the diagonal line of the tick

//                                        canvas.drawLine(centerX - tickSize, centerY, centerX, centerY + tickSize, tickPaint);


                                        canvas.restore();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }

        /**without error word is highlighting but only for 0th position*/
        /*if (text != null) {
            for (int index = 0 ; index < filteredWords.length; index ++) {
                String textToHighlight = filteredWords[index];
                List<Text.TextBlock> mTextBlockList =text.getTextBlocks();
                for (Text.TextBlock block : mTextBlockList) {
                    for (Text.Line mLine : block.getLines()){
                        String lineText = mLine.getText().toLowerCase();
                        String[] words = lineText.split("[-\n\\s]+");
                        List<String> wordsList = Arrays.asList(words);
                        //int startIndex = wordsList.indexOf(textToHighlight.split("[-\n\\s]+")[0]);
                        int startIndex = wordsList.indexOf(textToHighlight.split("[-\n\\s]+")[0]);
                        if (startIndex != -1) {
                            int endIndex = startIndex + textToHighlight.split("[-\n\\s]+").length;
                            StringBuilder matchedText = new StringBuilder();
                            for (int i = startIndex; i < endIndex; i++) {
                                if (i > startIndex) {
                                    matchedText.append(" ");
                                }
                                try
                                {
                                    matchedText.append(wordsList.get(i));
                                }catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }

                            if (similarity(matchedText.toString().toLowerCase(),textToHighlight.toLowerCase())>0.5) {
                                for (int i = startIndex; i < endIndex; i++) {
                                    try {
                                        Text.Element element = mLine.getElements().get(i);
                                        Log.d(GraphicOverlay.class.getSimpleName(), "Highlighting element : "+element.getText());

                                        Rect boundingBox = element.getBoundingBox();
                                        float top = translateY(boundingBox.top);
                                        float right = translateX(boundingBox.right);

                                        Path tickPath = new Path();
                                        tickPath.moveTo(top - 30.0f, right);
                                        tickPath.lineTo(top, right + 30.0f);
                                        tickPath.lineTo(top + 30.0f, right - 30.0f);
                                        canvas.drawPath(tickPath, paint);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                }
                            }else {
                                Log.d(GraphicOverlay.class.getSimpleName(), "Highlighting element : ELSE ");
                            }
                        }
                    }

                }
            }

        }
*/


        /**error array index out of bound exception is returns while reading words*/
/*        if (text != null) {
            for (Text.TextBlock block : text.getTextBlocks()) {
                if (!block.getLines().isEmpty()){
                    for (Text.Line line : block.getLines()) {
                        String lineText = line.getText().toLowerCase();
                        if (!lineText.trim().isEmpty()){
                            for (Text.Element element : line.getElements()) {
                                String[] toHighlight = highlightString.split(" ");
                                for (String words : toHighlight){
                                    if (!element.getText().trim().isEmpty() && !element.getText().trim().matches("^\\s*$")  && element.getText().trim().equalsIgnoreCase(words)){

                                        Log.d(GraphicOverlay.class.getSimpleName(), "Element of words : "+ element.getText().trim()+" : "+ words);

                                        if (calculateSimilarity(element.getText(), words) >= 0.8) {
                                            System.out.println("Strings are similar.");
                                            Rect boundingBox = element.getBoundingBox();
                                            float top = translateY(boundingBox.top);
                                            float right = translateX(boundingBox.right);

                                            Path tickPath = new Path();
                                            tickPath.moveTo(top - 30.0f, right);
                                            tickPath.lineTo(top, right + 30.0f);
                                            tickPath.lineTo(top + 30.0f, right - 30.0f);
                                            canvas.drawPath(tickPath, paint);
                                        }

                                *//*canvas.drawRect(left, top, right, bottom, paint);
                                if (calculateSimilarity(element.getText(), words) >= 0.8) {
                                    System.out.println("Strings are similar.");
                                    Rect boundingBox = element.getBoundingBox();
                                    float left = translateX(boundingBox.left);
                                    float top = translateY(boundingBox.top);
                                    float right = translateX(boundingBox.right);
                                    float bottom = translateY(boundingBox.bottom);
                                    canvas.drawRect(left, top, right, bottom, paint);
                                }*//*

                                    }
                                }

                            }
                        }

                    }
                }

            }
        }else {
            Log.d(GraphicOverlay.class.getSimpleName(), "Text null....");
        }*/

    }

    public float translateX(float x) {
        return x * getWidth() / overlayWidth;
    }

    public float translateY(float y) {
        return y * getHeight() / overlayHeight;
    }

    public float translateX(float x, float overlayWidth) {
        return x * getWidth() / overlayWidth;
    }

    public float translateY(float y, float overlayHeight) {
        return y * getHeight() / overlayHeight;
    }
    private int overlayWidth;
    private int overlayHeight;
    /** Draws the overlay with its associated graphic objects. */
/*   @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (text != null) {
            String[] highLight = highlightString.split("[-\n\\s]+");
            for (int index = 0 ; index < highLight.length; index ++) {
                String textToHighlight = highLight[index];
            }
            for (Text.TextBlock block : text.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                    for (Text.Element element : line.getElements()) {
                        Log.d(GraphicOverlay.class.getSimpleName(), "Element words : " + element.getText());
                        String[] words = highlightString.split("[-\n\\s]+");
                        for (int i = 0; i < words.length; i++){
                            String wordHigh = words[i];
                            if (similarity(wordHigh.toString().toLowerCase(),element.getText().toLowerCase())>0.5) {
                                float centerX = Objects.requireNonNull(element.getBoundingBox()).right;
                                float centerY = element.getBoundingBox().top;

                                @SuppressLint("DrawAllocation")
                                Paint tickPaint = new Paint();

                                tickPaint.setColor(TICK_COLOR);  // Set your desired color for the tick

                                tickPaint.setStrokeWidth(5);     // Set the width of the tick lines

                                float tickSize = 15;  // Adjust the size of the tick as needed

                                float rotationDegrees = 50; // Adjust the rotation angle as needed
                                canvas.save();  // Save the current canvas state
                                canvas.rotate(rotationDegrees, centerX, centerY);  // Apply rotation
                                canvas.drawLine(centerX-10 - tickSize+10, centerY, centerX, centerY + tickSize, tickPaint);
                                canvas.drawLine(centerX, centerY - tickSize, centerX, centerY + tickSize, tickPaint);
                                canvas.restore();
                            }
                        }
                       if (highlightString.contains(element.getText())){
                            Log.d(GraphicOverlay.class.getSimpleName(), "Highlighted Words : "+element.getText());
                            //canvas.drawRect(element.getBoundingBox(), paint);

                            float centerX = Objects.requireNonNull(element.getBoundingBox()).right;

                            float centerY = element.getBoundingBox().top;

                            @SuppressLint("DrawAllocation")
                            Paint tickPaint = new Paint();

                            tickPaint.setColor(TICK_COLOR);  // Set your desired color for the tick

                            tickPaint.setStrokeWidth(5);     // Set the width of the tick lines

                            float tickSize = 15;  // Adjust the size of the tick as needed

                            float rotationDegrees = 50; // Adjust the rotation angle as needed
                            canvas.save();  // Save the current canvas state
                            canvas.rotate(rotationDegrees, centerX, centerY);  // Apply rotation
                            canvas.drawLine(centerX-10 - tickSize+10, centerY, centerX, centerY + tickSize, tickPaint);
                            canvas.drawLine(centerX, centerY - tickSize, centerX, centerY + tickSize, tickPaint);
                            canvas.restore();

                        }
                    }
                }
            }
        }

    }*/

    public static double calculateSimilarity(String s1, String s2) {
        int maxLength = Math.max(s1.length(), s2.length());

        if (maxLength == 0) {
            return 1.0;  // Both strings are empty, so they are considered identical
        }

        int levenshteinDistance = calculateLevenshteinDistance(s1, s2);
        return 1.0 - (double) levenshteinDistance / maxLength;
    }

    private static int calculateLevenshteinDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                            newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                        }
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0) {
                costs[s2.length()] = lastValue;
            }
        }
        return costs[s2.length()];
    }


    public static double similarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2; shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) { return 1.0; /* both strings are zero length */ }

        double percentage =  (longerLength - editDistance(longer, shorter)) / (double) longerLength;
        Log.d("similarity",percentage+"");
        return percentage;
    }

    // Example implementation of the Levenshtein Edit Distance
// See http://rosettacode.org/wiki/Levenshtein_distance#Java
    public static int editDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0)
                    costs[j] = j;
                else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1))
                            newValue = Math.min(Math.min(newValue, lastValue),
                                    costs[j]) + 1;
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0)
                costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }




    private boolean shouldHighlight(String text) {
        // Customize this method to determine if the text should be highlighted
        // For example, you can check if the text matches a specific keyword
        return text.contains(highlightString);
    }



}
