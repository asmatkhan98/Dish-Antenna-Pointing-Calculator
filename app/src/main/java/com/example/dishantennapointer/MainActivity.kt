package com.example.dishantennapointer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.*
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlin.math.*

class MainActivity : AppCompatActivity(), SensorEventListener {
    // Constants
    companion object {
        private const val REQUEST_LOCATION = 1
        private const val ALPHA = 0.5f
        private const val ELEVATION_ALPHA = 0.5f
        private const val UPDATE_INTERVAL = 10
    }

    // UI Elements
    private lateinit var directionText: TextView
    private lateinit var elevationText: TextView
    private lateinit var resultText: TextView
    private lateinit var focalResult: TextView
    private lateinit var compassImage: ImageView
    private lateinit var latitudeInput: EditText
    private lateinit var longitudeInput: EditText
    private lateinit var satelliteInput: EditText
    private lateinit var diameterInput: EditText
    private lateinit var depthInput: EditText
    private lateinit var declinationInput: EditText
    private lateinit var calculateButton: Button
    private lateinit var calculateDepthButton: Button
    private lateinit var getLocationButton: Button
    private lateinit var directionSpinner: Spinner
    private lateinit var measurementSpinner: Spinner

    // Sensors and Location
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var magnetometer: Sensor
    private lateinit var locationManager: LocationManager
    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null
    private lateinit var vibrator: Vibrator

    // State Variables
    private var currentAzimuth = 0f
    private var displayedAzimuth = 0f
    private var currentElevation = 0f
    private var displayedElevation = 0f
    private var lastCompassUpdateTime = 0L
    private var lastElevationUpdateTime = 0L
    private var lastDirection = ""
    private var isVibrating = false
    private var measurementInches = true

