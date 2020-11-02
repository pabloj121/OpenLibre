package de.dorianscholz.openlibre.ui

import android.Manifest
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
import java.io.IOException
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
    private lateinit var listView: ListView
    private lateinit var list:List<String>
    lateinit var adapter: ArrayAdapter<*>
    private lateinit var calendarEvents:MutableList<CalendarEvent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            //param1 = it.getString(ARG_PARAM1)
            //param2 = it.getString(ARG_PARAM2)
        }

        calendar = Calendar.getInstance()
        list = listOf()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_agenda, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // set on click listener
        add_event_button = view.findViewById(R.id.addEvent)
        calendarView = view.findViewById(R.id.calendar)
        event_title = view.findViewById(R.id.event_title)
        event_description = view.findViewById(R.id.event_description)
        listView = view.findViewById(R.id.list_view)
        calendarEvents = mutableListOf<CalendarEvent>()

        // Falta volver a llamar al readCalendarEvent segun fecha
        var listEvents = readCalendarEvent(view.context)

        listEvents.forEach {
            list = list + listOf(it.getTitle() + "." + it.getDescription())
        }

        //list = listOf("hola", "adios", "que tal")

        // Adding the components to the list
        adapter = ArrayAdapter(view.context, android.R.layout.simple_list_item_1,list)
        listView.setAdapter(adapter)

        /*
        add_event_button.setOnClickListener() {
            object : View.OnClickListener {
                override fun onClick(view: View) {
                    Toast.makeText(view.context, "hola", Toast.LENGTH_SHORT).show()
                    System.out.println(event_title.toString() + ":" + event_title.text.toString())
                    addEvent(view, event_title.text.toString(), event_description.text.toString())
                }
            }
        }
        */

        add_event_button.setOnClickListener(View.OnClickListener {
            //Toast.makeText(view.context, event_title.toString() + ":" + event_title.text.toString(), Toast.LENGTH_SHORT).show()
            addEvent(view, event_title.text.toString(), event_description.text.toString())
        })

        calendarView.setOnDateChangeListener(OnDateChangeListener {
            view, year, month, dayOfMonth ->
            //calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            calendar.get(Calendar.DAY_OF_WEEK)

            val string:String = "Día seleccionado: " + dayOfMonth.toString() + "/" + month.toString() + "/" + year.toString()

            if (view != null) {
                Toast.makeText(view.context, string, Toast.LENGTH_SHORT).show()
            }
        })
    }

    //fun readCalendarEvent(context: Context): ArrayList<String?>? {
    fun readCalendarEvent(context: Context): MutableList<CalendarEvent> {

        // Permissions
        if (checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {

            val cursor:Cursor? = context.getContentResolver()
                    .query(Uri.parse("content://com.android.calendar/events"), arrayOf("calendar_id", "title", "description",
                            "dtstart", "dtend", "eventLocation"), null,
                            null, null)

            cursor?.moveToFirst()

            // fetching calendars name
            val CNames = arrayOfNulls<String>(cursor!!.getCount())

            for (i in CNames.indices) {
                calendarEvents.add(CalendarEvent(cursor.getString(1),cursor.getString(2)))
                CNames[i] = cursor.getString(1)
                cursor.moveToNext()
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

        /*
        val cursor:Cursor? = context.getContentResolver()
                .query(Uri.parse("content://com.android.calendar/events"), arrayOf("calendar_id", "title", "description",
                        "dtstart", "dtend", "eventLocation"), null,
                        null, null)


        cursor?.moveToFirst()

        // fetching calendars name
        val CNames = arrayOfNulls<String>(cursor!!.getCount())

        for (i in CNames.indices) {
            calendarEvents.add(CalendarEvent(cursor.getString(1),cursor.getString(2)))
            CNames[i] = cursor.getString(1)
            cursor.moveToNext()
        }
        */
        //return nameOfEvent
        return calendarEvents
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

    /*
    fun readingEvents(view: View){

        val cursor: Cursor = cr.query(Uri.parse("content://calendar/events"), arrayOf("calendar_id", "title", "description", "dtstart", "dtend", "eventLocation"), null, null, null)
        //Cursor cursor = cr.query(Uri.parse("content://calendar/calendars"), new String[]{ "_id", "name" }, null, null, null);
        //Cursor cursor = cr.query(Uri.parse("content://calendar/calendars"), new String[]{ "_id", "name" }, null, null, null);
        var add: String? = null
        cursor.moveToFirst()
        val CalNames = arrayOfNulls<String>(cursor.getCount())
        val CalIds = IntArray(cursor.getCount())
        for (i in CalNames.indices) {
            CalIds[i] = cursor.getInt(0)
            CalNames[i] = """
                Event${cursor.getInt(0).toString()}: 
                Title: ${cursor.getString(1).toString()}
                Description: ${cursor.getString(2).toString()}
                Start Date: ${Date(cursor.getLong(3)).toString()}
                End Date : ${Date(cursor.getLong(4)).toString()}
                Location : ${cursor.getString(5)}
                """.trimIndent()
            if (add == null) add = CalNames[i] else {
                add += CalNames[i]
            }

            (view.findViewById(R.id.calendars) as TextView).text = add
            cursor.moveToNext()
        }
        cursor.close()
    }
    */

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

// check !
class CalendarEvent(t: String, d: String) {
    private var id:String=""
    private var title:String=t
    private var description:String=d

    fun CalendarEvent(t:String, d:String){
        title = t
        description = d
    }

    fun setID(i:String){
        id = i
    }

    fun getID():String{
        return id
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
        var result:String = "Title: " + getTitle() + ". Description: " + getDescription()
        return result
    }

}