package de.dralle.wetherapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class LocationInputFragment : Fragment(), IUpdateListener {

    interface OnAPICallResultListener {
        fun onApiCallResult()
    }

    private val PERMISSION_FINE_LOCATION_REQUEST_CODE: Int=42
    private var callback: OnAPICallResultListener? = null
    private var shared: SharedDataContainer? = null
    private var fusedLocationProvide:FusedLocationProviderClient?=null

    fun setOnApiCallResultListener(listener: OnAPICallResultListener) {
        callback = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationProvide= context?.let { LocationServices.getFusedLocationProviderClient(it) }

        val btnCity = view.findViewById<Button>(R.id.btnSearchCity)
        val btnZip = view.findViewById<Button>(R.id.btnSearchZip)
        val btnGPS = view.findViewById<Button>(R.id.btnSearchGPS)
        val btnLocate = view.findViewById<Button>(R.id.btnLocate)

        btnCity.setOnClickListener(
            View.OnClickListener {
                callByCityName()
            })
        btnZip.setOnClickListener(
            View.OnClickListener {
                callByZipCode()
            })
        btnGPS.setOnClickListener(
            View.OnClickListener {
                callByGPS()
            })
        btnLocate.setOnClickListener {
            locateAndWriteGPS()
        }
        Log.i(tag, "test")
        val isDebugLoggable = Log.isLoggable(tag, Log.DEBUG)
        Log.e(tag, "Debug is loggable: $isDebugLoggable")

    }

    private fun locateAndWriteGPS() {
        if (ContextCompat.checkSelfPermission(
                activity!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PermissionChecker.PERMISSION_GRANTED
        ){
            val locTask:Task<Location>?=fusedLocationProvide?.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY,null)
            locTask?.addOnCompleteListener(activity!!, OnCompleteListener {
                Log.d(tag,"Location request completed")
                val loc=it.result
                if(loc!=null){
                    Log.d(tag,"Latitude: ${loc.latitude} Longitude: ${loc.longitude}")
                }else{
                    Log.w(tag,"No exact location found")
                }
                if(loc!=null){
                activity?.runOnUiThread {
                    writeLocationToUI(loc)
                }}
            })
        } else {
            if (shouldShowRequestPermissionRationale(
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                val dialog = MessageDialogFragment()
                if (fragmentManager != null) {
                    dialog.show(fragmentManager!!, tag)
                } else {
                    Log.w(tag, "Cant show dialog")
                }
            }
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),PERMISSION_FINE_LOCATION_REQUEST_CODE)
        }
    }

    private fun writeLocationToUI(loc: Location) {
        val etFieldLat = view?.findViewById<EditText>(R.id.editTextLatitude);
        val etFieldLon = view?.findViewById<EditText>(R.id.editTextLongitude);
        etFieldLat?.setText(loc.latitude.toString(),TextView.BufferType.EDITABLE)
        etFieldLon?.setText(loc.longitude.toString(),TextView.BufferType.EDITABLE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PERMISSION_FINE_LOCATION_REQUEST_CODE ->{
                if(grantResults.isNotEmpty()&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    Log.d(tag, "Access to location granted")
                }else{
                    Log.e(tag, "Access to location not granted")
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(tag, "onAttach()")
        wireToActivity(context)
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        Log.d(tag, "deprecated onAttach()")
        wireToActivity(activity as Context)
    }

    private fun wireToActivity(context: Context) {
        if (context is WetherAppMainActivity) {
            context.addUpdatableFragment(this)
        }
        if (context is OnAPICallResultListener) {
            Log.d(tag, "Attached listener")
            setOnApiCallResultListener(context)
        }
    }

    override fun update() {
        TODO("Not yet implemented")
    }

    override fun setSharedDataContainer(container: SharedDataContainer) {
        shared = container
    }

    private fun callByGPS() {
        if (ContextCompat.checkSelfPermission(
                context!!,
                Manifest.permission.INTERNET
            ) == PermissionChecker.PERMISSION_GRANTED
        ) {
            val lat=getLatitudeFromUI()
            val lon=getLongitudeFromUI()
            //Make sure the correct decimal point is used
            lat?.replace(',','.')
            lon?.replace(',','.')
            Log.d(tag, "Latitude is $lat, longitude is $lon");
            var urlString = resources.getString(R.string.api_call_gps)
            Log.d(tag, "Loaded api url $urlString")
            urlString = String.format(
                urlString,
                URLEncoder.encode(lat, "UTF-8"),
                URLEncoder.encode(lon, "UTF-8")
            )
            var additonalParams = getParams()
            var additionalParamsString = getParamString(additonalParams)
            urlString += additionalParamsString
            Log.i(tag, "Parameterized api url $urlString")
            GlobalScope.launch {
                var result: String? = null;
                try {
                    result = doApiCall(urlString)
                } catch (e: Exception) {
                    Log.e(tag, "API call failed ${e.message}")
                    val message = "API call failed: ${e.message}"
                    activity?.runOnUiThread {
                        val duration = Toast.LENGTH_LONG
                        val toast = Toast.makeText(context, message, duration)
                        toast.show()
                    }

                }
                if (result != null) {
                    val resultObject = JSONObject(result)
                    shared?.apiResponseObject = resultObject

                    activity?.runOnUiThread {
                        findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
                    }
                }
            }
        } else {
            Log.d(tag, "Internet access permission not granted")
            Toast.makeText(activity, "No internet access permission", Toast.LENGTH_LONG).show()
        }
    }

    private fun getLongitudeFromUI():String?{
        val etFieldLon = view?.findViewById<EditText>(R.id.editTextLongitude);
        return etFieldLon?.text.toString()
    }

    private fun getLatitudeFromUI(): String? {
        val etFieldLat = view?.findViewById<EditText>(R.id.editTextLatitude);
        return etFieldLat?.text.toString()
    }

    private fun callByZipCode() {
        var countryCode = getCountryCode()
        val zipCode = getZipCode()
        Log.d(tag, "Country code is $countryCode");
        Log.d(tag, "Zip code is $zipCode");
        var urlString = resources.getString(R.string.api_call_zip)
        Log.d(tag, "Loaded api url $urlString")
        urlString = String.format(
            urlString,
            URLEncoder.encode(zipCode, "UTF-8"),
            URLEncoder.encode(countryCode, "UTF-8")
        )
        var additonalParams = getParams()
        var additionalParamsString = getParamString(additonalParams)
        urlString += additionalParamsString
        Log.i(tag, "Parameterized api url $urlString")
        GlobalScope.launch (Dispatchers.IO){
            var result: String? = null;
            try {
                result = doApiCall(urlString)
            } catch (e: Exception) {
                Log.e(tag, "API call failed ${e.message}")
                val message = "API call failed: ${e.message}"
                activity?.runOnUiThread {
                    val duration = Toast.LENGTH_LONG
                    val toast = Toast.makeText(context, message, duration)
                    toast.show()
                }

            }
            if (result != null) {
                val resultObject = JSONObject(result)
                shared?.apiResponseObject = resultObject

                activity?.runOnUiThread {
                    findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
                }
            }
        }
    }

    private fun callByCityName() {
        if (ContextCompat.checkSelfPermission(
                context!!,
                Manifest.permission.INTERNET
            ) == PermissionChecker.PERMISSION_GRANTED
        ) {
            var countryCode = getCountryCode()
            var cityName = getCityName()
            Log.d(tag, "Country code is $countryCode");
            Log.d(tag, "City name is $cityName");
            var urlString = resources.getString(R.string.api_call_city)
            Log.d(tag, "Loaded api url $urlString")
            urlString = String.format(
                urlString,
                URLEncoder.encode(cityName, "UTF-8"),
                URLEncoder.encode(countryCode, "UTF-8")
            )
            var additonalParams = getParams()
            var additionalParamsString = getParamString(additonalParams)
            urlString += additionalParamsString
            Log.i(tag, "Parameterized api url $urlString")
            GlobalScope.launch {
                var result: String? = null;
                try {
                    result = doApiCall(urlString)
                } catch (e: Exception) {
                    Log.e(tag, "API call failed ${e.message}")
                    val message = "API call failed: ${e.message}"
                    activity?.runOnUiThread {
                        val duration = Toast.LENGTH_LONG
                        val toast = Toast.makeText(context, message, duration)
                        toast.show()
                    }

                }
                if (result != null) {
                    val resultObject = JSONObject(result)
                    shared?.apiResponseObject = resultObject

                    activity?.runOnUiThread {
                        findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
                    }
                }
            }
        } else {
            Log.d(tag, "Internet access permission not granted")
            Toast.makeText(activity, "No internet access permission", Toast.LENGTH_LONG).show()
        }
    }

    private fun doApiCall(endpoint: String): String? {
        return doApiCall(URL(endpoint))
    }

    private fun doApiCall(endpoint: URL): String? {
        var conn = endpoint.openConnection()
        if (conn is HttpURLConnection) {
            Log.d(tag, "API endpoint is http")
            conn.readTimeout = 15000
            conn.connectTimeout = 15000
            conn.requestMethod = "GET"
            conn.doInput = true
            conn.doOutput = false
            var apiAnswer = conn.inputStream
            var apiAnswerBufferedReader = apiAnswer.bufferedReader(StandardCharsets.UTF_8)
            var apiAnswerString = apiAnswerBufferedReader.readText();
            conn.disconnect()
            Log.d(tag, "Api answered with $apiAnswerString")
            return apiAnswerString
        } else {
            Log.e(tag, "API endpoint is not http")
        }
        return null
    }

    private fun getParamString(additonalParams: Map<String, String>): String {
        var paramString = ""
        for (param in additonalParams.entries) {
            paramString += "&" + URLEncoder.encode(param.key, "UTF-8") + "=" + URLEncoder.encode(
                param.value,
                "UTF-8"
            )
        }
        return paramString
    }

    private fun getParams(): Map<String, String> {
        var prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        var params: MutableMap<String, String>
        var apiKey = prefs.getString("api_key", null)
        Log.d(tag, "API key is $apiKey")
        params = HashMap<String, String>()
        if (apiKey != null) {
            params[resources.getString(R.string.api_key_key)] = apiKey
        }
        params[resources.getString(R.string.language_key)] =
            resources.getString(R.string.language_value)
        params[resources.getString(R.string.units_key)] = resources.getString(R.string.units_value)
        return params
    }

    private fun getZipCode(): String {
        return view?.findViewById<TextView>(R.id.editTextZip)?.text.toString()
    }

    private fun getCityName(): String {
        return view?.findViewById<TextView>(R.id.editTextCity)?.text.toString();
    }

    private fun getCountryCode(): String {
        return view?.findViewById<TextView>(R.id.editTextCountryCode)?.text.toString();
    }
}