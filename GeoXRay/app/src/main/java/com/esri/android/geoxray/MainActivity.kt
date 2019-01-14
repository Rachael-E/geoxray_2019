package com.esri.android.geoxray

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.esri.arcgisruntime.data.ServiceFeatureTable
import com.esri.arcgisruntime.layers.FeatureLayer
import com.esri.arcgisruntime.mapping.ArcGISScene
import com.esri.arcgisruntime.mapping.view.*
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import kotlinx.android.synthetic.main.activity_main.*




class MainActivity : AppCompatActivity() {
    private val LOCATION_PERMISSION_CODE = 10

    private var mUserRequestedInstall = true

    private var mSession: Session? = null

    private var mInitialLocation: Location? = null

    // Define a listener that responds to location updates
    val mLocationListener = object : LocationListener {

        override fun onLocationChanged(location: Location) {
            // Called when a new location is found by the network location provider.
            if (mInitialLocation == null) {
                mInitialLocation = location
                setUpARScene(location)
            }
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        }

        override fun onProviderEnabled(provider: String) {
        }

        override fun onProviderDisabled(provider: String) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions()
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
        if (requestCode == CameraPermissionHelper.CAMERA_PERMISSION_CODE) {
            if (!CameraPermissionHelper.hasCameraPermission(this)) {
                Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                        .show()
                if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                    // Permission denied with checking "Do not ask again".
                    CameraPermissionHelper.launchPermissionSettings(this)
                }
                finish()
            } else {
//            setUpARScene()
                getLocation()
            }
            if (!CameraPermissionHelper.hasCameraPermission(this)) {
                Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                        .show()
                if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                    // Permission denied with checking "Do not ask again".
                    CameraPermissionHelper.launchPermissionSettings(this)
                }
                finish()
            } else {
//            setUpARScene()
                getLocation()
            }
        }
    }

    fun getLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_CODE)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            mInitialLocation = locationManager.getLastKnownLocation(Manifest.permission.ACCESS_FINE_LOCATION)

            // Register the listener with the Location Manager to receive location updates
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, mLocationListener)
        }
    }

    fun requestPermissions() {
        // ARCore requires camera permission to operate.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this);
            return;
        }

        // Make sure ARCore is installed and up to date.
        try {
            if (mSession == null) {
                when (ArCoreApk.getInstance().requestInstall(this, mUserRequestedInstall)) {
                    ArCoreApk.InstallStatus.INSTALLED -> {
                        // Success, create the AR session.
                        mSession = Session(this)

//                        setUpARScene()
                        getLocation()
                    }

                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        // Ensures next invocation of requestInstall() will either return
                        // INSTALLED or throw an exception.
                        mUserRequestedInstall = false
                        return
                    }
                }
            }
        } catch (e: UnavailableUserDeclinedInstallationException) {
            // Display an appropriate message to the user and return gracefully.
            Toast.makeText(this, "TODO: handle exception $e", Toast.LENGTH_LONG)
                    .show()
            return
        }
    }

    fun setUpARScene(location: Location) {
        // Create scene without a basemap.  Background for scene content provided by device camera.
        sceneView.setScene(ArcGISScene());

        val graphicsOverlay = GraphicsOverlay()
        graphicsOverlay.getSceneProperties().setSurfacePlacement(LayerSceneProperties.SurfacePlacement.RELATIVE)
        sceneView.graphicsOverlays.add(graphicsOverlay)

        val opLayers = sceneView.getScene().getOperationalLayers();

        val sandstoneFeatureTable = ServiceFeatureTable(
                        "https://services1.arcgis.com/6677msI40mnLuuLr/arcgis/rest/services/GeoXRay_WFL1/FeatureServer/0")
        val sandstoneFeatureLayer = FeatureLayer(sandstoneFeatureTable)
        sandstoneFeatureLayer.sceneProperties.surfacePlacement = LayerSceneProperties.SurfacePlacement.RELATIVE
        opLayers.add(sandstoneFeatureLayer)

        val mudstoneFeatureTable = ServiceFeatureTable(
                "https://services1.arcgis.com/6677msI40mnLuuLr/arcgis/rest/services/GeoXRay_WFL1/FeatureServer/1")
        val mudstoneFeatureLayer = FeatureLayer(mudstoneFeatureTable)
        mudstoneFeatureLayer.sceneProperties.surfacePlacement = LayerSceneProperties.SurfacePlacement.RELATIVE
        opLayers.add(mudstoneFeatureLayer)

        val basaltFeatureTable = ServiceFeatureTable(
                "https://services1.arcgis.com/6677msI40mnLuuLr/arcgis/rest/services/GeoXRay_WFL1/FeatureServer/2")
        val basaltFeatureLayer = FeatureLayer(basaltFeatureTable)
        basaltFeatureLayer.sceneProperties.surfacePlacement = LayerSceneProperties.SurfacePlacement.RELATIVE
        opLayers.add(basaltFeatureLayer)

        val boreholeFeatureTable = ServiceFeatureTable(
                "https://services1.arcgis.com/6677msI40mnLuuLr/arcgis/rest/services/GeoXRay_WFL1/FeatureServer/3")
        val boreholeFeatureLayer = FeatureLayer(boreholeFeatureTable)
        boreholeFeatureLayer.sceneProperties.surfacePlacement = LayerSceneProperties.SurfacePlacement.RELATIVE
        opLayers.add(boreholeFeatureLayer)

        // set up class for drawing bore holes

        val boreHoleRenderer = BoreHoleRenderer()
        boreHoleRenderer.graphicsOverlay = graphicsOverlay
        boreHoleRenderer.basaltFeatureTable = basaltFeatureTable
        boreHoleRenderer.mudstoneFeatureTable = mudstoneFeatureTable
        boreHoleRenderer.sandstoneFeatureTable = sandstoneFeatureTable
        boreHoleRenderer.boreholeFeatureTable = boreholeFeatureTable
        boreHoleRenderer.renderBoreholes()

        // Enable AR for scene view.
        sceneView.setARModeEnabled(true)

        // Create an instance of Camera
        var camera = Camera(55.952486, -3.163775, 100.0, 0.0, 0.0, 0.0);
        camera = Camera(location.latitude, location.longitude, location.altitude + 100.0,
                0.0, 0.0, 0.0);
//        if (mInitialLocation != null) {
//            camera = Camera(mInitialLocation!!.latitude, mInitialLocation!!.longitude, mInitialLocation!!.altitude + 100.0,
//                    0.0, 0.0, 0.0);
//        }
        val fpcController = FirstPersonCameraController();
        fpcController.setInitialPosition(camera);
//        fpcController.setTranslationFactor(500.0);

        val arMotionSource = ARCoreMotionDataSource(arSceneView,this);
        fpcController.setDeviceMotionDataSource(arMotionSource);
        fpcController.setFramerate(FirstPersonCameraController.FirstPersonFramerate.BALANCED);
        sceneView.setCameraController(fpcController);

        // To update position and orientation of the camera with device sensors use:
        arMotionSource.startAll();
    }

}
