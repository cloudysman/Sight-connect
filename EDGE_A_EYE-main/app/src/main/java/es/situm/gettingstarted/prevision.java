package es.situm.gettingstarted;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;


//import com.google.ai.client.generativeai.GenerativeModel;
//import com.google.ai.client.generativeai.java.GenerativeModelFutures;
//import com.google.ai.client.generativeai.type.Content;
//import com.google.ai.client.generativeai.type.GenerateContentResponse;
//import com.google.android.gms.maps.model.LatLng;
//import com.google.common.util.concurrent.FutureCallback;
//import com.google.common.util.concurrent.ListenableFuture;
////import com.google.common.util.concurrent.FutureCallback;
//import com.google.common.util.concurrent.Futures;
//import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import es.situm.gettingstarted.guideinstructions.GuideInstructionsActivity;
import es.situm.sdk.SitumSdk;
import es.situm.sdk.directions.DirectionsRequest;
import es.situm.sdk.error.Error;
import es.situm.sdk.location.util.CoordinateConverter;
import es.situm.sdk.model.cartography.Building;
import es.situm.sdk.model.cartography.Poi;
import es.situm.sdk.model.cartography.Point;
import es.situm.sdk.model.directions.Indication;
import es.situm.sdk.model.directions.Route;
import es.situm.sdk.model.location.CartesianCoordinate;
import es.situm.sdk.model.location.Coordinate;
import es.situm.sdk.navigation.NavigationRequest;
import es.situm.sdk.utils.Handler;

import es.situm.gettingstarted.common.SampleActivity;

public class prevision extends AppCompatActivity implements TextToSpeech.OnInitListener{
    private int numero_entree = 1;//pas vraiment utile en fait
    private List<Poi> maPoiList = new ArrayList<>();
    //    private Building building;
    private Building building;
    private String buildingID = "16781";
    //    building = getBuildingFromIntent();
    private CoordinateConverter coordinateConverter;

    private List<Indication> liste_indications;

