package com.esri.android.geoxray

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.esri.arcgisruntime.layers.ArcGISSceneLayer
import com.esri.arcgisruntime.mapping.ArcGISScene
import com.esri.arcgisruntime.mapping.view.ARCoreMotionDataSource
import com.esri.arcgisruntime.mapping.view.Camera
import com.esri.arcgisruntime.mapping.view.FirstPersonCameraController
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private var mUserRequestedInstall = true

    private var mSession: Session? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions()
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show()
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this)
            }
            finish()
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

                        setUpARScene()
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

    fun setUpARScene() {
        // Create scene without a basemap.  Background for scene content provided by device camera.
        sceneView.setScene(ArcGISScene());
        // Add San Diego scene layer.  This operational data will render on a video feed (eg from the device camera).
        sceneView.getScene().getOperationalLayers().add(ArcGISSceneLayer("https://tiles.arcgis.com/tiles/Imiq6naek6ZWdour/arcgis/rest/services/San_Diego_Textured_Buildings/SceneServer/layers/0"));
        // Enable AR for scene view.
        sceneView.setARModeEnabled(true);
        // Create an instance of Camera


        val cameraSanDiego = Camera(32.707, -117.157, 60.0, 270.0, 0.0, 0.0);
        val fpcController = FirstPersonCameraController();
        fpcController.setInitialPosition(cameraSanDiego);
        fpcController.setTranslationFactor(500.0);

        val arMotionSource = ARCoreMotionDataSource(arSceneView,this);
        fpcController.setDeviceMotionDataSource(arMotionSource);
        fpcController.setFramerate(FirstPersonCameraController.FirstPersonFramerate.BALANCED);
        sceneView.setCameraController(fpcController);

        // To update position and orientation of the camera with device sensors use:
        arMotionSource.startAll();
    }

}
