package de.dorianscholz.openlibre.ui

import android.content.Intent
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import de.dorianscholz.openlibre.OpenLibre
import de.dorianscholz.openlibre.R
import de.dorianscholz.openlibre.model.GlucoseData
import de.dorianscholz.openlibre.model.ReadingData
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import kotlinx.android.synthetic.main.fragment_agenda.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * A simple [Fragment] subclass.
 * Use the [GlucoseFormFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class GlucoseFormFragment(saved_data: ArrayList<Pair<String, Long>>, last_data:Long) : DialogFragment() {
    // Visualization components
    private lateinit var rg_food_type:RadioGroup
    private lateinit var rg_food_time:RadioGroup
    private lateinit var radio_button_food_type:RadioButton
    private lateinit var radio_button_food_time:RadioButton
    private lateinit var low_insulin:EditText
    private lateinit var fast_insulin:EditText
    private lateinit var sport:Switch
    private lateinit var start_time:EditText
    private lateinit var end_time:EditText
    private lateinit var add_information_button:Button
    private var start_time_millis:Long = 0
    private var end_time_millis:Long = 0
    private lateinit var stress:Switch
    private var latest_data_processed:Long = last_data
    private var saved:ArrayList<Pair<String, Long>> = saved_data
    private var mRealmProcessedData:Realm = Realm.getInstance(OpenLibre.realmConfigProcessedData)

    // Information components
    private var low_insulin_units:Int = 0
    private var fast_insulin_units:Int = 0
    private var food_time:Int = 0
    private var food_type:Int = 0
    private var sport_check:Boolean = false
    private var stress_check:Boolean = false

    fun GlucoseFormFragment() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }

        //mRealmProcessedData = Realm.getInstance(OpenLibre.realmConfigProcessedData)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_glucose_form, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        add_information_button = view.findViewById(R.id.glucose_addition_information)
        rg_food_time = view.findViewById(R.id.food_time_schedule)
        rg_food_type = view.findViewById(R.id.food_type)

        low_insulin = view.findViewById(R.id.low_insulin)
        fast_insulin = view.findViewById(R.id.fast_insulin)
        sport = view.findViewById(R.id.sport)
        start_time = view.findViewById(R.id.start_time)
        end_time = view.findViewById(R.id.end_time)
        stress = view.findViewById(R.id.stress)

        add_information_button.setOnClickListener(View.OnClickListener {
            val currentTime = System.currentTimeMillis()

            // Data treatment and pass information to the Main Activity
            dataTreatment()

            val intent = Intent(context, MainActivity::class.java)
            // FAST_INSULIN_UNITS, LOW_INSULIN_UNITS, END_SPORT, LOW_INSULIN_TIME, FAST_INSULIN_TIME, STRESS
            intent.putExtra("FAST_INSULIN_UNITS", fast_insulin_units)
            intent.putExtra("LOW_INSULIN_UNITS", low_insulin_units)
            intent.putExtra("END_SPORT", end_time_millis)
            intent.putExtra("LOW_INSULIN_TIME", currentTime + TimeUnit.HOURS.toMillis(4))
            intent.putExtra("FAST_INSULIN_TIME", currentTime + TimeUnit.HOURS.toMillis(24))
            intent.putExtra("STRESS", currentTime)
            startActivity(intent)
        })


        rg_food_time.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.ftime1 -> {
                    food_time = 0
                }
                R.id.ftime2 -> {
                    food_time = 1
                }
                R.id.ftime3 -> {
                    food_time = 2
                }
            }
        })

        rg_food_type.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.ftype1 -> {
                    food_type = 0
                }
                R.id.ftype2 -> {
                    food_type = 1
                }
                R.id.ftype3 -> {
                    food_type = 2
                }
            }
        })

    }

    /*
    * Save the data introduced by the user into the fields of the class in order to
    * be used in the next step ( data treating and modification of the database).
    * It's not necessary to fill all the fields.
    */

    fun dataGathering(){
        // Collenting the data
        low_insulin_units = Integer.parseInt(fast_insulin.text.toString())
        fast_insulin_units = Integer.parseInt(low_insulin.text.toString())

        if (sport.isChecked){
            start_time.visibility = View.VISIBLE
            end_time.visibility = View.VISIBLE

            sport_check = true

            // Recogida de tiempo
            var str: String = start_time.getText().toString()
            val formatter: DateFormat = SimpleDateFormat("hh:mm:ss a")
            var date = formatter.parse(str)

            start_time_millis = date.getTime()

            str = end_time.getText().toString()
            date = formatter.parse(str)
            end_time_millis = date.getTime()
        }

        if (stress.isChecked){
            stress_check = true
        }
    }


    /*
    * The most important function of the app.
    *
    * Define the risk taking account the following three scans after the current.
    *
    * Also define the trend and odify the database in order to write the extra data
    * (insulin, sport, stress) obtained from the user.
    *
     */

    fun dataTreatment() {

        dataGathering()

        val history: RealmResults<GlucoseData>
        val data_processed = ReadingData()
        val realmProcessedData = Realm.getInstance(OpenLibre.realmConfigProcessedData)
        realmProcessedData.beginTransaction()

        // Maximum difference between one scan and the next
        val max_difference: Long = latest_data_processed + TimeUnit.MINUTES.toMillis(10)
        var time_difference = TimeUnit.MINUTES.toMillis(11)
        var iterator = 0
        val next_glucose = ArrayList<Float>()
        history = mRealmProcessedData.where(GlucoseData::class.java)
                .greaterThan("timezoneOffsetInMinutes", latest_data_processed - time_difference)
                .findAllSorted(GlucoseData.DATE, Sort.ASCENDING)
        var j = 0
        iterator = 0

        while (iterator < history.size - 3) {
            next_glucose.add(history[iterator + 3].glucose())
            next_glucose.add(history[iterator + 2].glucose())
            next_glucose.add(history[iterator + 1].glucose())


            // Check the following three scans to determine the risk
            j = iterator
            while (j < iterator + 3) {
                var index = 3
                val difference = history[j + index].timezoneOffsetInMinutes -
                        history[iterator].timezoneOffsetInMinutes

                // Check if the following scans are temporarily next
                if (difference < max_difference) {
                    if (history[j + index].glucose() > OpenLibre.GLUCOSE_TARGET_MAX) {          // above the maximum target
                        history[j + index].risk = 1
                    } else if (history[j + index].glucose() < OpenLibre.GLUCOSE_TARGET_MIN) {   // under the minimum target
                        history[j + index].risk = -1
                    } else {         // within the targets
                        history[j + index].risk = 0
                    }
                }
                --index
                ++j
            }

            // Check the previous scans to define the trend
            if (iterator > 1) { // 2 or greater
                time_difference = history[iterator].timezoneOffsetInMinutes -
                        history[iterator - 2].timezoneOffsetInMinutes.toLong()
                if (time_difference < max_difference) {
                    val glucose_difference = history[iterator].glucose() -
                            history[iterator - 2].glucose()
                    if (glucose_difference > 15) {           // descendent trend
                        history[iterator].trend = 0
                    } else if (glucose_difference < 15) {    // ascendent trend
                        history[iterator].trend = 2
                    } else {                                 // not trend
                        history[iterator].trend = 1
                    }
                }
            }


            // FAST_INSULIN_UNITS, LOW_INSULIN_UNITS, END_SPORT, LOW_INSULIN_TIME, FAST_INSULIN_TIME, STRESS
            for (par in saved) {
                var low_units = 0.toLong()
                var fast_units = 0.toLong()

                // We are going to comparise only times
                if (par.first != "LOW_INSULIN_UNITS" && par.first != "FAST_INSULIN_UNITS" &&
                        par.second < history[iterator].timezoneOffsetInMinutes) {
                    val first = par.first
                    saved.remove(par)
                    saved.add(Pair<String, Long>(first, 0.toLong()))
                } else {
                    when (par.first) {
                        "LOW_INSULIN_UNITS" -> {
                            low_units = par.second
                        }
                        "FAST_INSULIN_UNITS" -> {
                            fast_units = par.second
                        }
                        "END_SPORT" -> {
                            history[iterator].isSport = true
                        }
                        "LOW_INSULIN_TIME" -> {
                            history[iterator].setLow_insulin(low_units.toInt())
                        }
                        "FAST_INSULIN_TIME" ->{
                            history[iterator].setFast_insulin(fast_units.toInt())
                        }
                        "STRESS" -> history[iterator].isStress = true
                        else -> throw IllegalStateException("Unexpected value: " + par.first)
                    }
                }
            }
            ++iterator
        }
        latest_data_processed = history[history.size - 3].timezoneOffsetInMinutes.toLong()

        // !
        val readingData = realmProcessedData.copyToRealmOrUpdate(data_processed)
        realmProcessedData.commitTransaction()
        realmProcessedData.close()
    }


    /*
    companion object {
        @JvmStatic
        fun newInstance(): GlucoseFormFragment {
            return GlucoseFormFragment()
        }
    }
    */

}