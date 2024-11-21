package com.example.mapslabdemo

import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.subscriber.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import java.nio.charset.StandardCharsets
import java.util.UUID

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private val pointsList = mutableListOf<LatLng>()
    private var mqttClient: Mqtt5AsyncClient? = null
    private lateinit var dbHelper: DatabaseHelper


    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StudentSpeedAdapter
    private val studentSpeedList = mutableListOf<StudentSpeedInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        dbHelper = DatabaseHelper(this)


        loadStoredLocations()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupRecyclerView()
        setupMqttClient()
    }

    private fun loadStoredLocations() {
        val storedLocations = dbHelper.getAllLocations()
        storedLocations.forEach { addMarkerAndPolyline(it) }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = StudentSpeedAdapter(studentSpeedList) { info ->
            // Handle "View More" button click
            Log.d("RecyclerView", "View More clicked for: ${info.studentId}")
        }
        recyclerView.adapter = adapter
    }

    private fun setupMqttClient() {
        if (!isNetworkAvailable()) {
            Log.e("MQTT", "No network available. MQTT client will not connect.")
            Toast.makeText(this, "No network available", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            mqttClient = Mqtt5Client.builder()
                .identifier(UUID.randomUUID().toString())
                .serverHost("broker.sundaebytestt.com") // Replace with your broker's address
                .serverPort(1883)                        // Default MQTT port
                .buildAsync()

            mqttClient?.connect()?.whenComplete { _, exception ->
                if (exception != null) {
                    Log.e("MQTT", "Error connecting to broker", exception)
                } else {
                    Log.d("MQTT", "Connected to broker")
                    subscribeToTopic()
                }
            }
        } catch (e: Exception) {
            Log.e("MQTT", "Exception during MQTT client setup", e)
        }
    }

    private fun subscribeToTopic() {
        mqttClient?.subscribeWith()
            ?.topicFilter("assignment/location")
            ?.callback { publish ->
                try {
                    val payload = publish.payloadAsBytes
                    if (payload != null) {
                        val message = String(payload, StandardCharsets.UTF_8)
                        Log.d("MQTT", "Received message: $message")
                        processLocationMessage(message)
                    } else {
                        Log.e("MQTT", "Received empty payload")
                    }
                } catch (e: Exception) {
                    Log.e("MQTT", "Error processing message", e)
                }
            }
            ?.send()
    }

    private fun processLocationMessage(message: String) {
        // Extract latitude, longitude, studentId, and timestamp from the received message
        val latRegex = "Latitude: ([+-]?\\d*\\.\\d+|\\d+)".toRegex()
        val lonRegex = "Longitude: ([+-]?\\d*\\.\\d+|\\d+)".toRegex()
        val studentIdRegex = "StudentID: (\\w+)".toRegex()
        val minSpeedRegex = "MinSpeed: (\\d+) km/h".toRegex()
        val maxSpeedRegex = "MaxSpeed: (\\d+) km/h".toRegex()

        val latitude = latRegex.find(message)?.groups?.get(1)?.value?.toDoubleOrNull()
        val longitude = lonRegex.find(message)?.groups?.get(1)?.value?.toDoubleOrNull()
        val studentId = studentIdRegex.find(message)?.groups?.get(1)?.value ?: "Unknown"
        val minSpeed = minSpeedRegex.find(message)?.groups?.get(1)?.value ?: "0"
        val maxSpeed = maxSpeedRegex.find(message)?.groups?.get(1)?.value ?: "0"

        if (latitude != null && longitude != null) {
            val newLocation = CustomMarkerPoints(
                id = 0,
                point = LatLng(latitude, longitude),
                studentId = studentId,
                timestamp = System.currentTimeMillis()
            )


            dbHelper.insertLocation(latitude, longitude, studentId, System.currentTimeMillis())


            val newSpeedInfo = StudentSpeedInfo(studentId, "$minSpeed km/h", "$maxSpeed km/h")
            runOnUiThread {
                addMarkerAndPolyline(newLocation)
                studentSpeedList.add(newSpeedInfo)
                adapter.notifyDataSetChanged()
            }
        } else {
            Log.e("MQTT", "Invalid location data: $message")
        }
    }

    private fun addMarkerAndPolyline(markerPoint: CustomMarkerPoints) {
        pointsList.add(markerPoint.point)
        mMap.addMarker(
            MarkerOptions()
                .position(markerPoint.point)
                .title("Student: ${markerPoint.studentId}")
                .snippet("Timestamp: ${markerPoint.timestamp}")
        )
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerPoint.point, 15f))


        if (pointsList.size > 1) {
            val polylineOptions = PolylineOptions()
                .addAll(pointsList)
                .color(Color.BLUE)
                .width(5f)

            mMap.addPolyline(polylineOptions)


            val bounds = LatLngBounds.builder()
            pointsList.forEach { bounds.include(it) }
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttClient?.disconnect()?.whenComplete { _, exception ->
            if (exception != null) {
                Log.e("MQTT", "Error disconnecting from broker", exception)
            } else {
                Log.d("MQTT", "Disconnected from broker")
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }
}
