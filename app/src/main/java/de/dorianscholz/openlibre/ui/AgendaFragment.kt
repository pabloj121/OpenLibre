package de.dorianscholz.openlibre.ui

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.ognev.kotlin.agendacalendarview.AgendaCalendarView
import com.ognev.kotlin.agendacalendarview.CalendarController
import com.ognev.kotlin.agendacalendarview.models.CalendarEvent
import com.ognev.kotlin.agendacalendarview.CalendarManager
import com.ognev.kotlin.agendacalendarview.builder.CalendarContentManager
import com.ognev.kotlin.agendacalendarview.models.BaseCalendarEvent
import com.ognev.kotlin.agendacalendarview.models.DayItem
import com.ognev.kotlin.agendacalendarview.models.IDayItem
import com.ognev.kotlin.agendacalendarview.models.IWeekItem
import com.ognev.kotlin.agendacalendarview.render.DefaultEventAdapter
import com.ognev.kotlin.agendacalendarview.utils.DateHelper
import de.dorianscholz.openlibre.OpenLibre
import de.dorianscholz.openlibre.R
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_agenda.*
import java.text.SimpleDateFormat
import java.util.*


private var agenda_calendar_view: AgendaCalendarView? = null   // create a global variable which will hold your layout !

// CalendarExtensions
fun Calendar.isSameDay(newDate: Calendar): Boolean =
        this.get(Calendar.DAY_OF_MONTH) == newDate.get(Calendar.DAY_OF_MONTH)

// class MainActivity : AppCompatActivity(), CalendarController {}

