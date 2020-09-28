package de.dralle.wetherapp

import android.os.Bundle
import android.util.Log
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.view.iterator
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import de.dralle.wetherapp.FirstFragment.OnAPICallResultListener
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(), OnAPICallResultListener {

    private var settingsActive:Boolean=false
    private var updatables :MutableSet<IUpdateListener> = HashSet<IUpdateListener>()
    private val shared:SharedDataContainer= SharedDataContainer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val toolbar=findViewById<Toolbar>(R.id.toolbar)

    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val settingsMenu=menu?.findItem(R.id.action_settings)
        settingsMenu?.isEnabled=settingsActive
        return super.onPrepareOptionsMenu(menu)
    }

    fun activateSettingsMenu(){
        settingsActive=true
        invalidateOptionsMenu()
    }
    fun deactivateSettingsMenu(){
        settingsActive=false
        invalidateOptionsMenu()
    }

    fun addUpdatableFragment(fragment:IUpdateListener){
        Log.d(javaClass.name,"Wiring updatable $fragment")
        fragment.setSharedDataContainer(shared)
        updatables.add(fragment)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                nav_host_fragment.findNavController().navigate(R.id.action_FirstFragment_to_mainSettingsFragment)
                return true;
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onApiCallResult() {
        for (updatable in updatables){
            updatable.update();
        }
    }
}