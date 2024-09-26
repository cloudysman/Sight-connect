package es.situm.gettingstarted.samplelist;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import es.situm.gettingstarted.R;
import es.situm.gettingstarted.drawbuilding.DrawBuildingActivity;
import es.situm.gettingstarted.drawpois.DrawPoisActivity;
import es.situm.gettingstarted.drawposition.DrawPositionActivity;
import es.situm.gettingstarted.drawroute.DrawRouteActivity;
import es.situm.gettingstarted.drawroutegeojson.DrawRouteGeojsonActivity;
import es.situm.gettingstarted.fetchresources.FetchResourcesActivity;
import es.situm.gettingstarted.guideinstructions.GuideInstructionsActivity;
import es.situm.gettingstarted.indooroutdoor.IndoorOutdoorActivity;
import es.situm.gettingstarted.poifiltering.ListBuildingsActivity;
import es.situm.gettingstarted.pointinsidegeofence.PointInsideGeofenceActivity;
import es.situm.gettingstarted.positioning.PositioningActivity;
import es.situm.gettingstarted.prevision;
import es.situm.gettingstarted.realtime.RealTimeActivity;
import es.situm.gettingstarted.updatelocationparams.UpdateLocationParamsActivity;
import es.situm.gettingstarted.usewayfinding.WayfindingActivity;
import es.situm.gettingstarted.SettingsActivity;

public class SamplesActivity
        extends AppCompatActivity
        implements SamplesAdapter.OnSampleSelectedCallback {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_samples);

        List<Sample> items = new ArrayList<>();

        items.add(new Sample("Real-time itinerary", es.situm.gettingstarted.common.selectbuilding.SelectBuildingActivity.createIntent(this, GuideInstructionsActivity.class)));

        items.add(new Sample("Plan your itinerary", es.situm.gettingstarted.common.selectbuilding.SelectBuildingActivity.createIntent(this, prevision.class)));

        items.add(new Sample("Settings", new Intent(this, SettingsActivity.class)));

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(SamplesActivity.this));
        recyclerView.setAdapter(new SamplesAdapter(items, this));
    }

    @Override
    public void onSampleSelected(Sample sample) {
        startActivity(sample.getIntent());
    }

}