/**
 * A simple [Fragment] subclass.
 * Use the [AgendaFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AgendaFragment : Fragment(), CalendarController {

    private var oldDate: Calendar? = null
    private var eventList: MutableList<CalendarEvent> = ArrayList()
    private lateinit var minDate: Calendar
    private lateinit var maxDate: Calendar
    private lateinit var contentManager: CalendarContentManager
    private var startMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
    private var endMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
    private var loadingTask: LoadingTask? = null
    private var mRealmProcessedData: Realm? = null

    // Completar?!
    fun AgendaFragment() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mRealmProcessedData = Realm.getInstance(OpenLibre.realmConfigProcessedData)
        /*
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        */

        /*
        val minDate = Calendar.getInstance()
        val maxDate = Calendar.getInstance()
        minDate.add(Calendar.MONTH, -2)
        minDate[Calendar.DAY_OF_MONTH] = 1
        maxDate.add(Calendar.YEAR, 1)
        val eventList: List<CalendarEvent> = ArrayList()
        mockList(eventList)
        mAgendaCalendarView.init(eventList, minDate, maxDate, Locale.getDefault(), this)
        */


        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_main)

        oldDate = Calendar.getInstance()
        minDate = Calendar.getInstance()
        maxDate = Calendar.getInstance()

        minDate.add(Calendar.MONTH, -10)
        minDate.add(Calendar.YEAR, -1)
        minDate.set(Calendar.DAY_OF_MONTH, 1)
        maxDate.add(Calendar.YEAR, 1)


        //contentManager = CalendarContentManager(this, agenda_calendar_view, SampleEventAgendaAdapter(applicationContext))
        /*
        contentManager = CalendarContentManager(this, agenda_calendar_view, SampleEventAgendaAdapter(requireView().context))
        contentManager.locale = Locale.ENGLISH
        contentManager.setDateRange(minDate, maxDate)
        */

        val maxLength = Calendar.getInstance().getMaximum(Calendar.DAY_OF_MONTH)

        for (i in 1..maxLength) {
            val day = Calendar.getInstance(Locale.ENGLISH)
            day.timeInMillis = System.currentTimeMillis()
            day.set(Calendar.DAY_OF_MONTH, i)


            eventList.add(MyCalendarEvent(day, day,
                    DayItem.buildDayItemFromCal(day),
                    SampleEvent(name = "Awesome $i", description = "Event $i"))
                    .setEventInstanceDay(day))

            /*
            eventList.add(MyCalendarEvent(day, day,
                    DayItem.buildDayItemFromCal(day), null).setEventInstanceDay(day))
            */

            //DayItem.buildDayItemFromCal(day), SampleEvent("", "")).setEventInstanceDay(day))
            //eventList.add(MyCalendarEvent(day, day,
            //        DayItem.buildDayItemFromCal(day), CalendarEvent()).setEventInstanceDay(day))
            //DayItem.buildDayItemFromCal(day), null).setEventInstanceDay(day))
        }


        //contentManager.loadItemsFromStart(eventList)
        agenda_calendar_view.agendaView.agendaListView.setOnItemClickListener({ parent: AdapterView<*>, view: View, position: Int, id: Long ->
            Toast.makeText(view.context, "item: ".plus(position), Toast.LENGTH_SHORT).show()
        })
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        contentManager = CalendarContentManager(this, agenda_calendar_view, SampleEventAgendaAdapter(requireView().context))
        contentManager.locale = Locale.ENGLISH
        contentManager.setDateRange(minDate, maxDate)


        contentManager.loadItemsFromStart(eventList)

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_agenda, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AgendaFragment.
         */
        // TODO: Rename and change types and number of parameters

        @JvmStatic
        fun newInstance(): AgendaFragment? {
                /* AgendaFragment().apply {
                        arguments = Bundle().apply {
                            putString(ARG_PARAM1, param1)
                            putString(ARG_PARAM2, param2) }}  */
            return AgendaFragment()
        }
    }

    override fun onStart() {
        super.onStart()
    }

    fun show(supportFragmentManager: FragmentManager, s: String) {
    }

    fun addEvent(){
        // Using Google API's
    }




    override fun onStop() {
        super.onStop()
        loadingTask?.cancel(true)
    }

    override fun getEmptyEventLayout() = R.layout.view_agenda_empty_event

    override fun getEventLayout() = R.layout.view_agenda_event

    override fun onDaySelected(dayItem: IDayItem) {
    }

    override fun onScrollToDate(calendar: Calendar) {
        val lastPosition = agenda_calendar_view.agendaView.agendaListView.lastVisiblePosition + 1

        val isSameDay = oldDate?.isSameDay(calendar) ?: false
        // if (isSameDay && lastPosition == CalendarManager.getInstance(this).events.size) {
        if (isSameDay && lastPosition == activity?.let { CalendarManager.getInstance(it).events.size }) {
            if (!agenda_calendar_view.isCalendarLoading()) { // condition to prevent asynchronous requests
                loadItemsAsync(false)
            }
        }

        if (agenda_calendar_view.agendaView.agendaListView.firstVisiblePosition == 0) {
            val minCal = Calendar.getInstance()
            minCal.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH))
            if (calendar.get(Calendar.DAY_OF_MONTH) == minCal.get(Calendar.DAY_OF_MONTH)) {
                if (!agenda_calendar_view.isCalendarLoading()) { // condition to prevent asynchronous requests
                    loadItemsAsync(true)
                }
            }
        }

        oldDate = calendar
    }

    private fun loadItemsAsync(addFromStart: Boolean) {
        loadingTask?.cancel(true)

        loadingTask = LoadingTask(addFromStart)
        loadingTask?.execute()
    }



    inner class LoadingTask(private val addFromStart: Boolean) : AsyncTask<Unit, Unit, Unit>() {

        private val startMonthCal: Calendar = Calendar.getInstance()
        private val endMonthCal: Calendar = Calendar.getInstance()

        override fun onPreExecute() {
            super.onPreExecute()
            agenda_calendar_view.showProgress()
            eventList.clear()
        }

        override fun doInBackground(vararg params: Unit?) {
            Thread.sleep(2000) // simulating requesting json via rest api

            if (addFromStart) {
                if (startMonth == 0) {
                    startMonth = 11
                } else {
                    startMonth--
                }

                startMonthCal.set(Calendar.MONTH, startMonth)
                if (startMonth == 11) {
                    var year = startMonthCal.get(Calendar.YEAR)
                    startMonthCal.set(Calendar.YEAR, ++year)
                }


                for (i in 1..startMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                    val day = Calendar.getInstance(Locale.ENGLISH)
                    day.timeInMillis = System.currentTimeMillis()
                    day.set(Calendar.MONTH, startMonth)
                    day.set(Calendar.DAY_OF_MONTH, i)
                    if (endMonth == 11) {
                        day.set(Calendar.YEAR, day.get(Calendar.YEAR) - 1)
                    }

                    eventList.add(MyCalendarEvent(day, day,
                            DayItem.buildDayItemFromCal(day),
                            SampleEvent(name = "Awesome $i", description = "Event $i"))
                            .setEventInstanceDay(day))
                }
            } else {
                if (endMonth >= 11) {
                    endMonth = 0
                } else {
                    endMonth++
                }

                endMonthCal.set(Calendar.MONTH, endMonth)
                if (endMonth == 0) {
                    var year = endMonthCal.get(Calendar.YEAR)
                    endMonthCal.set(Calendar.YEAR, ++year)
                }

                for (i in 1..endMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                    val day = Calendar.getInstance(Locale.ENGLISH)
                    day.timeInMillis = System.currentTimeMillis()
                    day.set(Calendar.MONTH, endMonth)
                    day.set(Calendar.DAY_OF_MONTH, i)
                    if (endMonth == 0) {
                        day.set(Calendar.YEAR, day.get(Calendar.YEAR) + 1)
                    }

                    if (i % 4 == 0) {
                        val day1 = Calendar.getInstance()
                        day1.timeInMillis = System.currentTimeMillis()
                        day1.set(Calendar.MONTH, endMonth)
                        day1.set(Calendar.DAY_OF_MONTH, i)
                        eventList.add(MyCalendarEvent(day, day,
                                DayItem.buildDayItemFromCal(day),
                                SampleEvent(name = "Awesome $i", description = "Event $i"))
                                .setEventInstanceDay(day))
                    }

                    eventList.add(MyCalendarEvent(day, day,
                            DayItem.buildDayItemFromCal(day),
                            SampleEvent(name = "Awesome $i", description = "Event $i"))
                            .setEventInstanceDay(day))
                }
            }
        }

        override fun onPostExecute(user: Unit) {
            if (addFromStart) {
                contentManager.loadItemsFromStart(eventList)
            } else {
                contentManager.loadFromEndCalendar(eventList)
            }
            agenda_calendar_view.hideProgress()
        }
    }

}

