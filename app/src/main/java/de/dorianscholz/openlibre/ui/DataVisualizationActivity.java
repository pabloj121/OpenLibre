package de.dorianscholz.openlibre.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import de.dorianscholz.openlibre.R;
import de.dorianscholz.openlibre.model.GlucoseData;
import de.dorianscholz.openlibre.model.ReadingData;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.Sort;

import static de.dorianscholz.openlibre.OpenLibre.realmConfigProcessedData;
import static de.dorianscholz.openlibre.OpenLibre.GLUCOSE_TARGET_MAX;
import static de.dorianscholz.openlibre.OpenLibre.GLUCOSE_TARGET_MIN;


public class DataVisualizationActivity extends AppCompatActivity {

    private LineGraphSeries<DataPoint> data;
    private GraphView graph;
    private PieChart piechart;
    private BarGraphSeries<DataPoint> series;

    private Realm mRealmProcessedData;
    private long last_day_period, last_week_period = -1;
    private int number_points, current_position;
    private double x, y;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_visualization);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mRealmProcessedData = Realm.getInstance(realmConfigProcessedData);
        data = new LineGraphSeries<DataPoint>();

        final Button last_day = (Button) findViewById(R.id.lastday);
        final Button last_week = (Button) findViewById(R.id.lastweek);
        final Button finish_activity = (Button) findViewById(R.id.home);
        graph = (GraphView) findViewById(R.id.graph);
        piechart = (PieChart) findViewById(R.id.piechart);

        // Graph listeners
        last_day.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lastDay();
                lastPeriod(true);
            }
        });

        last_week.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //lastWeek();
                lastPeriod(false);
            }
        });

        finish_activity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishActivity(view);
            }
        });


    }
    @Override
    public void onBackPressed() {
        super.onBackPressed(); // por defecto si escribes aquí el super el botón hará lo que debía hacer si lo quitas ya no hará lo que debía de hacer y puedes programar otros comportamientos.
        //Quita el super y has un finish() a la actividad o bien replanteate bien lo que quieres hacer cuando se presione hacia atrás.
        // finish();
    }

    public void finishActivity(View view) {
        finish();
    }

    public void lastDay(){
        last_day_period = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);

        List<ReadingData> history = mRealmProcessedData.where(ReadingData.class)
                .greaterThan("timezoneOffsetInMinutes", last_day_period)
                .findAllSorted(ReadingData.DATE, Sort.ASCENDING);

        // Y si la busqueda no me devuelve ninguna fila ?!

        // Do a count of points
        for (ReadingData read: history) {
            number_points += read.getHistory().size();
        }

        for (ReadingData read: history) {
            RealmList<GlucoseData> controls = read.getHistory();

            for (GlucoseData glucose: controls) {
                x = glucose.glucose();
                y = current_position / number_points;
                data.appendData(new DataPoint(x,y), true, number_points);
                ++current_position; // check !
            }
        }
        graph.addSeries(data);
    }

    public void lastWeek(){
        last_week_period = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);

        List<ReadingData> history = mRealmProcessedData.where(ReadingData.class)
                .greaterThan("timezoneOffsetInMinutes", last_week_period)
                .findAllSorted(ReadingData.DATE, Sort.ASCENDING);

        for (ReadingData read: history) {
            number_points += read.getHistory().size();
        }

        for (ReadingData read: history) {
            RealmList<GlucoseData> controls = read.getHistory();

            for (GlucoseData glucose: controls) {
                x = glucose.glucose();
                y = current_position / number_points;
                data.appendData(new DataPoint(x,y), true, number_points);
                ++current_position; // check !
            }
        }
        graph.addSeries(data);
    }

    // To plot the last day controls, day would be true
    // and false to plot the last week controls
    public void lastPeriod(boolean day){
        piechart.setVisibility(View.GONE);
        graph.setVisibility(View.VISIBLE);

        graph.removeAllSeries(); // clear the data

        if (day) {
            last_day_period = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
        } else{
            last_day_period = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
        }

        List<ReadingData> history = mRealmProcessedData.where(ReadingData.class)
                .greaterThan("timezoneOffsetInMinutes", last_day_period)
                .findAllSorted(ReadingData.DATE, Sort.ASCENDING);

        if (history.size() == 0){
            Toast.makeText(this, "No data available", Toast.LENGTH_SHORT).show();
        } else{
            // Do a count of points
            for (ReadingData read: history) {
                number_points += read.getHistory().size();
            }

            for (ReadingData read: history) {
                RealmList<GlucoseData> controls = read.getHistory();

                for (GlucoseData glucose: controls) {
                    x = glucose.glucose();
                    y = current_position / number_points;
                    data.appendData(new DataPoint(x,y), true, number_points);
                    ++current_position; // check !
                }
            }
            graph.addSeries(data);
        }

    }

    // hemoglobina glicosilada media
    public void targetPeriod(){
        graph.setVisibility(View.GONE);
        piechart.setVisibility(View.VISIBLE);

        int in_target = 0, greater_than_target = 0, lower_than_target = 0;

        List<ReadingData> history = mRealmProcessedData.where(ReadingData.class)
                .findAllSorted(ReadingData.DATE, Sort.ASCENDING);

        for (ReadingData read: history) {
            RealmList<GlucoseData> controls = read.getHistory();

            for (GlucoseData glucose: controls) {
                if (glucose.glucose() < GLUCOSE_TARGET_MIN){
                    ++lower_than_target;
                } else if (glucose.glucose() < GLUCOSE_TARGET_MAX) {
                    ++in_target;
                } else{
                    ++greater_than_target;
                }

            }
        }

        int total_controls = lower_than_target + in_target + greater_than_target;

        ArrayList<PieEntry> targets = new ArrayList<>();
        targets.add(new PieEntry(lower_than_target, "Lower than target"));
        targets.add(new PieEntry(in_target, "In target"));
        targets.add(new PieEntry(greater_than_target, "Greater than target"));

        PieDataSet pieDataSet = new PieDataSet(targets, "Targets");
        pieDataSet.setColors(ColorTemplate.PASTEL_COLORS);
        pieDataSet.setValueTextColor(Color.BLACK);
        pieDataSet.setValueTextSize(16f);

        PieData pieData = new PieData(pieDataSet);

        piechart.setData(pieData);
        // piechart.getDescription().setEnabled(false);
        piechart.setCenterText("Target period");
        piechart.animate();
    }
}