    // Calculation Variables
    private var satelliteLongStr = ""
    private var directionStr = ""
    private var measurementStr = ""
    private var latitude = 0.0
    private var longitude = 0.0
    private var satelliteLongitude = 0.0
    private var declination = 2.75
    private var diameter = 0.0
    private var depth = 0.0
    private var trueAzimuth = 226.51
    private var magneticAzimuth = trueAzimuth - declination
    private var vibrationThreshhold = 2.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeUI()
        setupSensors()
        setupSpinners()
        setupListeners()
        refreshValues()
    }

    private fun initializeUI() {
        // Sensor displays
        directionText = findViewById(R.id.direction_text)
        elevationText = findViewById(R.id.pitch_text)
        compassImage = findViewById(R.id.compass_image)

        // Input fields
        latitudeInput = findViewById(R.id.latitudeInput)
        longitudeInput = findViewById(R.id.longitudeInput)
        satelliteInput = findViewById(R.id.satelliteInput)
        diameterInput = findViewById(R.id.diameterInput)
        depthInput = findViewById(R.id.depthInput)
        declinationInput = findViewById(R.id.declinationInput)

        // Buttons and results
        calculateButton = findViewById(R.id.calculateButton)
        calculateDepthButton = findViewById(R.id.calculateDepthButton)
        getLocationButton = findViewById(R.id.getLocationButton)
        resultText = findViewById(R.id.resultText)
        focalResult = findViewById(R.id.focalResult)

        // Spinners and system services
        directionSpinner = findViewById(R.id.directionSpinner)
        measurementSpinner = findViewById(R.id.measurementSpinner)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        
    }

    private fun setupSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)!!
        
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
    }

    private fun setupSpinners() {
        val directionAdapter = ArrayAdapter.createFromResource(
            this, R.array.direction_options, R.layout.custom_spinner_item
        )
        directionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        directionSpinner.adapter = directionAdapter

        val measurementAdapter = ArrayAdapter.createFromResource(
            this, R.array.measurement_options, R.layout.custom_spinner_item
        )
        measurementAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        measurementSpinner.adapter = measurementAdapter
    }

    private fun setupListeners() {
        calculateButton.setOnClickListener { calculateSatelliteAngles() }
        calculateDepthButton.setOnClickListener { calculateFocalPoint() }
        getLocationButton.setOnClickListener { requestLocation() }
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        directionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                refreshValues()
                directionStr = parent.getItemAtPosition(position).toString()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        measurementSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                refreshValues()
                measurementStr = parent.getItemAtPosition(position).toString()
                handleMeasurementChange()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun calculateSatelliteAngles() {
        try {
            refreshValues()
            if (directionStr == "W" && satelliteLongitude > 0) {
                satelliteLongitude *= -1
            }

            val deltaL = Math.toRadians(satelliteLongitude - longitude)
            val sinDeltaL = sin(deltaL)
            val cosDeltaL = cos(deltaL)
            val cosLatitude = cos(Math.toRadians(latitude))
            val sinLatitude = sin(Math.toRadians(latitude))

            trueAzimuth = Math.toDegrees(atan2(sinDeltaL, 
                cosLatitude * tan(Math.toRadians(0.0)) - sinLatitude * cosDeltaL))
            if (trueAzimuth < 0) trueAzimuth += 360

            val elevation = Math.toDegrees(atan((cosDeltaL * cosLatitude - 0.1512) / 
                sqrt(1 - (cosDeltaL * cosLatitude).pow(2))))
            val lnbSkew = Math.toDegrees(atan(sinDeltaL / tan(Math.toRadians(latitude))))
            
            magneticAzimuth = trueAzimuth - declination
            if (magneticAzimuth < 0) magneticAzimuth += 360

            resultText.text = String.format("%.2f° | %.2f°\n%.2f°\n%.2f°", 
                trueAzimuth, magneticAzimuth, elevation, -lnbSkew)
        } catch (e: Exception) {
            resultText.text = "Invalid input!\nPlease enter\nvalid numbers."
        }
    }

    private fun calculateFocalPoint() {
        try {
            refreshValues()
            val focal = diameter.pow(2) / (depth * 16)
            focalResult.text = String.format("Focal Point %.2f", focal)
        } catch (e: Exception) {
            focalResult.text = "Invalid input!\nPlease enter\nvalid numbers."
        }
    }

    private fun handleMeasurementChange() {
        try {
            val diameterTmp = diameterInput.text.toString().toDouble()
            val depthTmp = depthInput.text.toString().toDouble()

            if (measurementInches && measurementStr == "Centimetre") {
                measurementInches = false
                diameterInput.setText(String.format("%.2f", diameterTmp * 2.54))
                depthInput.setText(String.format("%.2f", depthTmp * 2.54))
            } else if (!measurementInches && measurementStr == "Inches") {
                measurementInches = true
                diameterInput.setText(String.format("%.2f", diameterTmp / 2.54))
                depthInput.setText(String.format("%.2f", depthTmp / 2.54))
            }

            refreshValues()
            calculateFocalPoint()
        } catch (e: NumberFormatException) {}
    }

    private fun refreshValues() {
        try {
            declination = declinationInput.text.toString().toDouble()
            latitude = latitudeInput.text.toString().toDouble()
            longitude = longitudeInput.text.toString().toDouble()
            satelliteLongitude = satelliteInput.text.toString().toDouble()
            satelliteLongStr = satelliteLongitude.toString()
            diameter = diameterInput.text.toString().toDouble()
            depth = depthInput.text.toString().toDouble()
        } catch (e: NumberFormatException) {}
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> gravity = event.values.clone()
            Sensor.TYPE_MAGNETIC_FIELD -> geomagnetic = event.values.clone()
        }

        if (gravity != null && geomagnetic != null) {
            val R = FloatArray(9)
            val I = FloatArray(9)

            if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(R, orientation)

                var azimuthInDegrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
                if (azimuthInDegrees < 0) azimuthInDegrees += 360

                val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                updateCompass(azimuthInDegrees)
                updateElevation(-pitch)
            }
        }
    }

    private fun updateCompass(newAzimuth: Float) {
        val filteredAzimuth = currentAzimuth + ALPHA * (newAzimuth - currentAzimuth)
        
        compassImage.animate()
            .rotation(-filteredAzimuth)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        currentAzimuth = filteredAzimuth

        val currentTime = System.currentTimeMillis()
        val newDirection = getCardinalDirection(filteredAzimuth)
        val shouldUpdate = (abs(filteredAzimuth - displayedAzimuth) > 1.0f || 
            newDirection != lastDirection) && 
            (currentTime - lastCompassUpdateTime) > UPDATE_INTERVAL

        if (shouldUpdate) {
            directionText.text = String.format("Azimuth: %s %.1f° ", newDirection, filteredAzimuth)
            displayedAzimuth = filteredAzimuth
            lastDirection = newDirection
            lastCompassUpdateTime = currentTime
        }

        var angleDifference = abs(filteredAzimuth - magneticAzimuth)
        angleDifference = min(angleDifference, 360 - angleDifference)

        if (angleDifference <= vibrationThreshhold) {
            startVibration()
        } else {
            stopVibration()
        }
    }

    private fun updateElevation(newElevation: Float) {
        currentElevation += ELEVATION_ALPHA * (newElevation - currentElevation)
        
        val currentTime = System.currentTimeMillis()
        val shouldUpdate = (abs(currentElevation - displayedElevation) > 0.2f && 
            currentTime - lastElevationUpdateTime > UPDATE_INTERVAL) || 
            lastElevationUpdateTime == 0L

        if (shouldUpdate) {
            elevationText.text = String.format("%.1f° Elevation", currentElevation)
            displayedElevation = currentElevation
            lastElevationUpdateTime = currentTime
        }
    }

    private fun startVibration() {
        if (!isVibrating && vibrator != null) {
            val effect = VibrationEffect.createWaveform(longArrayOf(0, 100, 100), 0)
            vibrator.vibrate(effect)
            isVibrating = true
        }
    }

    private fun stopVibration() {
        if (isVibrating && vibrator != null) {
            vibrator.cancel()
            isVibrating = false
        }
    }

    private fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION)
        } else {
            getLocation()
        }
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, 
                { location ->
                    latitudeInput.setText(String.format("%.6f", location.latitude))
                    longitudeInput.setText(String.format("%.6f", location.longitude))
                }, null)
        }
    }

    private fun getCardinalDirection(azimuth: Float): String {
        return when {
            azimuth >= 337.5 || azimuth < 22.5 -> "N"
            azimuth >= 22.5 && azimuth < 67.5 -> "NE"
            azimuth >= 67.5 && azimuth < 112.5 -> "E"
            azimuth >= 112.5 && azimuth < 157.5 -> "SE"
            azimuth >= 157.5 && azimuth < 202.5 -> "S"
            azimuth >= 202.5 && azimuth < 247.5 -> "SW"
            azimuth >= 247.5 && azimuth < 292.5 -> "W"
            azimuth >= 292.5 && azimuth < 337.5 -> "NW"
            else -> "N"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION && grantResults.isNotEmpty() 
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation()
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        stopVibration()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used in this app
    }
}