//private var eventList: MutableList<CalendarEvent> = ArrayList()
// Hay que probar esta función
private fun <E> MutableList<E>.add(myCalendarEvent: MyCalendarEvent) {
    this.add(myCalendarEvent)
}


class SampleEvent(name: String, description: String) {
    var id: Long = 0
    var name: String = name
    var desciption: String = description
}

class MyCalendarEvent: BaseCalendarEvent {

    override lateinit var startTime: Calendar
    override lateinit var endTime: Calendar
    override var event: Any? = null

    override lateinit var instanceDay: Calendar


    override lateinit var dayReference: IDayItem

    override lateinit var weekReference: IWeekItem

    override fun setEventInstanceDay(instanceDay: Calendar): MyCalendarEvent {
        this.instanceDay = instanceDay
        this.instanceDay.set(Calendar.HOUR, 0)
        this.instanceDay.set(Calendar.MINUTE, 0)
        this.instanceDay.set(Calendar.SECOND, 0)
        this.instanceDay.set(Calendar.MILLISECOND, 0)
        this.instanceDay.set(Calendar.AM_PM, 0)
        return this
    }

    constructor(calendarEvent: MyCalendarEvent) {
    }


    constructor(startTime: Calendar,
                endTime: Calendar,
                dayItem: DayItem,
                event: SampleEvent?)  {
        this.startTime = startTime
        this.endTime = endTime
        this.dayReference = dayItem
        this.event = event
    }


    override
    fun copy(): MyCalendarEvent {
        return MyCalendarEvent(this)
    }

    override fun hasEvent(): Boolean {
        return event != null
    }

    override
    fun toString(): String {
        return super.toString()
    }
}


//
class SampleEventAgendaAdapter(var context: Context) : DefaultEventAdapter() {
    private var format: SimpleDateFormat? = null

    init {
        format = SimpleDateFormat(context.getString(com.ognev.kotlin.agendacalendarview.R.string.header_date),
                Locale.ENGLISH)
    }

    override fun getHeaderLayout(): Int {
        return R.layout.view_agenda_header
    }

    override fun getHeaderItemView(view: View, day: Calendar) {
        val txtDayOfMonth = view.findViewById(R.id.view_agenda_day_of_month) as TextView
        val today = CalendarManager.instance!!.today

        if (DateHelper.sameDate(day, today)) {
            //txtDayOfMonth.setTextColor(context.resources.getColor(R.color.main_blue))
            txtDayOfMonth.setTextColor(context.resources.getColor(R.color.blue_selected))
        } else{
            txtDayOfMonth.setTextColor(context.resources.getColor(R.color.text_light_color))
        }

        txtDayOfMonth.text = format!!.format(day.time)
    }

    //override fun getEventItemView(view: View, event: CalendarEvent, position: Int) {
    override fun getEventItemView(view: View, event: com.ognev.kotlin.agendacalendarview.models.CalendarEvent, position: Int) {
        val myEvent = event as MyCalendarEvent
        val myObject: SampleEvent? = myEvent.event as SampleEvent?

        if(myEvent.hasEvent()) {
            (view.findViewById(R.id.name)
                    as TextView).text = myObject!!.name

            (view.findViewById(R.id.description)
                    as TextView).text = myObject.desciption
        }

        view.setOnClickListener {
            Toast.makeText(view.context, "Item: ".plus(position), Toast.LENGTH_SHORT).show()
        }
    }

    override fun getEventLayout(hasEvent: Boolean): Int {
        return if(hasEvent) R.layout.view_agenda_event else R.layout.view_agenda_empty_event
    }
}




//


