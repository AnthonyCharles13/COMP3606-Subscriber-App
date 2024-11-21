package com.example.subscriber

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import java.util.UUID
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

class MainActivity : AppCompatActivity(), UserListAdapterInterface, OnMapReadyCallback {
    private var client: Mqtt5AsyncClient? = null
    private var userListAdapter: UserListAdapter? = null
    private var stuColors: MutableMap<String, Int> = mutableMapOf()
    private var usedColors: HashSet<Int> = HashSet()
    val dbHelper = DatabaseHelper(this, null)
    private lateinit var mMap: GoogleMap
    //private lateinit var Map2: GoogleMap

//    private lateinit var tvsum: TextView
//    private lateinit var tvavg: TextView
//    private lateinit var tvmax: TextView
//    private lateinit var tvmin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        client = Mqtt5Client.builder()
            .identifier(UUID.randomUUID().toString())
            .serverHost("broker-816029229.sundaebytestt.com")
            .serverPort(1883)
            .buildAsync()

        try {
            client?.connect()?.whenComplete { _, throwable ->
                if (throwable == null) {
                    client?.subscribeWith()
                        ?.topicFilter("assignment/location")
                        ?.callback { publish ->
                            val jsonData = String(publish.payloadAsBytes)
                            handleIncomingData(jsonData)
                        }
                        ?.send()
                    Toast.makeText(this, "Subscribed to topic", Toast.LENGTH_SHORT).show()
                } else {
                    // Handle connection failure
                    Toast.makeText(this, "Failed to connect to broker", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "An error occurred during subscription", Toast.LENGTH_SHORT).show()
        }

        userListAdapter = UserListAdapter(this)
        val rvuserList: RecyclerView = findViewById(R.id.rvusers)
        rvuserList.adapter = userListAdapter
        rvuserList.layoutManager = LinearLayoutManager(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

//        val mapFragment2 = supportFragmentManager
//            .findFragmentById(R.id.indimap) as SupportMapFragment
//        mapFragment2.getMapAsync(this)

//        tvsum = findViewById(R.id.tvsummary)
//        tvavg = findViewById(R.id.indiavg)
//        tvmax = findViewById(R.id.indimax)
//        tvmin = findViewById(R.id.indimin)
    }

    override fun onDestroy() {
        super.onDestroy()
        client?.disconnect()
    }

    private fun plotLastFiveMinutesData() {
        Log.d("MAP", "made it to plot point")
        try {
            stuColors.forEach { (stuid, color) ->
                val updates = dbHelper.getLast5MinutesUpdates(stuid)
                Log.d("MAP", "inside each")
                if (updates.isNotEmpty()) {
                    Log.d("MAP", "updates  not empty")
                    runOnUiThread {
                        plotStudentData(updates, color)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MAP", "Plot Error: ${e.message}")
        }
    }

    private fun plotStudentData(updates: List<LocationData>, color: Int) {
        Log.d("MAP", "inside plot student data")
        val latLngPoints = mutableListOf<LatLng>()

        try {
            updates.forEach { update ->
                val latLng =
                    LatLng(update.latitude, update.longitude)
                latLngPoints.add(latLng)
                Log.d("MAP", "inside each update")
                runOnUiThread {
                    mMap.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title("Student: ${update.stuid}, Speed: ${update.speed} km/h")
                            .icon(
                                BitmapDescriptorFactory.defaultMarker(
                                    color.toFloat().coerceIn(0.0f, 360.0f)
                                )
                            )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MAP", "Plot Student Data Error: ${e.message}")
        }

        val polylineOptions = PolylineOptions()
            .addAll(latLngPoints)
            .color(color)
            .width(5f)
            .geodesic(true)

        runOnUiThread {
            mMap.addPolyline(polylineOptions)
        }

        val bounds = LatLngBounds.builder()
        latLngPoints.forEach { bounds.include(it) }
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
    }


    private fun getRandomColorWithoutRepeat(): Int {
        var color: Int
        do {
            val r = (Math.random() * 256).toInt()
            val g = (Math.random() * 256).toInt()
            val b = (Math.random() * 256).toInt()
            color = Color.rgb(r, g, b)
        } while (usedColors.contains(color))
        usedColors.add(color)
        return color
    }

    private fun deserializeLocationData(jsonData: String): LocationData {
        val gson = com.google.gson.Gson()
        return gson.fromJson(jsonData, LocationData::class.java)
    }

    private fun handleIncomingData(jsonData: String) {
        try {
            val locationData = deserializeLocationData(jsonData)

            val logMessage = """
            Received Data:
            Student ID: ${locationData.stuid}
            Latitude: ${locationData.latitude}
            Longitude: ${locationData.longitude}
            Speed: ${locationData.speed} km/h
            Timestamp: ${locationData.dateTime}
            """.trimIndent()
            Log.d("SubscriberApp", logMessage)

            // Add the student ID and color to the map
            var color = getRandomColorWithoutRepeat()
            if (locationData.stuid in stuColors && locationData.stuid != "") {
                color = stuColors[locationData.stuid!!]!!
            }
            else {
                if (locationData.stuid != "") {
                    stuColors[locationData.stuid] = color
                }
            }
            if (locationData.stuid != "" || locationData.stuid != null) {
                Log.d("DBLOG", "before saving to db")
                dbHelper.insertLocationUpdate(locationData)
                Log.d("UpdateList", "before updating list")
                updateList(stuColors)
                //val userList : RecyclerView = findViewById(R.id.rvusers)
                //userList.visibility = View.VISIBLE

                Log.d("UserList", "before plotting points")
                plotLastFiveMinutesData()
            }

        } catch (e: Exception) {
            Log.e("SubscriberApp", "Error handling incoming data: ${e.message}")
        }
    }

    override fun onItemClick(studentID: String) {
        //showNextPage(studentID)
        Toast.makeText(this, "Selected peer IP: $studentID", Toast.LENGTH_SHORT).show()
    }

    private fun updateList(newAttendeesList: Map<String, Int>) {
        userListAdapter?.updateList(newAttendeesList)
    }

    override fun onMapReady(p0: GoogleMap) {
        mMap = p0
    }

//    private fun showNextPage(stuid: String) {
//        val indiPage: ConstraintLayout = findViewById(R.id.indiPage)
//        val allPage: ConstraintLayout = findViewById(R.id.allPage)
//        indiPage.visibility = View.VISIBLE
//        allPage.visibility = View.GONE
//        tvsum.text = "Summary of " + stuid
//        val speeds = dbHelper.getIndiMinMaxSpeeds(stuid)
//
//        if (speeds != null) {
//            val (minSpeed, maxSpeed) = speeds
//            tvmin.text = "Min Speed: %.4f km/h".format(minSpeed)
//            tvmax.text = "Max Speed: %.4f km/h".format(maxSpeed)
//            tvavg.text = "Average Speed: %.4f km/h".format((minSpeed + maxSpeed) / 2)
//        } else {
//            tvmin.text = "Min Speed: N/A"
//            tvmax.text = "Max Speed: N/A"
//            tvavg.text = "Average Speed: N/A"
//        }
//
//        plotAllData(stuid)
//        var allindiupdates = dbHelper.getAllUpdatesByStudentID(stuid)
//    }

//    private fun plotAllData(stuid: String) {
//        Log.d("MAP", "made it to plot point")
//        //val dbHelper = DatabaseHelper(this, null)
//        var allindiupdates = dbHelper.getAllUpdatesByStudentID(stuid)
//        val color = stuColors[stuid]
//        try {
//            // Iterate through all students in the map
////            stuColors.forEach { (stuid, color) ->
////                val updates = dbHelper.getLast5MinutesUpdates(stuid)
////                Log.d("MAP", "inside each")
//                if (allindiupdates.isNotEmpty()) {
//                    Log.d("MAP", "updates  not empty")
//                    // Plot points and draw polyline for this student
//                    runOnUiThread {
//                        plotAllStudentData(allindiupdates, color!!.toInt())
//                    }
//                }
//        } catch (e: Exception) {
//            Log.e("MAP", "Plot Error: ${e.message}")
//        }
//    }
//
//    // Helper method to plot points and draw polyline for a single student
//    private fun plotAllStudentData(updates: List<LocationData>, color: Int) {
//        Log.d("MAP", "inside plot student data")
//        val latLngPoints = mutableListOf<com.google.android.gms.maps.model.LatLng>()
//
//        try {
//            // Plot each update as a marker
//            updates.forEach { update ->
//                val latLng =
//                    com.google.android.gms.maps.model.LatLng(update.latitude, update.longitude)
//                latLngPoints.add(latLng)
//                Log.d("MAP", "inside each update")
//                runOnUiThread {
//                    Map2.addMarker(
//                        com.google.android.gms.maps.model.MarkerOptions()
//                            .position(latLng)
//                            .title("Student: ${update.stuid}, Speed: ${update.speed} km/h")
//                            .icon(
//                                com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
//                                    color.toFloat().coerceIn(0.0f, 360.0f)
//                                )
//                            )
//                    )
//                }
//            }
//        } catch (e: Exception) {
//            Log.e("MAP", "Plot Student Data Error: ${e.message}")
//        }
//
//        // Draw polyline connecting the points
//        val polylineOptions = com.google.android.gms.maps.model.PolylineOptions()
//            .addAll(latLngPoints)
//            .color(color)
//            .width(5f)
//            .geodesic(true)
//
//        runOnUiThread {
//            Map2.addPolyline(polylineOptions)
//        }
//
//        // Adjust map bounds
//        val bounds = com.google.android.gms.maps.model.LatLngBounds.builder()
//        latLngPoints.forEach { bounds.include(it) }
//        Map2.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
//    }

}