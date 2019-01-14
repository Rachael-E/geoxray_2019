package com.esri.android.geoxray

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.esri.arcgisruntime.UnitSystem
import com.esri.arcgisruntime.data.QueryParameters
import com.esri.arcgisruntime.data.ServiceFeatureTable
import com.esri.arcgisruntime.geoanalysis.LocationDistanceMeasurement
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.PointCollection
import com.esri.arcgisruntime.geometry.Polygon
import com.esri.arcgisruntime.geometry.Polyline
import com.esri.arcgisruntime.layers.FeatureLayer
import com.esri.arcgisruntime.mapping.ArcGISScene
import com.esri.arcgisruntime.mapping.view.*
import com.esri.arcgisruntime.symbology.SimpleLineSymbol
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.distance_info.*
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.ExecutionException


class MainActivity : AppCompatActivity() {
    private lateinit var mDepthGuideSymbol: SimpleLineSymbol
    private lateinit var mSurfacePointSymbol: SimpleMarkerSymbol

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

        mDepthGuideSymbol = SimpleLineSymbol(SimpleLineSymbol.Style.DASH, Color.LTGRAY, 1f)
        mSurfacePointSymbol = SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.RED, 5f)

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

        val graphicsOverlay0 = GraphicsOverlay()
        sceneView.graphicsOverlays.add(graphicsOverlay0)
        drawDepthGuidesFor(sandstoneFeatureTable, graphicsOverlay0)

        val mudstoneFeatureTable = ServiceFeatureTable(
                "https://services1.arcgis.com/6677msI40mnLuuLr/arcgis/rest/services/GeoXRay_WFL1/FeatureServer/1")
        val mudstoneFeatureLayer = FeatureLayer(mudstoneFeatureTable)
        mudstoneFeatureLayer.sceneProperties.surfacePlacement = LayerSceneProperties.SurfacePlacement.RELATIVE
        opLayers.add(mudstoneFeatureLayer)

        val graphicsOverlay1 = GraphicsOverlay()
        sceneView.graphicsOverlays.add(graphicsOverlay1)
        drawDepthGuidesFor(mudstoneFeatureTable, graphicsOverlay1)

        val basaltFeatureTable = ServiceFeatureTable(
                "https://services1.arcgis.com/6677msI40mnLuuLr/arcgis/rest/services/GeoXRay_WFL1/FeatureServer/2")
        val basaltFeatureLayer = FeatureLayer(basaltFeatureTable)
        basaltFeatureLayer.sceneProperties.surfacePlacement = LayerSceneProperties.SurfacePlacement.RELATIVE
        opLayers.add(basaltFeatureLayer)

        val graphicsOverlay2 = GraphicsOverlay()
        sceneView.graphicsOverlays.add(graphicsOverlay2)
        drawDepthGuidesFor(basaltFeatureTable, graphicsOverlay2)

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
        sceneView.setCameraController(fpcController);val distanceMeasurement = LocationDistanceMeasurement(Point(0.0, 0.0, 0.0),
                Point(1.0, 1.0, 1.0))
        distanceMeasurement.unitSystem = UnitSystem.METRIC
        val currDistanceTextView = findViewById<TextView>(R.id.currDistanceTextView)
        val twoDecPlaces = DecimalFormat("##.00")
        distanceMeasurement.addMeasurementChangedListener { measurementChangedEvent ->
            currDistanceTextView.text = twoDecPlaces.format(distanceMeasurement.directDistance.value).toString()
        }
        val analysisOverlay = AnalysisOverlay()
        analysisOverlay.isVisible = false
        sceneView.analysisOverlays.add(analysisOverlay)
        analysisOverlay.analyses.add(distanceMeasurement)

        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels

        val centerScreenPoint = android.graphics.Point(width / 2, height / 2)

        // set up distance measurement
        startDistanceButton.setOnClickListener { v ->

            // distance measurement task
            val distanceTimerTask = object : TimerTask() {
                override fun run() {
                    val cameraPoint = fpcController.initialPosition.location
                    val sightLinePoints = ArrayList<Point>()
                    sightLinePoints.add(Point(cameraPoint.x, cameraPoint.y, cameraPoint.z))
                    //Log.d(TAG, "Camera point: " + cameraPoint.getX() + ", " + cameraPoint.getY() + ", " + cameraPoint.getZ());
                    val lookAtPointFuture = sceneView!!.screenToLocationAsync(centerScreenPoint)
                    lookAtPointFuture.addDoneListener {
                        try {
                            val lookAtPoint = lookAtPointFuture.get()
                            sightLinePoints.add(Point(lookAtPoint.x, lookAtPoint.y, lookAtPoint.z))
                            //Log.d(TAG, "Look at point: " + lookAtPoint.getX() + ", " + lookAtPoint.getY() + ", " + lookAtPoint.getZ());

                            // update distance measurement start and end points
                            distanceMeasurement.startLocation = cameraPoint //TODO - BUG! This doesn't update!
                            distanceMeasurement.endLocation = lookAtPoint

                        } catch (e: InterruptedException) {
                            Log.e("MapView", "Error getting point on surface: " + e.message)
                        } catch (e: ExecutionException) {
                            Log.e("MapView", "Error getting point on surface: " + e.message)
                        }
                    }
                }
            }
            val distanceTimer = Timer()
            distanceTimer.scheduleAtFixedRate(distanceTimerTask, 50, 50)

            // identify task
            val identifyTimerTask = object : TimerTask() {
                override fun run() {
                    val identifyFuture = sceneView!!
                            .identifyLayersAsync(centerScreenPoint, 10.0, false)
                    identifyFuture.addDoneListener {
                        try {
                            val identifyLayerResultList = identifyFuture.get()
                            if (!identifyLayerResultList.isEmpty()) {
                                val identifyLayerResult = identifyLayerResultList[identifyLayerResultList.size - 1]
                                val layerName = identifyLayerResult.layerContent.name

                                runOnUiThread { currLayerTextView.text = layerName }
                            }
                        } catch (e: InterruptedException) {
                            Log.e("MapView", "Error in identify: " + e.message)
                        } catch (e: ExecutionException) {
                            Log.e("MapView", "Error in identify: " + e.message)
                        }
                    }
                }
            }
            val identifyTimer = Timer()
            identifyTimer.scheduleAtFixedRate(identifyTimerTask, 2000, 2000)
        }


        // To update position and orientation of the camera with device sensors use:
        arMotionSource.startAll();
    }

    private fun drawDepthGuidesFor(serviceFeatureTable: ServiceFeatureTable, graphicsOverlay: GraphicsOverlay) {

        // get all features in the scene view's spatial reference
        val parameters = QueryParameters()
        parameters.whereClause = "1=1"
        parameters.outSpatialReference = sceneView.spatialReference
        val sandstoneResult = serviceFeatureTable.queryFeaturesAsync(parameters)
        sandstoneResult.addDoneListener {
            try {
                for (feature in sandstoneResult.get()) {
                    val triangle = feature.geometry as Polygon
                    val triangleParts = triangle.parts
                    for (point in triangleParts.partsAsPoints) {
                        // create a point at the surface
                        val pointOnSurface = Point(point.x, point.y, 141.0)
                        val depthGuidePoints = ArrayList<Point>()
                        depthGuidePoints.add(point)
                        depthGuidePoints.add(pointOnSurface)
                        val surfacePointGraphic = Graphic(pointOnSurface, mSurfacePointSymbol!!)
                        graphicsOverlay.graphics.add(surfacePointGraphic)
                        graphicsOverlay.sceneProperties.surfacePlacement = LayerSceneProperties.SurfacePlacement.RELATIVE
                        val depthGuidePointPair = PointCollection(depthGuidePoints)
                        val depthGuidePolyline = Polyline(depthGuidePointPair)
                        graphicsOverlay.graphics.add(Graphic(depthGuidePolyline, mDepthGuideSymbol!!))
                    }
                }
            } catch (e: InterruptedException) {
                Log.e("MapView", "Error querying features: " + e.message)
            } catch (e: ExecutionException) {
                Log.e("MapView", "Error querying features: " + e.message)
            }
        }
    }

}
