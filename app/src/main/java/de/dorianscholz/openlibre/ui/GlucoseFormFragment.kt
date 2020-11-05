package de.dorianscholz.openlibre.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import de.dorianscholz.openlibre.R
import java.text.DateFormat
import java.text.SimpleDateFormat


/**
 * A simple [Fragment] subclass.
 * Use the [GlucoseFormFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class GlucoseFormFragment : DialogFragment() {
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
            // Data treatment and pass information to the Main Activity
            dataTreatment()
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


    // Once you have completed the data, it's the moment to treat the database
    fun dataTreatment(){
        dataGathering()

        // Now we can check the extra data and fulfill the database with it

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment GlucoseFormFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(): GlucoseFormFragment {
            return GlucoseFormFragment()
        }
    }
}