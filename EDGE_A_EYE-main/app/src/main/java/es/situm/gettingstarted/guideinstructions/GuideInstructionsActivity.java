package es.situm.gettingstarted.guideinstructions;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.SensorEventListener;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import es.situm.gettingstarted.R;
import es.situm.gettingstarted.common.SampleActivity;
import es.situm.sdk.SitumSdk;
import es.situm.sdk.directions.DirectionsRequest;
import es.situm.sdk.error.Error;
import es.situm.sdk.location.LocationListener;
import es.situm.sdk.location.LocationManager;
import es.situm.sdk.location.LocationRequest;
import es.situm.sdk.location.LocationStatus;
import es.situm.sdk.location.util.CoordinateConverter;
import es.situm.sdk.model.cartography.Building;
import es.situm.sdk.model.cartography.Floor;
import es.situm.sdk.model.cartography.Poi;
import es.situm.sdk.model.cartography.Point;
import es.situm.sdk.model.directions.Indication;
import es.situm.sdk.model.directions.Route;
import es.situm.sdk.model.directions.RouteSegment;
import es.situm.sdk.model.location.Bounds;
import es.situm.sdk.model.location.CartesianCoordinate;
import es.situm.sdk.model.location.Coordinate;
import es.situm.sdk.model.location.Location;
import es.situm.sdk.model.navigation.NavigationProgress;
import es.situm.sdk.navigation.NavigationListener;
import es.situm.sdk.navigation.NavigationRequest;
import es.situm.sdk.utils.Handler;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import java.util.Locale;
import java.util.Objects;


