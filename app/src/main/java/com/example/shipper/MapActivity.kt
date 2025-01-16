package com.example.shipper

import OrderDetails
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.shipper.databinding.ActivityMapBinding
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.ceil

class MapActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var binding: ActivityMapBinding
    private lateinit var mapView: MapView
    private lateinit var shipperMarker: Marker
    private lateinit var customerMarker: Marker
    private lateinit var requestQueue: RequestQueue

    private var currentPolyline: Polyline? = null
    private var shipperLocation = GeoPoint(0.0, 0.0)
    private var customerLocation = GeoPoint(0.0, 0.0)

    private lateinit var tvDistance: TextView
    private lateinit var tvEstimatedTime: TextView

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        Configuration.getInstance().userAgentValue = packageName

        mapView = binding.mapView
        mapView.setMultiTouchControls(true)

        tvDistance = binding.tvDistance
        tvEstimatedTime = binding.tvEstimatedTime
        requestQueue = Volley.newRequestQueue(this)

        shipperMarker = Marker(mapView)
        customerMarker = Marker(mapView)

        fetchCustomerLocation()
        startLocationUpdates()

        binding.btnLocation.setOnClickListener {
            zoomToShipperLocation()
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1001
            )
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000 // Cập nhật mỗi 5 giây
        ).build()

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation
                if (location != null) {
                    shipperLocation = GeoPoint(location.latitude, location.longitude)
                    updateShipperMarker(shipperLocation)
                    requestRoute(shipperLocation, customerLocation)
                    sendLocationToFirebase(shipperLocation)
                }
            }
        }, mainLooper)
    }

    private fun sendLocationToFirebase(location: GeoPoint) {
        val shipperId = auth.currentUser?.uid.orEmpty()
        if (shipperId.isNotEmpty()) {
            val shipperLocationData = mapOf(
                "latitude" to location.latitude,
                "longitude" to location.longitude
            )
            databaseReference.child("shippers").child(shipperId).updateChildren(shipperLocationData)
        }
    }

    private fun fetchCustomerLocation() {
        val orderDetails = intent.getSerializableExtra("selectedOrder") as? OrderDetails
        if (orderDetails != null) {
            customerLocation = GeoPoint(orderDetails.latitude, orderDetails.longitude)
            setupCustomerMarker(customerLocation)
        } else {
            Toast.makeText(this, "Order details not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCustomerMarker(location: GeoPoint) {
        customerMarker.position = location
        customerMarker.title = "Customer Location"
        customerMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        val iconDrawable = getResizedDrawable(R.drawable.img_customer_location, 80, 80)
        if (iconDrawable != null) {
            customerMarker.icon = iconDrawable
        } else {
            customerMarker.icon = ContextCompat.getDrawable(this, R.drawable.img_customer_location)
        }

        mapView.overlays.add(customerMarker)
        mapView.controller.setCenter(location)
    }

    private fun updateShipperMarker(location: GeoPoint) {
        shipperMarker.position = location
        shipperMarker.title = "Shipper's Current Location"

        val iconDrawable = getResizedDrawable(R.drawable.img_bike, 80, 80)
        if (iconDrawable != null) {
            shipperMarker.icon = iconDrawable
        } else {
            shipperMarker.icon = ContextCompat.getDrawable(this, R.drawable.img_bike)
        }

        if (!mapView.overlays.contains(shipperMarker)) {
            mapView.overlays.add(shipperMarker)
        }

        mapView.invalidate()
    }

    private fun requestRoute(shipperLocation: GeoPoint, customerLocation: GeoPoint) {
        if (shipperLocation.latitude == 0.0 || customerLocation.latitude == 0.0) return

        val url = "https://router.project-osrm.org/route/v1/driving/" +
                "${shipperLocation.longitude},${shipperLocation.latitude};" +
                "${customerLocation.longitude},${customerLocation.latitude}?overview=full&geometries=geojson"

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener<JSONObject> { response ->
                val routes = response.getJSONArray("routes")
                if (routes.length() > 0) {
                    val route = routes.getJSONObject(0)
                    val distance = route.getDouble("distance") / 1000
                    val duration = route.getDouble("duration") / 60

                    tvDistance.text = String.format("%.2f km", distance)
                    tvEstimatedTime.text = String.format("~%d phút", ceil(duration).toInt())

                    val geometry = route.getJSONObject("geometry")
                    val coordinates = geometry.getJSONArray("coordinates")
                    drawRoute(coordinates)
                }
            },
            Response.ErrorListener {
                Toast.makeText(this, "Failed to fetch route", Toast.LENGTH_SHORT).show()
            }
        )

        requestQueue.add(jsonObjectRequest)
    }

    private fun drawRoute(coordinates: org.json.JSONArray) {
        currentPolyline?.let { mapView.overlays.remove(it) }

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
        polyline.outlinePaint.color = resources.getColor(android.R.color.holo_blue_dark, null)
        polyline.outlinePaint.strokeWidth = 10f

        mapView.overlays.add(polyline)
        currentPolyline = polyline
        mapView.invalidate()
    }

    private fun zoomToShipperLocation() {
        if (shipperLocation.latitude != 0.0 && shipperLocation.longitude != 0.0) {
            mapView.controller.setZoom(18.0)
            mapView.controller.animateTo(shipperLocation)
        } else {
            Toast.makeText(this, "Unable to determine your location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getResizedDrawable(resourceId: Int, width: Int, height: Int): Drawable? {
        val bitmap = BitmapFactory.decodeResource(resources, resourceId)
        return bitmap?.let {
            val resizedBitmap = Bitmap.createScaledBitmap(it, width, height, true)
            BitmapDrawable(resources, resizedBitmap)
        }
    }
}
