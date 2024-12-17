package com.example.shipper

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import android.widget.Toast
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import android.view.MotionEvent

import com.example.shipper.databinding.ActivityMapBinding

class MapActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMapBinding
    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var shipperMarker: Marker
    private var isUserInteracting = false // Cờ kiểm tra người dùng có thao tác với bản đồ không
    private val shipperId = "shipper123" // ID của shipper để lưu trên Firebase
    private val customerLocation = GeoPoint(20.9947593, 105.8104609) // Địa chỉ khách hàng
    private var currentPolyline: Polyline? = null // Biến lưu trữ polyline hiện tại
    private lateinit var requestQueue: com.android.volley.RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // OSMDroid Configuration
        Configuration.getInstance().userAgentValue = packageName

        // MapView setup
        mapView = binding.mapView
        mapView.setMultiTouchControls(true)

        // Cài đặt marker cho khách hàng
        setupCustomerMarker()

        // Phát hiện thao tác trên bản đồ
        setupMapTouchListener()

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Set up the marker for the shipper
        shipperMarker = Marker(mapView)

        // Request location permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            startTracking()
        }

        // Handle Zoom Button

        binding.btnZoomToMyLocation.setOnClickListener {
            zoomToMyLocation()
        }

        // Setup requestQueue for OSRM route
        requestQueue = Volley.newRequestQueue(this)
    }

    private fun setupCustomerMarker() {
        val customerMarker = Marker(mapView)
        customerMarker.position = customerLocation
        customerMarker.title = "Customer Location"

        // Đặt biểu tượng img_customer_location
        val iconDrawable = getResizedDrawable(R.drawable.img_customer_location, 80, 80)
        if (iconDrawable != null) {
            customerMarker.icon = iconDrawable
            customerMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        } else {
            // Xử lý nếu không tải được ảnh hoặc sử dụng mặc định
            customerMarker.icon = resources.getDrawable(R.drawable.img_customer_location, null)
            customerMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }

        // Thêm customerMarker vào bản đồ
        mapView.overlays.add(customerMarker)
        mapView.invalidate() // Vẽ lại bản đồ
    }

    private fun setupMapTouchListener() {
        mapView.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    isUserInteracting = true // Người dùng bắt đầu thao tác
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isUserInteracting = false // Người dùng kết thúc thao tác
                }
            }
            false // Cho phép bản đồ xử lý các sự kiện khác
        }
    }

    private fun startTracking() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000 // Update location every 5 seconds
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation
                    if (location != null) {
                        updateLocationOnFirebase(location.latitude, location.longitude)

                        if (!isUserInteracting) {
                            updateShipperMarker(GeoPoint(location.latitude, location.longitude))
                            requestRoute(GeoPoint(location.latitude, location.longitude), customerLocation)
                        }
                    }
                }
            },
            mainLooper
        )
    }

    private fun updateLocationOnFirebase(latitude: Double, longitude: Double) {
        val location = mapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to System.currentTimeMillis()
        )
        database.child("shippers").child(shipperId).setValue(location)
    }

    private fun updateShipperMarker(location: GeoPoint) {
        shipperMarker.position = location
        shipperMarker.title = "Shipper's Current Location"

        val iconDrawable = getResizedDrawable(R.drawable.img_bike, 80, 80)

        if (iconDrawable != null) {
            shipperMarker.icon = iconDrawable
            shipperMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        } else {
            shipperMarker.icon = resources.getDrawable(R.drawable.img_bike, null)
            shipperMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }

        if (!mapView.overlays.contains(shipperMarker)) {
            mapView.overlays.add(shipperMarker)
        }

        if (!isUserInteracting) {
            mapView.controller.setCenter(location)
            mapView.invalidate()
        }
    }

    private fun getResizedDrawable(resourceId: Int, width: Int, height: Int): Drawable? {
        val bitmap = BitmapFactory.decodeResource(resources, resourceId) ?: return null
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        return BitmapDrawable(resources, resizedBitmap)
    }

    private fun requestRoute(shipperLocation: GeoPoint, customerLocation: GeoPoint) {
        val url = "https://router.project-osrm.org/route/v1/driving/" +
                "${shipperLocation.longitude},${shipperLocation.latitude};" +
                "${customerLocation.longitude},${customerLocation.latitude}?overview=full&geometries=geojson"

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val routes = response.getJSONArray("routes")
                if (routes.length() > 0) {
                    val geometry = routes.getJSONObject(0).getJSONObject("geometry")
                    val coordinates = geometry.getJSONArray("coordinates")
                    drawRoute(coordinates)
                }
            },
            {
                Toast.makeText(this, "Failed to fetch route", Toast.LENGTH_SHORT).show()
            }
        )

        requestQueue.add(jsonObjectRequest)
    }

    private fun drawRoute(coordinates: org.json.JSONArray) {
        currentPolyline?.let {
            mapView.overlays.remove(it)
        }

        val routePoints = mutableListOf<GeoPoint>()
        for (i in 0 until coordinates.length()) {
            val point = coordinates.getJSONArray(i)
            val longitude = point.getDouble(0)
            val latitude = point.getDouble(1)
            routePoints.add(GeoPoint(latitude, longitude))
        }

        val polyline = Polyline()
        polyline.setPoints(routePoints)
        polyline.title = "Route"
        polyline.outlinePaint.color = resources.getColor(android.R.color.holo_blue_dark)
        polyline.outlinePaint.strokeWidth = 10f

        mapView.overlays.add(polyline)
        currentPolyline = polyline
        mapView.invalidate()
    }

    private fun zoomToMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                mapView.controller.animateTo(geoPoint)
                mapView.controller.setZoom(18.0)
                mapView.invalidate()
            }
        }
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1001
    }
}
