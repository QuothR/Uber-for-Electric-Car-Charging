package com.github.electroway.ui.main

import android.Manifest
import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.electroway.Application
import com.github.electroway.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.concurrent.Executors
import kotlin.random.Random

class MapFragment : Fragment(), OnMapReadyCallback {
    private var mapFragment: SupportMapFragment? = null
    private lateinit var googleMap: GoogleMap
    private lateinit var markerBitmap: BitmapDescriptor

    companion object {
        const val AUTOCOMPLETE_REQUEST_CODE = 1
    }

    private val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
    private val args: MapFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance()
        }
        parentFragmentManager.beginTransaction().replace(R.id.map, mapFragment!!).commit()
        mapFragment!!.getMapAsync(this)

        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
            .build(requireContext())

        view.findViewById<Toolbar>(R.id.toolbar).setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.home_button -> {
                    findNavController().navigate(R.id.action_mapFragment_to_homeFragment)
                }
                R.id.app_bar_search -> {
                    startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
                }
            }
            true
        }

        if (!Places.isInitialized()) {
            val context = requireContext()
            context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            ).apply {
                val key = metaData.getString("com.google.android.geo.API_KEY")!!
                Places.initialize(requireActivity().applicationContext, key)
            }
        }

        view.findViewById<FloatingActionButton>(R.id.mapLocationButton)
            .setOnClickListener { moveToCurrentLocation() }
    }

    private fun moveToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val locationManager =
                requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            var provider: String? = null
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                provider = LocationManager.GPS_PROVIDER
            } else if (locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
                )
            ) {
                provider = LocationManager.NETWORK_PROVIDER
            }
            if (provider != null) {
                locationManager.getCurrentLocation(
                    provider,
                    null,
                    requireContext().mainExecutor
                ) {
                    googleMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                it.latitude,
                                it.longitude
                            ), 20.0f
                        )
                    )
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), 44
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    data?.let {
                        val place = Autocomplete.getPlaceFromIntent(data)
                        googleMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                place.latLng,
                                20.0f
                            )
                        )
                    }
                }
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap

        if (args.addStation) {
            googleMap.setOnMapLongClickListener {
                val action = MapFragmentDirections.actionMapFragmentToHomeFragment(it)
                findNavController().navigate(action)
            }
            val homeItem =
                requireView().findViewById<Toolbar>(R.id.toolbar).menu.findItem(R.id.home_button)
            homeItem.isEnabled = false
            homeItem.isVisible = false
        } else {
            val session = (requireActivity().application as Application).session
            session.getStations {
                if (it != null) {
                    markerBitmap =
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.ic_baseline_ev_station
                        )!!.run {
                            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                            val bitmap =
                                Bitmap.createBitmap(
                                    intrinsicWidth,
                                    intrinsicHeight,
                                    Bitmap.Config.ARGB_8888
                                )
                            draw(Canvas(bitmap))
                            BitmapDescriptorFactory.fromBitmap(bitmap)
                        }
                    var markersPos = mutableMapOf<Marker, Int>()
                    for (i in 0 until it.length()) {
                        val obj = it.getJSONObject(i)
                        val address = obj.getString("address")
                        val latitude = obj.getDouble("latitude")
                        val longitude = obj.getDouble("longitude")
                        markersPos[googleMap.addMarker(
                            MarkerOptions().position(LatLng(latitude, longitude)).title(address)
                                .icon(markerBitmap)
                        )] = i
                    }

                    googleMap.setOnInfoWindowClickListener {
                        val action =
                            MapFragmentDirections.actionMapFragmentToReviewsFragment(markersPos[it]!!)
                        findNavController().navigate(action)
                    }
                }
            }
        }
    }
}