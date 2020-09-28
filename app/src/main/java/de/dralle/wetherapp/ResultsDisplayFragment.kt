package de.dralle.wetherapp

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_second.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ResultsDisplayFragment : Fragment(),IUpdateListener {

    private var shared: SharedDataContainer?=null

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_second, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        update()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(tag,"onAttach()")
        wireToActivity(context)
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        Log.d(tag,"deprecated onAttach()")
        wireToActivity(activity as Context)
    }

    private fun wireToActivity(context: Context) {
        if(context is WetherAppMainActivity){
            context.addUpdatableFragment(this)
        }
    }

    override fun update() {
        Log.d(tag,"Update second fragment")
        Log.d(tag,"Data is ${shared?.apiResponseObject}")
        val fullResponse=shared?.apiResponseObject
        if(fullResponse!=null){
            activity?.runOnUiThread{
                updateUI(fullResponse)
            }
        }
    }

    private fun updateUI(fullResponse: JSONObject) {
        val coords=fullResponse.get("coord")
        val weatherOverview=fullResponse.getJSONArray("weather").getJSONObject(0) //error in openweatherapi?
        val weatherMain=weatherOverview.get("main")
        val weatherDetail=fullResponse.getJSONObject("main")
        val temp=weatherDetail.get("temp")
        val feel=weatherDetail.get("feels_like")
        val airPressure=weatherDetail.get("pressure")
        val humidity=weatherDetail.get("humidity")
        val minTemp=weatherDetail.get("temp_min")
        val maxTemp=weatherDetail.get("temp_max")
        val wind=fullResponse.getJSONObject("wind")
        val windSpeed=wind.get("speed")
        val windDir=wind.get("deg")
        var gusts:Any?=null
        if(wind.has("gust")){
            gusts=wind.get("gust")
        }
        val timestamp=fullResponse.getLong("dt")
        val additional=fullResponse.getJSONObject("sys")
        val countryCode=additional.get("country")
        val sunrise=additional.getLong("sunrise")
        val sunset=additional.getLong("sunset")
        val timezoneShiftSeconds=fullResponse.getLong("timezone")
        val cityName=fullResponse.get("name")

        val timestampShifted=timestamp+timezoneShiftSeconds
        val sunriseShifted=sunrise+timezoneShiftSeconds
        val sunsetShifted=sunset+timezoneShiftSeconds

        var availableTimezones=TimeZone.getAvailableIDs(timezoneShiftSeconds.toInt()*1000);
        var timezone:TimeZone?= null
        if(availableTimezones.isNotEmpty()){
            timezone= TimeZone.getTimeZone(availableTimezones[0])
            Log.d(tag,"Timezone chosen from list: ${timezone.displayName}. This might not be the correct timezone for that region, but it is chosen based on the given offset.")
        }else{
            timezone= TimeZone.getDefault()
            Log.d(tag,"No timezone suitable for the given offset $timezoneShiftSeconds found. Using device default timezone: ${timezone.displayName}" )
        }

        val formatterDateAndTime=SimpleDateFormat("yyyy-MM-dd HH:mm")
        formatterDateAndTime.timeZone= timezone
        val timestampDateInstance=Date(timestamp*1000)

        val timestampString=formatterDateAndTime.format(timestampDateInstance)

        val formatterTime=SimpleDateFormat("HH:mm")
        formatterTime.timeZone= timezone
        val sunriseString=formatterTime.format(Date(sunrise*1000))
        val sunsetString=formatterTime.format(Date(sunset*1000))

        val unitsTemp=resources.getString(R.string.unit_temperature_metric)
        val unitsSpeed=resources.getString(R.string.unit_speed_metric)

        Log.d(tag,"Object parsing complete")

        tv_loc.text=resources.getString(R.string.header_line,"$cityName,$countryCode")
        tv_situation.text=resources.getString(R.string.overview,weatherMain)
        tv_temp.text=resources.getString(R.string.cur_temperature,temp,unitsTemp)
        tv_fl.text=resources.getString(R.string.feels_temperature,feel,unitsTemp)
        tv_min.text=resources.getString(R.string.min_temperature,minTemp,unitsTemp)
        tv_max.text=resources.getString(R.string.max_temperature,maxTemp,unitsTemp)
        tv_humid.text=resources.getString(R.string.humidity,humidity)
        tv_press.text=resources.getString(R.string.pressure,airPressure)
        tv_sunrise.text=resources.getString(R.string.sunrise,sunriseString)
        tv_sunset.text=resources.getString(R.string.sunset,sunsetString)
        tv_timstamp.text=resources.getString(R.string.timestamp,timestampString)
        tv_wspeed.text=resources.getString(R.string.wind_speed,windSpeed,unitsSpeed)
        tv_wdir.text=resources.getString(R.string.heading,windDir)
        tv_wgust.text=if(gusts==null) "" else resources.getString(R.string.wind_gusts,gusts,unitsSpeed)
    }

    override fun setSharedDataContainer(container: SharedDataContainer) {
        shared=container
    }
}