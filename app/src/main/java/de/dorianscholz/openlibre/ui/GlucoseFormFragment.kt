package de.dorianscholz.openlibre.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import de.dorianscholz.openlibre.R
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


/**
 * A simple [Fragment] subclass.
 * Use the [GlucoseFormFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class GlucoseFormFragment : DialogFragment() {
    private lateinit var add_information_button:Button
    private lateinit var low_insulin:EditText
    private lateinit var fast_insulin:EditText
    private lateinit var sport:Switch
    private lateinit var start_time:EditText
    private lateinit var end_time:EditText
    private lateinit var stress:Switch


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
        low_insulin = view.findViewById(R.id.low_insulin)
        fast_insulin = view.findViewById(R.id.fast_insulin)
        sport = view.findViewById(R.id.sport)
        start_time = view.findViewById(R.id.start_time)
        end_time = view.findViewById(R.id.end_time)
        stress = view.findViewById(R.id.stress)

        var time1:String = ""
        var date1:String = ""
        var time2:String = ""
        var date2:String = ""

        // It's not necessary to fill all the fields
        var l_insulin_units:Int = Integer.parseInt(fast_insulin.text.toString())
        var f_insulin_units:Int = Integer.parseInt(low_insulin.text.toString())

        if (sport.isChecked){
            start_time.visibility = View.VISIBLE
            end_time.visibility = View.VISIBLE

        }

        // Obtain values from EditText
        val formatter: DateFormat = SimpleDateFormat("dd/MM/yyyy") // Make sure user insert date into edittext in this format.
        var dateObject: Date

        try {   // Start time
            val dob_var: String = start_time.getText().toString()
            dateObject = formatter.parse(dob_var)
            date1 = SimpleDateFormat("dd/MM/yyyy").format(dateObject)
            time1 = SimpleDateFormat("h:mmaa").format(dateObject)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        try {   // End time
            val dob_var: String = start_time.getText().toString()
            dateObject = formatter.parse(dob_var)
            //dateObject.time

            date2 = SimpleDateFormat("dd/MM/yyyy").format(dateObject)
            time2 = SimpleDateFormat("h:mmaa").format(dateObject)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        //Toast.makeText(context, date1 + time1, Toast.LENGTH_LONG).show()



        add_information_button.setOnClickListener(View.OnClickListener {

            // Data treatment and pass information to the Main Activity
            dataTreatment()
        })
    }

    // Once you have completed the data, it's the moment to treat the database
    fun dataTreatment(){

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