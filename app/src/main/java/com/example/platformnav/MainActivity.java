package com.example.platformnav;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    // Location data (similar to the Flutter version, but in Java)
    private final List<Map<String, Object>> _locations = new ArrayList<>();
    private final List<Map<String, String>> _connections = new ArrayList<>();
    private String _startLocation = "";
    private String _endLocation = "";
    private List<String> _route = new ArrayList<>();
    private TextToSpeech _tts;
    private EditText _searchBox;
    private ListView _searchResultsList;
    private ArrayAdapter<String> _searchAdapter;
    private List<String> _filteredLocations = new ArrayList<>();
    private boolean _isSearching = false;
    private MapView _mapView;  // Custom view for displaying the map

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize TTS
        _tts = new TextToSpeech(this, this);

        // Initialize locations and connections
        initializeData();

        // Initialize UI elements
        final TextView startLocationText = findViewById(R.id.start_location_text);
        final TextView endLocationText = findViewById(R.id.end_location_text);
        final TextView routeDisplayTextView = findViewById(R.id.route_display_text);
        final Button startNavigationButton = findViewById(R.id.start_navigation_button);
        _searchBox = findViewById(R.id.search_box);
        _searchResultsList = findViewById(R.id.search_results);
        _mapView = findViewById(R.id.map_container);

        // Set up search functionality
        _filteredLocations = getLocationNames(); // Initialize with all location names
        _searchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, _filteredLocations);
        _searchResultsList.setAdapter(_searchAdapter);
        _searchResultsList.setVisibility(View.GONE); // Initially hide the list

        _searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase();
                _filteredLocations.clear();
                if (query.isEmpty()) {
                    _isSearching = false;
                } else {
                    _isSearching = true;
                    for (Map<String, Object> location : _locations) {
                        if (((String) location.get("name")).toLowerCase().contains(query)) {
                            _filteredLocations.add((String) location.get("name"));
                        }
                    }
                }
                _searchAdapter.notifyDataSetChanged();
                _searchResultsList.setVisibility(_isSearching ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        _searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    _searchResultsList.setVisibility(View.GONE);
                    _isSearching = false;
                    return true;
                }
                return false;
            }
        });

        _searchResultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedName = _searchAdapter.getItem(position);
                String selectedId = getLocationId(selectedName);
                if (_startLocation.isEmpty()) {
                    _startLocation = selectedId;
                    startLocationText.setText(selectedName);
                } else if (_endLocation.isEmpty()) {
                    _endLocation = selectedId;
                    endLocationText.setText(selectedName);
                }
                _isSearching = false;
                _searchResultsList.setVisibility(View.GONE);
                _searchBox.setText("");
                _filteredLocations.clear();  //clear the list
                _searchAdapter.notifyDataSetChanged();
                _mapView.invalidate(); // Redraw the map to show selections
            }
        });

        // Set click listener for the start navigation button
        startNavigationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!_startLocation.isEmpty() && !_endLocation.isEmpty()) {
                    _route = findRoute(_startLocation, _endLocation);
                    StringBuilder routeText = new StringBuilder();
                    if (_route != null && !_route.isEmpty()) {
                        for (String step : _route) {
                            routeText.append("• ").append(step).append("\n");
                        }
                        routeDisplayTextView.setText(routeText.toString());
                        speakRoute(_route);
                    } else {
                        routeDisplayTextView.setText("No route found.");
                        speak("No route found.");
                    }
                } else {
                    speak("Please select start and end locations.");
                }
                _mapView.invalidate(); // Redraw the map to show the route
            }
        });
        _mapView.setLocations(_locations);
        _mapView.setConnections(_connections);
        _mapView.setStartLocation(_startLocation);
        _mapView.setEndLocation(_endLocation);
        _mapView.setRoute(_route);
    }

    private void initializeData() {
        // Initialize location data
        _locations.add(new HashMap<String, Object>() {{
            put("id", "platform1");
            put("x", 50.0f);
            put("y", 100.0f);
            put("name", "Platform 1");
            put("type", "platform");
        }});
        _locations.add(new HashMap<String, Object>() {{
            put("id", "ticketCounter");
            put("x", 200.0f);
            put("y", 150.0f);
            put("name", "Ticket Counter");
            put("type", "facility");
        }});
        _locations.add(new HashMap<String, Object>() {{
            put("id", "restroom1");
            put("x", 100.0f);
            put("y", 250.0f);
            put("name", "Restroom 1");
            put("type", "facility");
        }});
        _locations.add(new HashMap<String, Object>() {{
            put("id", "waitingArea");
            put("x", 250.0f);
            put("y", 50.0f);
            put("name", "Waiting Area");
            put("type", "facility");
        }});
        _locations.add(new HashMap<String, Object>() {{
            put("id", "exit1");
            put("x", 350.0f);
            put("y", 200.0f);
            put("name", "Exit 1");
            put("type", "exit");
        }});
        _locations.add(new HashMap<String, Object>() {{
            put("id", "platform2");
            put("x", 400.0f);
            put("y", 300.0f);
            put("name", "Platform 2");
            put("type", "platform");
        }});

        // Initialize connection data
        _connections.add(new HashMap<String, String>() {{
            put("from", "platform1");
            put("to", "ticketCounter");
        }});
        _connections.add(new HashMap<String, String>() {{
            put("from", "ticketCounter");
            put("to", "restroom1");
        }});
        _connections.add(new HashMap<String, String>() {{
            put("from", "ticketCounter");
            put("to", "waitingArea");
        }});
        _connections.add(new HashMap<String, String>() {{
            put("from", "waitingArea");
            put("to", "exit1");
        }});
        _connections.add(new HashMap<String, String>() {{
            put("from", "platform2");
            put("to", "exit1");
        }});
    }

    private String getLocationName(String id) {
        for (Map<String, Object> location : _locations) {
            if (location.get("id").equals(id)) {
                return (String) location.get("name");
            }
        }
        return "Unknown Location";
    }
    private String getLocationId(String name) {
        for (Map<String, Object> location : _locations) {
            if (location.get("name").equals(name)) {
                return (String) location.get("id");
            }
        }
        return null;
    }

    private List<String> getLocationNames() {
        List<String> names = new ArrayList<>();
        for (Map<String, Object> location : _locations) {
            names.add((String) location.get("name"));
        }
        return names;
    }

    // A* search algorithm (simplified for this static map)
    private List<String> findRoute(String startId, String endId) {
        if (startId.equals(endId)) {
            List<String> result = new ArrayList<>();
            result.add(getLocationName(startId));
            return result;
        }

        Map<String, Node> nodeMap = new HashMap<>();
        for (Map<String, Object> location : _locations) {
            String id = (String) location.get("id");
            String name = (String) location.get("name");
            nodeMap.put(id, new Node(id, name));
        }

        for (Map<String, String> connection : _connections) {
            Node fromNode = nodeMap.get(connection.get("from"));
            Node toNode = nodeMap.get(connection.get("to"));
            if (fromNode != null && toNode != null) {
                fromNode.neighbors.add(toNode);
            }
        }

        Node startNode = nodeMap.get(startId);
        Node endNode = nodeMap.get(endId);

        if (startNode == null || endNode == null) {
            return new ArrayList<>(); // Return empty list if start or end is not found
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<Node, Node> cameFrom = new HashMap<>();

        startNode.gScore = 0;
        startNode.fScore = heuristicCostEstimate(startNode, endNode);
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.id.equals(endNode.id)) {
                return reconstructPath(cameFrom, current);
            }

            current.visited = true;

            for (Node neighbor : current.neighbors) {
                if (neighbor.visited) continue;

                double tentativeGScore = current.gScore + 1; // Assuming each step costs 1

                if (!openSet.contains(neighbor)) {
                    openSet.add(neighbor);
                } else if (tentativeGScore >= neighbor.gScore) {
                    continue;
                }

                cameFrom.put(neighbor, current);
                neighbor.gScore = tentativeGScore;
                neighbor.fScore = tentativeGScore + heuristicCostEstimate(neighbor, endNode);
            }
        }
        return new ArrayList<>();
    }

    private double heuristicCostEstimate(Node start, Node end) {
        // Use Manhattan distance as a simple heuristic (for demonstration)
        float startX = (float) _locations.stream().filter(loc -> ((String) loc.get("id")).equals(start.id)).findFirst().get().get("x");
        float startY = (float) _locations.stream().filter(loc -> ((String) loc.get("id")).equals(start.id)).findFirst().get().get("y");
        float endX = (float) _locations.stream().filter(loc -> ((String) loc.get("id")).equals(end.id)).findFirst().get().get("x");
        float endY = (float) _locations.stream().filter(loc -> ((String) loc.get("id")).equals(end.id)).findFirst().get().get("y");
        return Math.abs(startX - endX) + Math.abs(startY - endY);
    }

    private List<String> reconstructPath(Map<Node, Node> cameFrom, Node current) {
        List<String> path = new ArrayList<>();
        path.add(current.name);
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(0, current.name); // Insert at the beginning
        }
        return path;
    }

    // TTS methods
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = _tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Language is not supported");
            }
        } else {
            Log.e("TTS", "Initialization failed");
        }
    }

    private void speak(String text) {
        _tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void speakRoute(List<String> route) {
        StringBuilder text = new StringBuilder("Starting navigation. ");
        for (String step : route) {
            text.append("Then, go to ").append(step).append(". ");
        }
        text.append("You have reached your destination.");
        _tts.speak(text.toString(), TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    protected void onDestroy() {
        if (_tts != null) {
            _tts.stop();
            _tts.shutdown();
        }
        super.onDestroy();
    }

    // Inner class for Node (A* algorithm)
    private static class Node implements Comparable<Node> {
        String id;
        String name;
        double gScore;
        double fScore;
        List<Node> neighbors;
        boolean visited;

        Node(String id, String name) {
            this.id = id;
            this.name = name;
            this.gScore = Double.POSITIVE_INFINITY;
            this.fScore = Double.POSITIVE_INFINITY;
            this.neighbors = new ArrayList<>();
            this.visited = false;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.fScore, other.fScore);
        }
    }

    //Custom view for map
    public static class MapView extends View {
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
}
