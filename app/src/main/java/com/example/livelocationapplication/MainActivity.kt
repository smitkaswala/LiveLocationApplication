package com.example.livelocationapplication


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.Builder.IMPLICIT_MIN_UPDATE_INTERVAL
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.ktx.androidParameters
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity() , OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var currentLocationMarker: Marker? = null
    private var userLocationMarker: Marker? = null
    private var currentLocation: Location? = null
    private var firstTimeFlag = true
    private var crtLocation = "location"
    private var userLocation = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val supportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        supportMapFragment.getMapAsync(this)



        Firebase.dynamicLinks.getDynamicLink(intent).addOnSuccessListener {
            if (it != null){
                userLocation = it.link?.getQueryParameter("locationId").toString()

                val dbReference: DatabaseReference = Firebase.database.reference
                dbReference.addValueEventListener(locationListener)

            }
        }

        val button = findViewById<Button>(R.id.btn_find_location)
        button.setOnClickListener {
            onLocationShare(crtLocation)
        }

    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap?.mapType = GoogleMap.MAP_TYPE_HYBRID
    }

    override fun onResume() {
        super.onResume()
        if (isGooglePlayServicesAvailable()) {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            startCurrentLocationUpdates()
        }
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val status = googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (ConnectionResult.SUCCESS == status) return true else {
            if (googleApiAvailability.isUserResolvableError(status)) Toast.makeText(
                this,
                "Please Install google play services to use this application",
                Toast.LENGTH_LONG
            ).show()
        }
        return false
    }

    private fun startCurrentLocationUpdates() {
        /*val locationRequest: LocationRequest = LocationRequest.create()
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest.setInterval(5000)*/

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .apply {
                setWaitForAccurateLocation(false)
                setMinUpdateIntervalMillis(IMPLICIT_MIN_UPDATE_INTERVAL)
                setMaxUpdateDelayMillis(5000)
            }.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION),
                    555
                )
                return
            }
        }
        fusedLocationProviderClient?.requestLocationUpdates(
            locationRequest,
            mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            if (locationResult.lastLocation == null) return
            currentLocation = locationResult.lastLocation
            if (firstTimeFlag && googleMap != null) {
                currentLocation?.let { animateCamera(it) }
                firstTimeFlag = false
            }
            currentLocation?.let { showMarker(it) }
        }
    }

    private fun animateCamera(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        googleMap?.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                getCameraPositionWithBearing(
                    latLng
                )
            )
        )
    }

    private fun showMarker(currentLocation: Location) {
        val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
        /*if (currentLocationMarker == null) currentLocationMarker = googleMap?.addMarker(
            MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker()).position(latLng)
        ) else MarkerAnimation.animateMarkerToGB(
            currentLocationMarker,
            latLng,
            LatLngInterpolator.Spherical()
        )*/

        googleMap?.isMyLocationEnabled = true

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val database: FirebaseDatabase = FirebaseDatabase.getInstance()
        val ref: DatabaseReference = database.getReference(crtLocation)
        ref.setValue(latLng)
    }

    private fun getCameraPositionWithBearing(latLng: LatLng): CameraPosition {
        return CameraPosition.Builder().target(latLng).zoom(16f).build()
    }


    private val locationListener = object : ValueEventListener {
//             @SuppressLint("LongLogTag")
        @SuppressLint("UseCompatLoadingForDrawables")
        override fun onDataChange(snapshot: DataSnapshot) {
            if(snapshot.exists()){
                val location = snapshot.child(userLocation).getValue(LocationInfo::class.java)
                val locationLat = location?.latitude
                val locationLong = location?.longitude

                if (locationLat != null && locationLong!= null) {

                    val latLng = LatLng(locationLat, locationLong)

                    Picasso.get().load("https://s3.eu-west-1.amazonaws.com/easyday-bucket/profile_images/cropped7635074666646685692.jpg").into(object : com.squareup.picasso.Target {
                        override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {

                            val marker: View = (getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.custom_marker_layout, null)

                            val markerImage = marker.findViewById<View>(R.id.user_dp) as CircleImageView
                            markerImage.setImageBitmap(bitmap)
                            val textName = marker.findViewById<View>(R.id.name) as TextView
                            textName.text = "•"

                            val displayMetrics = DisplayMetrics()
                            @Suppress("DEPRECATION")
                            (this@MainActivity as Activity).windowManager.defaultDisplay.getMetrics(displayMetrics)
                            marker.layoutParams = ViewGroup.LayoutParams(52, ViewGroup.LayoutParams.WRAP_CONTENT)
                            marker.measure(displayMetrics.widthPixels, displayMetrics.heightPixels)
                            marker.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)
                            @Suppress("DEPRECATION")
                            marker.buildDrawingCache()
                            val bitmapIcon = Bitmap.createBitmap(
                                marker.measuredWidth,
                                marker.measuredHeight,
                                Bitmap.Config.ARGB_8888
                            )
                            val canvas = Canvas(bitmapIcon)
                            marker.draw(canvas)

                            if (userLocationMarker == null) userLocationMarker = googleMap?.addMarker(
                                MarkerOptions().icon(bitmapIcon?.let {
                                    BitmapDescriptorFactory.fromBitmap(
                                        it
                                    )
                                }).position(latLng)
                            ) else MarkerAnimation.animateMarkerToGB(
                                userLocationMarker,
                                latLng,
                                LatLngInterpolator.Spherical()
                            )

                        }

                        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

                        override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {}
                        
                    })

                   /* if (userLocationMarker == null) userLocationMarker = googleMap?.addMarker(
                        MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)).position(latLng)
                    ) else MarkerAnimation.animateMarkerToGB(
                        userLocationMarker,
                        latLng,
                        LatLngInterpolator.Spherical()
                    )*/

                }

            }

        }
        // show this toast if there is an error while reading from the database
        override fun onCancelled(error: DatabaseError) {
            Toast.makeText(applicationContext, "Could not read from database", Toast.LENGTH_LONG).show()
        }

    }

    private fun onLocationShare(key: String?) {

        val url = "https://livelocationapplication.page.link/shareLocation?locationId=${key}"

        generateURL(url.toUri()) {
            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(Intent.EXTRA_SUBJECT, "Sharing URL")
            i.putExtra(Intent.EXTRA_TEXT, it)
            startActivity(Intent.createChooser(i, "Share URL"))
        }

    }

    private fun generateURL(
        generateURI : Uri,
        getShareLink : (String) -> Unit = {}
    ){
        val shareLink = FirebaseDynamicLinks.getInstance().createDynamicLink().run {
            link = generateURI
            domainUriPrefix = "https://livelocationapplication.page.link"

            androidParameters {
                build()
            }

            buildDynamicLink()

        }

        getShareLink.invoke(shareLink.uri.toString())

    }

}