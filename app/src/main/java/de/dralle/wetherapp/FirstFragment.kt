package de.dralle.wetherapp

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
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
class FirstFragment : Fragment(), IUpdateListener {

    interface OnAPICallResultListener {
        fun onApiCallResult()
    }

    private var callback: OnAPICallResultListener? = null
    private var shared: SharedDataContainer? = null

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

        val btnCity = view.findViewById<Button>(R.id.btnSearchCity)
        val btnZip = view.findViewById<Button>(R.id.btnSearchZip)
        val btnGPS = view.findViewById<Button>(R.id.btnSearchGPS)
        val btnLocate=view.findViewById<Button>(R.id.btnLocate)

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
                findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
            })
        btnLocate.setOnClickListener {
            locateAndWriteGPS()
        }
        Log.i(tag, "test")
        val isDebugLoggable = Log.isLoggable(tag, Log.DEBUG)
        Log.e(tag, "Debug is loggable: $isDebugLoggable")
    }

    private fun locateAndWriteGPS() {
        TODO("Not yet implemented")
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
        if (context is MainActivity) {
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
        TODO("Not yet implemented")
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
    }

    private fun callByCityName() {
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