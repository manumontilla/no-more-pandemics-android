package com.net.mjmon.myapplication.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.net.mjmon.myapplication.R
import com.net.mjmon.myapplication.model.SymptomModel
import java.util.*


class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private lateinit var rv: RecyclerView
    private  var list_symtoms: MutableList<SymptomModel> = mutableListOf<SymptomModel>()
    private  var my_symtoms: MutableList<String> = mutableListOf<String>()
    val PERMISSION_ID = 42
    lateinit var mFusedLocationClient: FusedLocationProviderClient
    lateinit var localization: GeoPoint
    lateinit var today_now:java.util.Date
    lateinit var today_min:java.util.Date
    lateinit var today_max:java.util.Date



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        setContentView(R.layout.activity_dashboard)
        setSupportActionBar(findViewById(R.id.dashboard_toolbar))
        setTitle("Which are your symtoms today ?")

        val timeZone = TimeZone.getTimeZone("UTC")

        today_now = Calendar.getInstance(timeZone).time

        today_min = Calendar.getInstance(timeZone).time
        today_min.hours=0
        today_min.minutes=0
        today_min.seconds =0

        today_max = Calendar.getInstance(timeZone).time
        today_max.hours=23
        today_max.minutes=59
        today_max.seconds =59


       // loadSyntoms()
        loadMySyntomsToday()
        rv= findViewById(R.id.rv_dashboard)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getLastLocation()


    }


    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {

                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    var location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()
                    } else {
                        localization = GeoPoint(location.latitude,location.longitude)
                    }
                }
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        var mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient!!.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            var mLastLocation: Location = locationResult.lastLocation
            localization = GeoPoint(mLastLocation.latitude,mLastLocation.latitude)
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.dashboard_child, menu)
        return true
    }

    private fun isLocationEnabled(): Boolean {
        var locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            return true
        }
        return false
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_ID
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            }
        }
    }




    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.menu_change_password -> {
                startActivity(Intent(this, ChangePasswordActivity::class.java))
                true
            }
            R.id.menu_logout -> {
                auth.signOut()
                startActivity(Intent(this, LoiginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun deleteSyntoms() {


    }



    private  fun  loadSyntoms() {
        db.collection("sintomas")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    list_symtoms.add(SymptomModel(document.id,
                        document.data.get("nombre").toString(),
                        if(my_symtoms.contains(document.id)) true else false ))
                }
                rv.apply {
                    layoutManager = LinearLayoutManager(this@DashboardActivity)
                    adapter = SymptomAdapter(list_symtoms) { symptomModel: SymptomModel, i: Int ->
                        if(symptomModel.checked!!) {
                            //DELETE

                            var doc: DocumentReference = db.document("/sintomas/"+symptomModel.id.toString())

                             db.collection("sintomas_usuario")
                                .whereEqualTo("usuario",auth.currentUser?.uid.toString())
                                .whereEqualTo("sintoma",doc)
                                 .whereGreaterThanOrEqualTo("fecha", today_min)
                                 .whereLessThanOrEqualTo("fecha", today_max)
                                .get()
                                 .addOnSuccessListener { result ->
                                     for (document in result) {
                                         db.collection("sintomas_usuario").document(document.id)
                                             .delete()
                                         symptomModel.checked = false;
                                         this.adapter?.notifyDataSetChanged()
                                     }
                                 }
                                 .addOnFailureListener({result ->
                                     var a= result
                                 })
                        }else {
                            //INSERT


                            val symtom_user = hashMapOf(
                                "usuario" to auth.currentUser?.uid.toString(),
                                "idSintoma" to symptomModel.id,
                                "sintoma" to db.document("sintomas/" + symptomModel.id),
                                "nombre_sintoma" to symptomModel.name,
                                "localizacion" to localization,
                                "fecha" to today_now
                            )
                            db.collection("sintomas_usuario")
                                .add(symtom_user)
                                .addOnSuccessListener { documentReference ->
                                    symptomModel.checked=true;
                                    this.adapter?.notifyDataSetChanged()
                                }
                                .addOnFailureListener { e ->
                                    symptomModel.checked =false;
                                    this.adapter?.notifyDataSetChanged()
                                }
                        }

                    }
                }

            }
            .addOnFailureListener { exception ->

            }
    }

    private fun loadMySyntomsToday() {
        db.collection("sintomas_usuario")
            .whereEqualTo("usuario",auth.currentUser?.uid.toString())
            .whereGreaterThanOrEqualTo("fecha", today_min)
            .whereLessThanOrEqualTo("fecha", today_max)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    my_symtoms.add((document.get("sintoma") as DocumentReference).id)
                }
                loadSyntoms();
            }   .addOnFailureListener { e ->
                var a=e;

            }




    }



}