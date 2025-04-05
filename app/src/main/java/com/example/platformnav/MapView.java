package com.example.platformnav;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapView extends View {
    private List<Map<String, Object>> locations;
    private List<Map<String, String>> connections;
    private String startLocationId;
    private String endLocationId;
    private List<String> route;
    private Paint locationPaint;
    private Paint connectionPaint;
    private Paint startPaint;
    private Paint endPaint;
    private float scaleFactor = 1.0f;
    private float panX = 0.0f;
    private float panY = 0.0f;
    private float previousX;
    private float previousY;
    private boolean isDragging = false;
    private float minX = Float.MAX_VALUE;
    private float minY = Float.MAX_VALUE;
    private float maxX = Float.MIN_VALUE;
    private float maxY = Float.MIN_VALUE;
    private boolean initialScaleDone = false;


    public MapView(Context context) {
        super(context);
        init();
    }

    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        locationPaint = new Paint();
        locationPaint.setColor(Color.BLUE);
        locationPaint.setStyle(Paint.Style.FILL);
        connectionPaint = new Paint();
        connectionPaint.setColor(Color.GRAY);
        connectionPaint.setStyle(Paint.Style.STROKE);
        connectionPaint.setStrokeWidth(3);
        startPaint = new Paint();
        startPaint.setColor(Color.GREEN);
        startPaint.setStyle(Paint.Style.FILL);
        endPaint = new Paint();
        endPaint.setColor(Color.RED);
        endPaint.setStyle(Paint.Style.FILL);
        route = new ArrayList<>();
    }

    public void setLocations(List<Map<String, Object>> locations) {
        this.locations = locations;
        calculateMinMax();
        initialScaleDone = false; //reset initial scaling
        requestLayout(); // Trigger a layout pass
        invalidate();
    }

    public void setConnections(List<Map<String, String>> connections) {
        this.connections = connections;
        invalidate();
    }

    public void setStartLocation(String startLocationId) {
        this.startLocationId = startLocationId;
        invalidate();
    }

    public void setEndLocation(String endLocationId) {
        this.endLocationId = endLocationId;
        invalidate();
    }

    public void setRoute(List<String> route) {
        this.route = route;
        invalidate();
    }

    private void calculateMinMax() {
        if (locations == null || locations.isEmpty()) return;

        minX = Float.MAX_VALUE;
        minY = Float.MAX_VALUE;
        maxX = Float.MIN_VALUE;
        maxY = Float.MIN_VALUE;

        for (Map<String, Object> location : locations) {
            float x = (float) location.get("x");
            float y = (float) location.get("y");
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (locations == null || locations.isEmpty()) return;

        float canvasWidth = getWidth();
        float canvasHeight = getHeight();

        if (!initialScaleDone) {
            // Calculate the scale factor to fit the entire map within theview
            float mapWidth = maxX - minX;
            float mapHeight = maxY - minY;

            if (mapWidth > 0 && mapHeight > 0) {
                float scaleX = canvasWidth / mapWidth;
                float scaleY = canvasHeight / mapHeight;
                scaleFactor = Math.min(scaleX, scaleY) * 0.8f; //add a buffer of 0.8
            }
            //calculate the bounds of the map after scaling.
            float scaledMapWidth = mapWidth * scaleFactor;
            float scaledMapHeight = mapHeight * scaleFactor;

            //calculate the offset to center the map.
            panX = (canvasWidth - scaledMapWidth) / 2 - minX * scaleFactor;
            panY = (canvasHeight - scaledMapHeight) / 2 - minY * scaleFactor;
            initialScaleDone = true;
        }
        canvas.save();
        canvas.translate(panX, panY);
        canvas.scale(scaleFactor, scaleFactor);

        // Draw connections
        if (connections != null) {
            for (Map<String, String> connection : connections) {
                String fromId = connection.get("from");
                String toId = connection.get("to");

                float startX = 0, startY = 0, endX = 0, endY = 0;
                boolean foundStart = false, foundEnd = false;

                for (Map<String, Object> location : locations) {
                    if (location.get("id").equals(fromId)) {
                        startX = (float) location.get("x");
                        startY = (float) location.get("y");
                        foundStart = true;
                    }
                    if (location.get("id").equals(toId)) {
                        endX = (float) location.get("x");
                        endY = (float) location.get("y");
                        foundEnd = true;
                    }
                    if (foundStart && foundEnd) break;
                }
                canvas.drawLine(startX, startY, endX, endY, connectionPaint);
            }
        }

        // Draw locations
        for (Map<String, Object> location : locations) {
            float x = (float) location.get("x");
            float y = (float) location.get("y");
            String id = (String) location.get("id");
            Paint paint = locationPaint;
            if (id.equals(startLocationId)) {
                paint = startPaint;
            } else if (id.equals(endLocationId)) {
                paint = endPaint;
            }
            canvas.drawCircle(x, y, 10, paint);
        }
        //draw route
        if(route != null && !route.isEmpty()){
            float startX = 0, startY = 0, endX = 0, endY = 0;
            boolean foundStart = false;
            for(int i=0; i< route.size()-1; i++){
                String fromName = route.get(i);
                String toName = route.get(i+1);
                for (Map<String, Object> location : locations) {
                    if ( ((String)location.get("name")).equals(fromName)) {
                        startX = (float) location.get("x");
                        startY = (float) location.get("y");
                        foundStart = true;
                    }
                    if ( ((String)location.get("name")).equals(toName)) {
                        endX = (float) location.get("x");
                        endY = (float) location.get("y");
                        break;
                    }
                }
                if(foundStart){
                    connectionPaint.setColor(Color.GREEN);
                    canvas.drawLine(startX, startY, endX, endY, connectionPaint);
                }
            }
        }
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDragging = true;
                previousX = x;
                previousY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    float deltaX = x - previousX;
                    float deltaY = y - previousY;
                    panX += deltaX;
                    panY += deltaY;
                    previousX = x;
                    previousY = y;
                    invalidate(); // Redraw the map
                }
                break;
            case MotionEvent.ACTION_UP:
                isDragging = false;
                break;
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                break;
        }
        return true;
    }
}