    private TextToSpeech tts;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prevision);

        building = getBuildingFromIntent();

        tts = new TextToSpeech(this, this);

        coordinateConverter = new CoordinateConverter(building.getDimensions(),building.getCenter(),building.getRotation());

        Button btnShowPOIs = findViewById(R.id.btn_show_pois);
        btnShowPOIs.setOnClickListener(v -> showPOIDialog());

        SitumSdk.communicationManager().fetchIndoorPOIsFromBuilding(building, new Handler<Collection<Poi>>() {
            @Override
            public void onSuccess(Collection<Poi> pois) {
                maPoiList = new ArrayList<>(pois);
            }
            @Override
            public void onFailure(Error error) {
            }
        });
    }
    //Mes fonctions pour le tts et le shake
    @Override
    public void onInit(int status) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean estFrancais = prefs.getBoolean("fr_en", true);

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

    private StringBuilder sb = new StringBuilder();
    private void speakAfter(String text){
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, null);
        sb.append(text);
    }


    private void showPOIDialog() {
        if (maPoiList.isEmpty()) {
            Toast.makeText(this, "No POIs available", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a POI");

        List<String> poiNames = new ArrayList<>();
        for (Poi poi : maPoiList) {
            poiNames.add(poi.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, poiNames);
        builder.setAdapter(adapter, (dialog, which) -> {
            Poi selectedPoi = maPoiList.get(which);
            getDirection(selectedPoi);
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private Point createPointWithFloor(Poi poi){
        Coordinate coordinate = poi.getCoordinate();
        CartesianCoordinate cartesianCoordinate= coordinateConverter.toCartesianCoordinate(coordinate);
        return new Point(building.getIdentifier(), poi.getFloorIdentifier(),coordinate, cartesianCoordinate );
    }

    void getDirection(Poi poi){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean UtiliserAcensceur = prefs.getBoolean("Use elevator", true);

        //Cette initialisation est supposée toujours être override, mais je dois mettre une valeur initiale pour éviter les erreurs
        Poi depart = maPoiList.get(numero_entree);
        //Si le nom du poi entrée change, ça ne fonctionnera plus
        for(Poi monPoi:maPoiList){
            if(monPoi.getName().equals("entry")){
                depart = monPoi;
            }
        }

        DirectionsRequest directionsRequest;
        if(UtiliserAcensceur){
            directionsRequest = new DirectionsRequest.Builder()
                    .from(createPointWithFloor(depart), null)
                    .to(createPointWithFloor(poi))
                    .accessibilityMode(DirectionsRequest.AccessibilityMode.ONLY_ACCESSIBLE)
                    .build();
        }else{
            directionsRequest = new DirectionsRequest.Builder()
                    .from(createPointWithFloor(depart), null)
                    .to(createPointWithFloor(poi))
                    .accessibilityMode(DirectionsRequest.AccessibilityMode.ONLY_NOT_ACCESSIBLE_FLOOR_CHANGES)
                    .build();
        }

        SitumSdk.directionsManager().requestDirections(directionsRequest, new Handler<Route>() {
            @Override
            public void onSuccess(Route route) {
                liste_indications = route.getIndications();
                DecrireTrajet(liste_indications);
            }
            @Override
            public void onFailure(Error error) {

            }
        });
    }

    private void DecrireTrajet(List<Indication> listeIndications) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean estFrancais = prefs.getBoolean("en", true);

        if(estFrancais){
            speakAfter("Here is the route you will have to take : ");
        }else{
            speakAfter("Here is the route you will have to take : ");
        }


        speakAfter(ConvertIndicationToText(listeIndications.get(0)));
        for(int i = 1; i < listeIndications.size(); i++){

            if(estFrancais){
                speakAfter(", then, ");
                speakAfter(ConvertIndicationToText(listeIndications.get(i)));
            }else{
                speakAfter(", then, ");
                speakAfter(ConvertIndicationToTextEnglish(listeIndications.get(i)));
            }


        }




        //ça serait ici pour passer le text dans le LLM


        LogIndication(sb.toString());
    }

    private void LogIndication(String s){
        Log.i(GuideInstructionsActivity.class.getSimpleName(), s);
    }


    private String ConvertIndicationToText(Indication indication){
        String message = "message invalide";
        Indication.Action action = indication.getIndicationType();
        int distance = (int)Math.round(indication.getDistance());
        if(action == Indication.Action.GO_AHEAD){
            message = "Move forward to " + distance + " meters";
        } else if (action == Indication.Action.TURN) {
            double angle = Math.abs(indication.getOrientation());
            Indication.Orientation orientation = indication.getOrientationType();
            boolean estGauche = orientation == Indication.Orientation.LEFT || orientation == Indication.Orientation.SHARP_LEFT;
            if(angle > 3 * Math.PI / 4){
                message = "Make a U-turn, then move forward by " + distance + " meters";
            } else if (angle > Math.PI/3) {
                if(estGauche){
                    message = "Turn left, then move forward by " + distance + " meters";
                }else{
                    message = "Turn left, then move forward by " + distance + " meters";
                }
            }else{
                if(estGauche){
                    message = "Turn left, then move forward by " + distance + " meters";
                }else{
                    message = "Turn left, then move forward by " + distance + " meters";
                }
            }
        } else if (action == Indication.Action.CHANGE_FLOOR) {
            message = "Go up to the 3rd floor using the elevator or the stairs located on the right wall";
        }
        return message;
    }


    public static final String EXTRA_BUILDING = "EXTRA_BUILDING";

    protected Building getBuildingFromIntent() {
        return (Building) getIntent().getParcelableExtra(EXTRA_BUILDING);
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
        }  else if (action == Indication.Action.CHANGE_FLOOR) {
            message = "Go up to floor 3 using the elevator or the stairs on the right wall";
        }
        return message;
    }
}

