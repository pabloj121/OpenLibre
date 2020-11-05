package de.dorianscholz.openlibre.ui

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.CalendarView.OnDateChangeListener
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import de.dorianscholz.openlibre.R
import java.text.SimpleDateFormat
import java.util.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AgendaFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AgendaFragment : Fragment() {
    private lateinit var add_event_button: Button
    private lateinit var calendarView: CalendarView
    private lateinit var event_title:EditText
    private lateinit var event_description:EditText
    private lateinit var calendar:Calendar
    private lateinit var calendarEvents:MutableList<CalendarEvent>
    private var selectedDay:Int = 0
    private var selectedMonth:Int = 0
    private var selectedYear:Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }

        calendar = Calendar.getInstance()

        selectedDay = calendar.get(Calendar.DAY_OF_MONTH)
        selectedMonth = calendar.get(Calendar.MONTH)
        selectedYear = calendar.get(Calendar.YEAR)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_agenda, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        add_event_button = view.findViewById(R.id.addEvent)
        calendarView = view.findViewById(R.id.calendar)
        event_title = view.findViewById(R.id.event_title)
        event_description = view.findViewById(R.id.event_description)
        calendarEvents = mutableListOf<CalendarEvent>()

        add_event_button.setOnClickListener(View.OnClickListener {
            addEvent(view, event_title.text.toString(), event_description.text.toString())
        })

        calendarView.setOnDateChangeListener(OnDateChangeListener {
            view, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            calendar.get(Calendar.DAY_OF_WEEK)

            selectedDay = dayOfMonth
            selectedMonth = month
            selectedYear = year

            if (view != null) {
                val string:String = "Día seleccionado: " + dayOfMonth.toString() + "/" +
                        month.toString() + "/" + year.toString()

                Toast.makeText(view.context, string, Toast.LENGTH_SHORT).show()
                seeCalendarData(calendar.timeInMillis)
            }
        })
    }


    fun readCalendar(context: Context): MutableList<CalendarEvent>{
        calendarEvents.clear()

        // Permissions
        if (checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {

            val DEBUG_TAG: String = "MyActivity"
            val INSTANCE_PROJECTION: Array<String> = arrayOf(
                    CalendarContract.Instances.EVENT_ID, // 0
                    CalendarContract.Instances.BEGIN, // 1
                    CalendarContract.Instances.TITLE, // 2
                    CalendarContract.Instances.DESCRIPTION  // 3
            )

            // The indices for the projection array above.
            val PROJECTION_ID_INDEX: Int = 0
            val PROJECTION_BEGIN_INDEX: Int = 1
            val PROJECTION_TITLE_INDEX: Int = 2
            val PROJECTION_DESCRIPTION_INDEX: Int = 3

            // Specify the date range you want to search for recurring event instances
            // In this case, the whole day selected
            val startMillis: Long = Calendar.getInstance().run {
                set(selectedYear, selectedMonth, selectedDay, 0, 0)
                timeInMillis
            }
            val endMillis: Long = Calendar.getInstance().run {
                set(selectedYear, selectedMonth, selectedDay, 23, 59)
                timeInMillis
            }

            // The ID of the recurring event whose instances you are searching
            // for in the Instances table
            val selection: String = "${CalendarContract.Instances.EVENT_ID} = ?"
            val selectionArgs: Array<String> = arrayOf("207")

            // Construct the query with the desired date range.
            val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, startMillis)
            ContentUris.appendId(builder, endMillis)

            // Submit the query
            //val cur: Cursor = contentResolver.query(
            val cur: Cursor? = context.contentResolver.query(     //contentResolver.query(
                    builder.build(),
                    INSTANCE_PROJECTION,
                    selection,
                    selectionArgs, null
            )

            if (cur != null) {
                while (cur.moveToNext()) {
                    // Get the field values
                    val eventID: Long = cur.getLong(PROJECTION_ID_INDEX)
                    val beginVal: Long = cur.getLong(PROJECTION_BEGIN_INDEX)
                    val title: String = cur.getString(PROJECTION_TITLE_INDEX)
                    val description = cur.getString(PROJECTION_DESCRIPTION_INDEX)

                    // Do something with the values.
                    calendarEvents.add(CalendarEvent(eventID, beginVal, title, description))

                    // Log.i(DEBUG_TAG, "Event: $title")
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = beginVal
                    }
                    val formatter = SimpleDateFormat("MM/dd/yyyy")
                    // Log.i(DEBUG_TAG, "Date: ${formatter.format(calendar.time)}")
                }
            }

        } // Read calendar has not been granted
        else if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Toast.makeText(context, "Read calendar permission is " +
                    "needed to add an event", Toast.LENGTH_SHORT).show()

            // Request read calendar permission
            requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), 1)
        } else {
            // You can directly ask for the permission.
            requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), 1)
        }
        return calendarEvents
    }

    fun seeCalendarData(startMillis:Long){
        val builder: Uri.Builder = CalendarContract.CONTENT_URI.buildUpon()
                .appendPath("time")
        ContentUris.appendId(builder, startMillis)
        val intent = Intent(Intent.ACTION_VIEW)
                .setData(builder.build())
        startActivity(intent)
    }

    fun addEvent(view: View, title: String, description: String) {

        if(title.isNotEmpty() && description.isNotEmpty()){
            val intent = Intent(Intent.ACTION_INSERT)
            //intent.data = CalendarContract.CONTENT_URI
            // falta calendarID
            intent.setData(CalendarContract.Events.CONTENT_URI)
            intent.putExtra(CalendarContract.Events.TITLE, title)
            intent.putExtra(CalendarContract.Events.DESCRIPTION, description)
            intent.putExtra(CalendarContract.Events.EVENT_TIMEZONE, calendar.timeZone)
            //DTSTART,DURATION
            intent.putExtra(CalendarContract.Events.DTSTART, calendar.timeInMillis)
            intent.putExtra(CalendarContract.Events.HAS_ALARM, false)
            intent.putExtra(CalendarContract.Events.ALLOWED_REMINDERS, false)

            //intent.putExtra(CalendarContract.Events.ALL_DAY, false)
            startActivity(intent)

            // Probar el requireActivity
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(view.context, "There is no app than can support this action", Toast.LENGTH_SHORT).show()
            }

        } else{
            Toast.makeText(view.context, "You must complete all the fields",Toast.LENGTH_SHORT).show()
        }

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment AgendaFragment.
         */
        @JvmStatic
        fun newInstance(): AgendaFragment? {
            return AgendaFragment()
        }
    }
}

class CalendarEvent(i: Long, begin: Long, tit: String, desc: String) {
    private var id:Long = i
    private var begin_value:Long = begin
    private var title:String=tit
    private var description:String=desc


    fun setID(i:Long){
        id = i
    }

    fun getID():Long{
        return id
    }

    fun setBeginValue(begin:Long){
        begin_value = begin
    }

    fun getBeginValue():Long{
        return begin_value
    }

    fun setTitle(t:String){
        title = t
    }

    fun getTitle():String{
        return title
    }

    fun setDescription(d:String){
        description = d
    }

    fun getDescription():String{
        return description
    }

    override fun toString():String{
        val date = Date(begin_value)
        val format = SimpleDateFormat("yyyy/ MM/ dd HH:mm")
        val date_event = format.format(date)

        var result:String = "Title: " + getTitle() + "Hora" + date_event + "\n. Description: " + getDescription()
        return result
    }

}