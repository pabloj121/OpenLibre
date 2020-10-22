package de.dorianscholz.openlibre.ui;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import de.dorianscholz.openlibre.R;
import de.dorianscholz.openlibre.model.GlucoseData;
import de.dorianscholz.openlibre.model.ReadingData;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.Sort;

import static de.dorianscholz.openlibre.OpenLibre.realmConfigProcessedData;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ReportFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReportFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    // private String mParam1;
    // private String mParam2;


    LineGraphSeries<DataPoint> data;
    GraphView graph;
    // Button last_day, last_week;
    private Realm mRealmProcessedData;
    long last_day_period, last_week_period = -1;
    int number_points, current_position;
    double x,y;

    public ReportFragment() {
        data = new LineGraphSeries<DataPoint>();
        mRealmProcessedData = Realm.getInstance(realmConfigProcessedData);
        number_points = current_position = 0;
        x = y = 0.0;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ReportFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ReportFragment newInstance(){ //(String param1, String param2) {
        ReportFragment fragment = new ReportFragment();
        Bundle args = new Bundle();
        // args.putString(ARG_PARAM1, param1);
        // args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_main);
        /*
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        */

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // System.out.println("hola");
        View view = inflater.inflate(R.layout.fragment_report, null);

        final Button last_day = (Button) view.findViewById(R.id.lastday);
        final Button last_week = (Button) view.findViewById(R.id.lastweek);
        graph = (GraphView) view.findViewById(R.id.graph);

        // Graph listeners
        last_day.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lastDay();
                //lastPeriod(true);
            }
        });

        last_week.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lastWeek();
                //lastPeriod(false);
            }
        });

        // System.out.println("hola2");
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_report, view, false);
        return inflater.inflate(R.layout.fragment_report, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        // set visibility ON
    }

    @Override
    public void onStop() {
        super.onStop();
        // set visibility OFF
    }

    @Override
    public void onPause() {
        super.onPause();
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

        if (day) {
            last_day_period = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
        } else{
            last_day_period = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
        }

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
}