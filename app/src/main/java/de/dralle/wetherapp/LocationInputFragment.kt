package de.dralle.wetherapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.location.LocationManagerCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
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

    private val LOCATION_INTENT_REQUEST_CODE: Int = 1337
    private val PERMISSION_FINE_LOCATION_REQUEST_CODE: Int = 42
    private var callback: OnAPICallResultListener? = null
    private var shared: SharedDataContainer? = null
    private var fusedLocationProvider: FusedLocationProviderClient? = null

    fun setOnApiCallResultListener(listener: OnAPICallResultListener) {
        callback = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.location_input_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // activate settings menu button
        if (activity is WetherAppMainActivity) {
            (activity as WetherAppMainActivity).activateSettingsMenu()
        }

        fusedLocationProvider = context?.let { LocationServices.getFusedLocationProviderClient(it) }

        val btnCity = view.findViewById<Button>(R.id.btnSearchCity)
        val btnZip = view.findViewById<Button>(R.id.btnSearchZip)
        val btnCurLoc = view.findViewById<Button>(R.id.btnLocSeGPS)

        btnCity?.setOnClickListener(
            View.OnClickListener {
                callByCityName()
            })
        btnZip?.setOnClickListener(
            View.OnClickListener {
                callByZipCode()
            })
        btnCurLoc?.setOnClickListener {
            //check permission for location
            handleCallAPIWithCurrentLocation()
        }
        Log.d(tag, "Location input fragment: onViewCreated finished")


    }

    private fun handleCallAPIWithCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PermissionChecker.PERMISSION_GRANTED
        ) {
            handleLocationAvailabilityRequest()
        } else {
            //check if should show message
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                Log.d(tag, "Trying to showing location dialog")
                if (context != null) {
                    val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
                    dialogBuilder.setTitle(R.string.need_gps_title)
                    dialogBuilder.setMessage(R.string.need_gps_reason)
                    dialogBuilder.setPositiveButton(
                        R.string.gps_dialog_ok,
                        DialogInterface.OnClickListener { dialog, id ->
                            Log.d(tag, "Location dialog: OK")
                            //Request permission
                            requestLocationPermission()

                        })
                    dialogBuilder.setNegativeButton(
                        R.string.gps_dialog_cancel,
                        DialogInterface.OnClickListener { dialog, id ->
                            Log.d(
                                tag,
                                "Location dialog: Cancel (Btn)"
                            )
                        })
                    dialogBuilder.setOnCancelListener {
                        Log.d(
                            tag,
                            "Location dialog: Cancel (cancel)"
                        )
                    }
                    dialogBuilder.setOnDismissListener {
                        Log.d(
                            tag,
                            "Location dialog: Cancel (dismiss)"
                        )
                    }
                    dialogBuilder.show();
                } else {
                    Log.w(tag, "Unable to show location dialog due to context being $context")
                }
            } else {
                Log.d(tag, "Not showing location dialog")
                //Request permission
                requestLocationPermission()
            }
        }
    }
    /**
     * Request permission for fine location. Check onRequestPermissionsResult for result. Request code is PERMISSION_FINE_LOCATION_REQUEST_CODE
     */
    private fun requestLocationPermission() {
        Log.d(tag, "Request permission for fine location")
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_FINE_LOCATION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_FINE_LOCATION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    handleLocationPermissionRequestResult(grantResults[0])
                } else {
                    Log.e(tag, "No location permission grant results")
                }
            }
        }
    }

    private fun handleLocationPermissionRequestResult(grantResult: Int) {
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            handleLocationPermissionGranted()
        } else {
            Log.w(tag, "Access to location not granted")
        }
    }

    private fun handleLocationPermissionGranted() {
        Log.d(tag, "Access to location granted")
        if (ContextCompat.checkSelfPermission( //Permission check needed for fusedLocationProvider not to complain
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PermissionChecker.PERMISSION_GRANTED
        ) {
            if (fusedLocationProvider != null) {
                handleLocationAvailabilityRequest()
            } else {
                Log.w(tag, "No fused locationProvider")
            }
        } else {
            Log.w(tag, "No location permission (This message should never be displayed here)")
        }
    }

    /**
     * Check if location services are enabled, and if yes, proceed with currentLocationRequest. Assumes location permission be granted.
     */
    private fun handleLocationAvailabilityRequest() {
        if (LocationManagerCompat.isLocationEnabled(context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager)) {
            Log.d(tag, "Location services available")
            handleCurrentLocationRequest()
        } else {
            Log.d(tag, "Location services not available")
            Log.d(tag, "Trying to showing location services dialog")
            if (context != null) {
                val dialogBuilder: AlertDialog.Builder =
                    AlertDialog.Builder(
                        requireContext()
                    )
                dialogBuilder.setTitle(R.string.need_ls_title)
                dialogBuilder.setMessage(R.string.need_ls_reason)
                dialogBuilder.setPositiveButton(
                    R.string.gps_dialog_ok,
                    DialogInterface.OnClickListener { dialog, id ->
                        Log.d(tag, "Location services dialog: OK")
                        requestUserEnableLocationServices()
                    })
                dialogBuilder.setNegativeButton(
                    R.string.gps_dialog_cancel,
                    DialogInterface.OnClickListener { dialog, id ->
                        Log.d(
                            tag,
                            "Location services dialog: Cancel (Btn)"
                        )
                    })
                dialogBuilder.setOnCancelListener {
                    Log.d(
                        tag,
                        "Location services dialog: Cancel (cancel)"
                    )
                }
                dialogBuilder.setOnDismissListener {
                    Log.d(
                        tag,
                        "Location services dialog: Cancel (dismiss)"
                    )
                }
                dialogBuilder.show()
            } else {
                Log.w(
                    tag,
                    "Unable to show location services dialog due to context being $context"
                )
            }
        }

    }

    /**
     * Send an intent to open location settings so hat the user can enable location services. Check onActivityResult for Results. RequestCode is LOCATION_INTENT_REQUEST_CODE
     */
    private fun requestUserEnableLocationServices() {
        //Send user to enable location services
        val locationIntent =
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivityForResult(locationIntent, LOCATION_INTENT_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(
            tag,
            "Intent finished. Request code: $requestCode Result code: $resultCode Intent: $data"
        )
        if (requestCode == LOCATION_INTENT_REQUEST_CODE) {
            handleLocationIntentFinish()
        }
    }

    private fun handleLocationIntentFinish() {
        Log.d(tag, "Location Intent finished")
        handleLocationAvailabilityRequest()
    }

    /**
     * Request current location, handle it and send api call. Assumes location permission to be granted.
     */
    private fun handleCurrentLocationRequest() {
        Log.d(tag, "Location services available")
        //Request location
        val locTask: Task<Location>? =
            fusedLocationProvider?.getCurrentLocation( //TODO: add idle spinner
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                null
            )
        locTask?.addOnCompleteListener(requireActivity(), OnCompleteListener {
            Log.d(tag, "Location request completed")
            val loc = it.result
            if (loc != null) {
                Log.d(tag, "Latitude: ${loc.latitude} Longitude: ${loc.longitude}")
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.INTERNET
                    ) == PermissionChecker.PERMISSION_GRANTED
                ) {
                    Log.d(tag, "Internet permission available")
                    val lat = loc.latitude
                    var lon = loc.longitude
                    var latString = lat.toString()
                    var lonString = lon.toString()
                    Log.d(tag, "Latitude is $lat, longitude is $lon");
                    Log.d(
                        tag,
                        "Latitude is $latString, longitude is $lonString (String(1))"
                    );
                    latString = latString.replace(',', '.')
                    lonString = lonString.replace(',', '.')
                    Log.d(
                        tag,
                        "Latitude is $latString, longitude is $lonString (String(2))"
                    );
                    var urlString = resources.getString(R.string.api_call_gps)
                    Log.d(tag, "Loaded api url $urlString")
                    urlString = String.format(
                        urlString,
                        URLEncoder.encode(latString, "UTF-8"),
                        URLEncoder.encode(lonString, "UTF-8")
                    )
                    Log.d(tag, "Api url with lat/lon $urlString")
                    var additonalParams = getParams()
                    var additionalParamsString = getParamString(additonalParams)
                    urlString += additionalParamsString
                    Log.d(tag, "Parameterized api url $urlString")
                    GlobalScope.launch(Dispatchers.IO) {
                        sendApiCallAndShowResult(urlString)
                    }
                } else {
                    Log.d(tag, "Internet permission not available")
                }
            } else {
                Log.w(tag, "No exact location found")
            }
        })
    }

    /**
     * Execute the API call and navigate to result fragment on success.
     */
    private fun sendApiCallAndShowResult(urlString: String) {
        var result: String? = null;
        try {
            result = doApiCall(urlString)
        } catch (e: Exception) {
            Log.e(tag, "API call failed ${e.message}")
            val message = "API call failed: ${e.message}"
            activity?.runOnUiThread {
                showLongToast(message)
            }

        }
        if (result != null) {
            val resultObject = JSONObject(result)
            shared?.apiResponseObject = resultObject

            activity?.runOnUiThread {
                navigateToResultFragment()
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

    private fun callByZipCode() {
        var countryCode = getCountryCodeFromSettings()
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
        GlobalScope.launch(Dispatchers.IO) {
            var result: String? = null;
            try {
                result = doApiCall(urlString)
            } catch (e: Exception) {
                Log.e(tag, "API call failed ${e.message}")
                val message = "API call failed: ${e.message}"
                activity?.runOnUiThread {
                    showLongToast(message)
                }

            }
            if (result != null) {
                val resultObject = JSONObject(result)
                shared?.apiResponseObject = resultObject

                activity?.runOnUiThread {
                    navigateToResultFragment()
                }
            }
        }
    }

    private fun callByCityName() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.INTERNET
            ) == PermissionChecker.PERMISSION_GRANTED
        ) {
            var countryCode = getCountryCodeFromSettings()
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
                        showLongToast(message)
                    }

                }
                if (result != null) {
                    val resultObject = JSONObject(result)
                    shared?.apiResponseObject = resultObject

                    activity?.runOnUiThread {
                        navigateToResultFragment()
                    }
                }
            }
        } else {
            Log.d(tag, "Internet access permission not granted")
            Toast.makeText(activity, "No internet access permission", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Trigger navigation flow to resultFragment
     */
    private fun navigateToResultFragment() {
        findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
    }

    /**
     * Show a message in a toast (Popup). Display duration is LENGTH_LONG
     */
    private fun showLongToast(message: String) {
        val duration = Toast.LENGTH_LONG
        val toast = Toast.makeText(context, message, duration)
        toast.show()
    }

    /**
     * Execute API call to endpoint (GET) and return result as string if successful.
     */
    private fun doApiCall(endpoint: String): String? {
        Log.d(tag, "Trying to call $endpoint")
        return doApiCall(URL(endpoint))
    }

    /**
     * Execute API call to endpoint (GET) and return result as string if successful.
     */
    private fun doApiCall(endpoint: URL): String? {
        Log.d(tag, "Trying to call $endpoint")
        val conn = endpoint.openConnection()
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

    /**
     * Get a string of additional URL parameterse
     */
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

    /**
     * Get additional parameters for openweatherapi
     */
    private fun getParams(): Map<String, String> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        val params: MutableMap<String, String>
        val apiKey = prefs.getString("api_key", null)
        val languageCode=prefs.getString("language_code",resources.getString(R.string.language_value))
        val units=prefs.getString("units",resources.getString(R.string.units_value))
        Log.d(tag, "API key is $apiKey")
        params = HashMap<String, String>()
        if (apiKey != null) {
            params[resources.getString(R.string.api_key_key)] = apiKey
        }
        if(languageCode!=null){
            params[resources.getString(R.string.language_key)] =
                languageCode
        }
        if(units!=null){
            params[resources.getString(R.string.units_key)] = units
        }
        return params
    }

    /**
     * Get zip code from UI
     */
    private fun getZipCode(): String {
        return view?.findViewById<TextView>(R.id.editTextZip)?.text.toString()
    }

    /**
     * Get city name from UI
     */
    private fun getCityName(): String {
        return view?.findViewById<TextView>(R.id.editTextCity)?.text.toString();
    }

    /**
     * Get country code from preferences.
     */
    private fun getCountryCodeFromSettings(): String {
        val prefs=PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString("country_code","DE") ?: ""
    }
}