import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class GuideInstructionsActivity extends SampleActivity implements OnMapReadyCallback, TextToSpeech.OnInitListener, SensorEventListener {
    private TextToSpeech tts;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastUpdate;
    private long dernierMessageAutomatique;
    private long lastShakeTime;

    private static final int SHAKE_THRESHOLD = 1200;
    private static final long SHAKE_DELAY = 3000; // délai de 2 secondes
    private static final long DELAI_AUTOMATIQUE = 5000; // délai de 5 secondes
    private float last_x, last_y, last_z;
    //---------------------------------------

    private final static String TAG = GuideInstructionsActivity.class.getSimpleName();
    private final int ACCESS_FINE_LOCATION_REQUEST_CODE = 3096;

    private GoogleMap googleMap;
    private NavigationRequest navigationRequest;
    private final List<Polyline> polylines = new ArrayList<>();

    private LocationManager locationManager;
    private LocationListener locationListener;
    private Location current;
    private Point to;

    private Building building;
    private ProgressBar progressBar;
    private RelativeLayout navigationLayout;
    private TextView mNavText;

    private Marker prev;
    private Marker markerDestination;

    private boolean navigation = false;
    private String floorId;

    boolean isMapShow;
    private CoordinateConverter coordinateConverter;

    private List<Poi> maPoiList = new ArrayList<>();

    private String firstIndication;

    private List<Indication> liste_indications;

    private Indication indicationActuelle;

    private Boolean estArrive;
    private Boolean premiereIndicationDonnee;

    private Boolean sortiChemin;
    private Poi selectedPoi;

    /**
     * Getting the permisions we need about localization.
     *
     */
    private void requestPermisions(){
        ActivityCompat.requestPermissions(GuideInstructionsActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                ACCESS_FINE_LOCATION_REQUEST_CODE);
    }

    /**
     * Checking if we have the requested permissions
     *
     */
    private void checkPermisions(){
        if(ContextCompat.checkSelfPermission(GuideInstructionsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(GuideInstructionsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)){

                Snackbar.make(findViewById(android.R.id.content),
                                "Need location permission to enable sevice",
                                Snackbar.LENGTH_INDEFINITE)
                        .setAction("Open", view -> requestPermisions()).show();
            }else{
                requestPermisions();
            }
        }
    }

    /**
     *
     * REQUESTCODE = 1 : NO PERMISSIONS
     *
     */

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACCESS_FINE_LOCATION_REQUEST_CODE) {
            if (!(grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                finishActivity(1);
            }
        }
    }

    // END REQUEST PERMISIONS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Récupérer les préférences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showMap = prefs.getBoolean("show_map", true);

        if (showMap){
            setContentView(R.layout.activity_guide_indications);
        }
        else{
            setContentView(R.layout.activity_guide_indications_nomap);
        }
        locationManager = SitumSdk.locationManager();
        building = getBuildingFromIntent();
        setup();
        checkLocationManager();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        if (!showMap) {
            // Masquer la vue de la carte
            View mapView = mapFragment.getView();
            if (mapView != null) {
                mapView.setVisibility(View.GONE);
            }
        }

        //Code de Justin pour aller chercher les pois avec le truc dans le dossier
        GetPoisCaseUse getPoisCaseUse = new GetPoisCaseUse();
        getPoisCaseUse.get(building, new GetPoisCaseUse.Callback() {
            @Override
            public void onSuccess(List<Poi> pois) {
                // Traitez la liste des POIs ici
                maPoiList = pois;
            }

            @Override
            public void onError(Error error) {
                // Gérez les erreurs ici
                Log.e("POI", "Error while retrieving POIs : " + error.getMessage());
            }
        });

        //Mes variables
        Button btnShowPOIs = findViewById(R.id.btn_show_pois);
        btnShowPOIs.setOnClickListener(v -> showPOIDialog());

        //Pour le tts et le shake
        //Pour le tts
        // Initialiser le TextToSpeech
        tts = new TextToSpeech(this, this);

        // Initialiser le SensorManager et l'accéléromètre
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        lastUpdate = System.currentTimeMillis();
        lastShakeTime = 0; // initialiser la dernière secousse à 0
        dernierMessageAutomatique = 0;


    }

    //Mes fonctions pour le tts et le shake
    @Override
    public void onInit(int status) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean estFrancais = prefs.getBoolean("en", true);

        if (status == TextToSpeech.SUCCESS) {
            int result;
            if(estFrancais){
                result = tts.setLanguage(Locale.CANADA_FRENCH);
            }else{
                result = tts.setLanguage(Locale.ENGLISH);
            }
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The specified language is not supported !");
            }
        } else {
            Log.e("TTS", "TTS initialization failed !");
        }
    }

    private void speak(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void speakAfter(String text){
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Désenregistrer le listener pour les capteurs
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float[] values = event.values;
            float x = values[0];
            float y = values[1];
            float z = values[2];

            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastUpdate) > 200) {
                long diffTime = (currentTime - lastUpdate);
                lastUpdate = currentTime;

                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    if (currentTime - lastShakeTime > SHAKE_DELAY) {
                        speak(firstIndication);
                        lastShakeTime = currentTime; // mettre à jour le temps de la dernière secousse
                    }
                }

                last_x = x;
                last_y = y;
                last_z = z;
            }
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Rien à faire ici
    }

    private void showPOIDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);


        List<String> poiNames = new ArrayList<>();
        for (Poi poi : maPoiList) {
            poiNames.add(poi.getName());
        }

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, poiNames) {
            @NonNull
            @Override
            public View getView(int position,  View convertView, @NonNull ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    textView.setTextAppearance(R.style.POIListTextStyle); // Appliquer le style personnalisé
                }
                textView.setBackgroundResource(R.drawable.rounded_rectangle); // Appliquer le fond arrondi

                // Ajouter des marges autour de chaque élément
                ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                layoutParams.setMargins(0, 0, 0, 10); // Espacement à gauche, en haut, à droite, en bas
                textView.setLayoutParams(layoutParams);

                return textView;
            }
        };


        builder.setAdapter(adapter, (dialog, which) -> {
            selectedPoi = maPoiList.get(which);
            navigateToPoi(selectedPoi);
        });

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background); // Appliquer le fond arrondi
        dialog.show();

    }

    private void navigateToPoi(Poi poi) {
        LatLng latLng = new LatLng(poi.getCoordinate().getLatitude(), poi.getCoordinate().getLongitude());
        String floorIdentifier = poi.getFloorIdentifier();
        removePolylines();
        if (markerDestination != null) {
            markerDestination.remove();
        }

        //En gros le point to ici c'est lui qu'il fallait changer
        // la fonction createPointWithFloor c'est ma propre fonction j'ai ajouté en paramètre le floorIdentifier qu'on peut
        //obtenir sur le poi et juste en changeant ça la navigation se rend à la bonne place

        to = createPointWithFloor(latLng, floorIdentifier);
        if (current == null) {
            return;
        }
        navigation = true;
        getRoute();
        markerDestination = googleMap.addMarker(new MarkerOptions().position(latLng).title(poi.getName()));
    }

    private void checkLocationManager() {
        if (locationManager.isRunning()) {
            stopLocation();
            SitumSdk.locationManager().removeUpdates(locationListener);
        } else {
            startLocation();

        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: ");
        super.onResume();
        checkPermisions();

        // Enregistrer le listener pour les capteurs
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onDestroy(){
        Log.d(TAG, "onDestroy: ");
        SitumSdk.navigationManager().removeUpdates();
        SitumSdk.locationManager().removeUpdates(locationListener);
        stopLocation();
        super.onDestroy();
    }

    private Bitmap resizeBitmap(int drawableRes, int width, int height) {
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), drawableRes);
        return Bitmap.createScaledBitmap(imageBitmap, width, height, false);
    }

    private void startLocation(){
        if(locationManager.isRunning()){
            return;
        }
        final String[] floor_save = {floorId};
        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (prev != null) prev.remove();
                LatLng latLng = new LatLng(location.getCoordinate().getLatitude(),
                        location.getCoordinate().getLongitude());

                Bitmap resizedBitmap = resizeBitmap(R.drawable.position, 80, 80);
                BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(resizedBitmap);

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(latLng)
                        .icon(bitmapDescriptor)
                        .anchor(0.1f, 0.1f)
                        .zIndex(100);

                prev = googleMap.addMarker(markerOptions);
                current = location;
                floorId = current.getPosition().getFloorIdentifier();

                if (!isMapShow || !Objects.equals(floor_save[0], floorId)) {
                    drawMap(); // Recharger la carte
                    floor_save[0] = floorId;
                    isMapShow = true;
                }

                if(to != null){
                    if(navigation) {

                        SitumSdk.navigationManager().updateWithLocation(current);
                    }

                    navigationCreation();
                }
            }

            @Override
            public void onStatusChanged(@NonNull LocationStatus locationStatus) {
                Log.d(TAG, "onStatusChanged: " + locationStatus);
            }

            @Override
            public void onError(@NonNull Error error) {
                Log.e(TAG, "onError: " + error.getMessage());
            }
        };
        LocationRequest locationRequest = new LocationRequest.Builder()
                .buildingIdentifier(building.getIdentifier())
                .useWifi(true)
                .useBle(true)
                .useForegroundService(true)
                .build();
        SitumSdk.locationManager().requestLocationUpdates(locationRequest, locationListener);
    }

    private void stopLocation(){
        if (!locationManager.isRunning()){
            return;
        }
        locationManager.removeUpdates(locationListener);
        current = null;
        stopNavigation();
        if (prev != null)
            prev.remove();

        removePolylines();
    }

    private void removePolylines() {
        for (Polyline polyline : polylines) {
            polyline.remove();
        }
        polylines.clear();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        coordinateConverter = new CoordinateConverter(building.getDimensions(),building.getCenter(),building.getRotation());
        this.googleMap.getUiSettings().setMapToolbarEnabled(false);
        this.googleMap.setOnMapClickListener(latLng -> getPoint(googleMap, latLng));
        this.googleMap.setBuildingsEnabled(false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showMap = prefs.getBoolean("show_map", true);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (!showMap) {
            assert mapFragment != null;
            View mapView = mapFragment.getView();
            if (mapView != null) {
                mapView.setVisibility(View.GONE);
            }
        }

        this.googleMap.getUiSettings().setTiltGesturesEnabled(false);

        this.googleMap.getUiSettings().setMapToolbarEnabled(false);
        this.googleMap.setOnMapClickListener(latLng -> getPoint(googleMap, latLng));
    }

    private void getPoint(GoogleMap googleMap, LatLng latLng) {
        removePolylines();
        if (markerDestination != null) {
            markerDestination.remove();
        }
        to = createPoint(latLng);
        if (current == null) {
            return;
        }
        navigation = true;
        getRoute();
        markerDestination = googleMap.addMarker(new MarkerOptions().position(latLng).title("destination"));
    }

    void drawMap() {
        fetchCurrentFloorImage(building, new GuideInstructionsActivity.Callback() {
            @Override
            public void onSuccess(Bitmap floorImage) {

                drawBuilding(floorImage);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onError(Error error) {
                Toast.makeText(GuideInstructionsActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    void fetchCurrentFloorImage(Building building, GuideInstructionsActivity.Callback callback) {
        SitumSdk.communicationManager().fetchFloorsFromBuilding(building.getIdentifier(), new Handler<Collection<Floor>>() {
            @Override
            public void onSuccess(Collection<Floor> floorsCollection) {
                List<Floor> floors = new ArrayList<>(floorsCollection);
                Floor currentFloor = floors.get(0);
                for (Floor floor : floors) {
                    if (floor.getIdentifier().equals(current.getPosition().getFloorIdentifier())) {
                        currentFloor = floor;
                    }
                }
                floorId = currentFloor.getIdentifier();
                SitumSdk.communicationManager().fetchMapFromFloor(currentFloor, new Handler<Bitmap>() {
                    @Override
                    public void onSuccess(Bitmap bitmap) {
                        callback.onSuccess(bitmap);
                    }

                    @Override
                    public void onFailure(Error error) {
                        callback.onError(error);
                    }
                });
            }
            @Override
            public void onFailure(Error error) {
                callback.onError(error);
            }
        });
    }

    private Point createPoint(LatLng latLng) {
        Coordinate coordinate = new Coordinate(latLng.latitude, latLng.longitude);
        CartesianCoordinate cartesianCoordinate= coordinateConverter.toCartesianCoordinate(coordinate);
        return new Point(building.getIdentifier(), floorId,coordinate,cartesianCoordinate );
    }

    private Point createPointWithFloor(LatLng latLng, String floorIdentifier){
        Coordinate coordinate = new Coordinate(latLng.latitude, latLng.longitude);
        CartesianCoordinate cartesianCoordinate= coordinateConverter.toCartesianCoordinate(coordinate);
        return new Point(building.getIdentifier(), floorIdentifier,coordinate,cartesianCoordinate );
    }

    interface Callback {
        void onSuccess(Bitmap floorImage);

        void onError(Error error);
    }

    private void setup(){
        progressBar = findViewById(R.id.progressBar);
        navigationLayout = findViewById(R.id.navigation_layout);
        mNavText = findViewById(R.id.tv_indication);
    }

    void drawBuilding(Bitmap bitmap) {
        Bounds drawBounds = building.getBounds();
        Coordinate coordinateNE = drawBounds.getNorthEast();
        Coordinate coordinateSW = drawBounds.getSouthWest();
        LatLngBounds latLngBounds = new LatLngBounds(
                new LatLng(coordinateSW.getLatitude(), coordinateSW.getLongitude()),
                new LatLng(coordinateNE.getLatitude(), coordinateNE.getLongitude()));

        this.googleMap.addGroundOverlay(new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromBitmap(bitmap))
                .bearing((float) building.getRotation().degrees())
                .positionFromBounds(latLngBounds)
                .zIndex(1));

        this.googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100));
    }

    void getRoute(){
        // Récupérer les préférences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean UtiliserAcensceur = prefs.getBoolean("Use elevator", true);

        DirectionsRequest directionsRequest;
        if(UtiliserAcensceur){
            directionsRequest = new DirectionsRequest.Builder()
                    .from(current.getPosition(), null)
                    .to(to)
                    .accessibilityMode(DirectionsRequest.AccessibilityMode.ONLY_ACCESSIBLE)
                    .build();
        }else{
            directionsRequest = new DirectionsRequest.Builder()
                    .from(current.getPosition(), null)
                    .to(to)
                    .accessibilityMode(DirectionsRequest.AccessibilityMode.ONLY_NOT_ACCESSIBLE_FLOOR_CHANGES)
                    .build();
        }

        SitumSdk.directionsManager().requestDirections(directionsRequest, new Handler<Route>() {
            @Override
            public void onSuccess(Route route) {
                removePolylines();
                drawRoute(route);
                centerCamera(route);
                liste_indications = route.getIndications();

                navigationRequest = new NavigationRequest.Builder()
                        .route(route)
                        .distanceToGoalThreshold(3d)
                        .distanceToChangeFloorThreshold(2d)
                        .outsideRouteThreshold(5d)
                        .build();

                startNavigation();
                premiereIndicationDonnee = false;
                estArrive = false;
                sortiChemin = false;
            }
            @Override
            public void onFailure(Error error) {

            }
        });
    }

    private void drawRoute(Route route) {

        for (RouteSegment segment : route.getSegments()) {
            //For each segment you must draw a polyline
            //Add an if to filter and draw only the current selected floor
            if (Objects.equals(segment.getFloorIdentifier(), floorId)) {
                List<LatLng> latLngs = new ArrayList<>();
                for (Point point : segment.getPoints()) {
                    latLngs.add(new LatLng(point.getCoordinate().getLatitude(), point.getCoordinate().getLongitude()));
                }


                PolylineOptions polyLineOptions = new PolylineOptions()
                        .color(Color.RED)
                        .width(30f)
                        .zIndex(3)
                        .addAll(latLngs);
                Polyline polyline = googleMap.addPolyline(polyLineOptions);
                polylines.add(polyline);
            }
        }
    }

    private void centerCamera(Route route) {
        Coordinate from = route.getFrom().getCoordinate();
        Coordinate to = route.getTo().getCoordinate();

        LatLngBounds.Builder builder = new LatLngBounds.Builder()
                .include(new LatLng(from.getLatitude(), from.getLongitude()))
                .include(new LatLng(to.getLatitude(), to.getLongitude()));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
    }

    void navigationCreation(){
        navigationLayout.setVisibility(View.VISIBLE);
    }

    void startNavigation(){

        // Récupérer les préférences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean Indications_Automatiques = prefs.getBoolean("Automatic Directions", true);
        boolean estFrancais = prefs.getBoolean("fr_en", true);
        Log.d(TAG, "startNavigation: ");
        SitumSdk.navigationManager().requestNavigationUpdates(navigationRequest, new NavigationListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDestinationReached() {
                Log.d(TAG, "onDestinationReached: ");
                if(estFrancais){
                    mNavText.setText("Arrived at destination");
                }else{
                    mNavText.setText("Arrived at destination");
                }

                removePolylines();
                if(!estArrive){
                    if(estFrancais){
                        speak("You have arrived");
                    }else{
                        speak("You have arrived");
                    }

                    estArrive = true;
                }
                if(estFrancais){
                    firstIndication = "You are already arrived";
                }else{
                    firstIndication = "You are already arrived";
                }

            }

            @Override
            public void onProgress(NavigationProgress navigationProgress) {
                Context context = getApplicationContext();

                Log.d(TAG, "onProgress: " + navigationProgress.getCurrentIndication().toText(context));
                if(estFrancais){
                    mNavText.setText(ConvertIndicationToText(navigationProgress.getCurrentIndication()));
                }else{
                    mNavText.setText(ConvertIndicationToTextEnglish(navigationProgress.getCurrentIndication()));
                }

                //firstIndication = navigationProgress.getCurrentIndication().toText(context);
                if(premiereIndicationDonnee){
                    //Si l'indication actuelle a changé, on la parle à voix haute
                    Indication nouvelle_indication = navigationProgress.getCurrentIndication();

                    if(Indications_Automatiques && System.currentTimeMillis() - dernierMessageAutomatique > DELAI_AUTOMATIQUE && DoitDireIndication(nouvelle_indication, indicationActuelle)){
                        dernierMessageAutomatique = System.currentTimeMillis();
                        indicationActuelle = navigationProgress.getCurrentIndication();
                        if(estFrancais){
                            firstIndication = ConvertIndicationToText(indicationActuelle);
                            speak(firstIndication);
                        }else{
                            firstIndication = ConvertIndicationToTextEnglish(indicationActuelle);
                            speak(firstIndication);
                        }
                    }

                    indicationActuelle = navigationProgress.getCurrentIndication();
                    if(estFrancais){
                        firstIndication = ConvertIndicationToText(indicationActuelle);
                    }else{
                        firstIndication = ConvertIndicationToTextEnglish(indicationActuelle);
                    }
                }else{
                    indicationActuelle = navigationProgress.getCurrentIndication();
                    if(estFrancais){
                        speakAfter("Starting navigation : ");
                        firstIndication = ConvertIndicationToText(indicationActuelle);
                        speakAfter(firstIndication);
                    }else{
                        speakAfter("Starting navigation : ");
                        firstIndication = ConvertIndicationToTextEnglish(indicationActuelle);
                        speakAfter(firstIndication);
                    }
                    premiereIndicationDonnee = true;
                }

                DirectionsRequest directionsRequest = new DirectionsRequest.Builder()
                        .from(current.getPosition(), null)
                        .to(to)
                        .build();

                SitumSdk.directionsManager().requestDirections(directionsRequest, new Handler<Route>() {
                    @Override
                    public void onSuccess(Route route) {
                        removePolylines();
                        drawRoute(route);

                    }
                    @Override
                    public void onFailure(Error error) {

                    }
                });
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onUserOutsideRoute() {
                Log.d(TAG, "onUserOutsideRoute: ");
                if(estFrancais){
                    mNavText.setText("Outside of the route");
                }else{
                    mNavText.setText("Outside of the route");
                }
                if(!sortiChemin){
                    if(estFrancais){
                        speak("You have left the route, recalculating the route ");
                    }else{
                        speak("You have left the route, recalculating the route ");
                    }
                    sortiChemin = true;
                }

                getRoute();

//                DirectionsRequest directionsRequest = new DirectionsRequest.Builder()
//                        .from(current.getPosition(), null)
//                        .to(to)
//                        .build();
//
//                SitumSdk.directionsManager().requestDirections(directionsRequest, new Handler<Route>() {
//                    @Override
//                    public void onSuccess(Route route) {
//                        removePolylines();
//                        drawRoute(route);
//
//                        Indication direction = route.getIndications().get(0);
//
//                        if(direction != indicationActuelle) {
//                            indicationActuelle = direction;
//                            speakAfter(ConvertIndicationToText(direction));
//                            mNavText.setText(ConvertIndicationToText(direction));
//
//                        }
//
//
//
//
//                    }
//                    @Override
//                    public void onFailure(Error error) {
//
//                    }
//                });



            }
        });
    }

    private boolean DoitDireIndication(Indication nouvelleIndication, Indication indicationActuelle) {
        Indication.Action nouvelleAction = nouvelleIndication.getIndicationType();
        return nouvelleAction != Indication.Action.GO_AHEAD && nouvelleAction != indicationActuelle.getIndicationType();
    }

    private String ConvertIndicationToText(Indication indication){
        String message = "message invalide";
        Indication.Action action = indication.getIndicationType();
        int distance = (int)Math.round(indication.getDistance());
        if(action == Indication.Action.GO_AHEAD){
            message = "Move forward to" + distance + " meters";
        } else if (action == Indication.Action.TURN) {
            double angle = Math.abs(indication.getOrientation());
            Indication.Orientation orientation = indication.getOrientationType();
            boolean estGauche = orientation == Indication.Orientation.LEFT || orientation == Indication.Orientation.SHARP_LEFT;
            if(angle > 3 * Math.PI / 4){
                message = "Make a U-turn, then move forward to " + distance + " meters";
            } else if (angle > Math.PI/3) {
                if(estGauche){
                    message = "Turn left, then move forward to " + distance + " meters";
                }else{
                    message = "Turn right, then move forward to " + distance + " meters";
                }
            }else{
                if(estGauche){
                    message = "Slightly turn left, then move forward to " + distance + " meters";
                }else{
                    message = "Slightly turn right, then move forward to " + distance + " meters";
                }
            }
        } else if (action == Indication.Action.CHANGE_FLOOR) {
            Indication.Orientation orientation = indication.getOrientationType();
            StringBuilder sb = new StringBuilder();
            if(indication.getDistanceToNextLevel() > 0){
                sb.append("Go up");
            }else{
                sb.append("Go down");
            }
            sb.append("upstairs").append(indication.getNextLevel()).append("using the elevator or the stairs located ");
            if(orientation == Indication.Orientation.LEFT || orientation == Indication.Orientation.SHARP_LEFT){
                sb.append("to your left");
            } else if (orientation == Indication.Orientation.RIGHT || orientation == Indication.Orientation.SHARP_RIGHT) {
                sb.append("to your right");
            } else if (orientation == Indication.Orientation.STRAIGHT) {
                sb.append("straight ahead");
            }else{
                sb.append("behind you");
            }
            sb.append("at a distance of").append(distance).append(" meters");
            message = sb.toString();
        }
        return message;
    }
    private String ConvertIndicationToTextEnglish(Indication indication){
        String message = "invalid message";
        Indication.Action action = indication.getIndicationType();
        int distance = (int)Math.round(indication.getDistance());
        if(action == Indication.Action.GO_AHEAD){
            message = "Go ahead for " + distance + " meters";
        } else if (action == Indication.Action.TURN) {
            double angle = Math.abs(indication.getOrientation());
            Indication.Orientation orientation = indication.getOrientationType();
            boolean estGauche = orientation == Indication.Orientation.LEFT || orientation == Indication.Orientation.SHARP_LEFT;
            if(angle > 3 * Math.PI / 4){
                message = "Turn around, and advance for " + distance + " meters";
            } else if (angle > Math.PI/3) {
                if(estGauche){
                    message = "Turn left, and advance for " + distance + " meters";
                }else{
                    message = "Turn right, and advance for " + distance + " meters";
                }
            }else{
                if(estGauche){
                    message = "Turn slightly left, and advance for " + distance + " meters";
                }else{
                    message = "Turn slightly right, and advance for " + distance + " meters";
                }
            }
        } else if (action == Indication.Action.CHANGE_FLOOR) {
            Indication.Orientation orientation = indication.getOrientationType();
            StringBuilder sb = new StringBuilder();
            if(indication.getDistanceToNextLevel() > 0){
                sb.append("Go up");
            }else{
                sb.append("Go down");
            }
            sb.append(" to floor ").append(indication.getNextLevel()).append(" using the stairs or the elevator that is ");
            if(orientation == Indication.Orientation.LEFT || orientation == Indication.Orientation.SHARP_LEFT){
                sb.append("on your left");
            } else if (orientation == Indication.Orientation.RIGHT || orientation == Indication.Orientation.SHARP_RIGHT) {
                sb.append("on your right");
            } else if (orientation == Indication.Orientation.STRAIGHT) {
                sb.append("straight ahead");
            }else{
                sb.append("behind you");
            }
            sb.append(" at a distance of ").append(distance).append(" meters");
            message = sb.toString();
        }
        return message;
    }

    @SuppressLint("SetTextI18n")
    void stopNavigation(){
        removePolylines();
        to = null;
        navigationRequest = null;
        navigationLayout.setVisibility(View.GONE);
        navigation = false;
        mNavText.setText("Navigation");
    }

}