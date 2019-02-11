import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LayerSceneProperties;
import com.esri.arcgisruntime.mapping.view.SceneView;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class SimpleSurfaceBuilder extends Application {

  private GraphicsOverlay graphicsOverlay;
  private SceneView sceneView;
  private static final String ELEVATION_IMAGE_SERVICE =
          "http://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer";

  @Override
  public void start(Stage stage) {

    try {

      // create stack pane and JavaFX app scene
      StackPane stackPane = new StackPane();
      Scene fxScene = new Scene(stackPane);

      // set title, size, and add JavaFX scene to stage
      stage.setTitle("Surface Builder");
      stage.setWidth(800);
      stage.setHeight(700);
      stage.setScene(fxScene);
      stage.show();

      // create a scene and add a basemap to it
      ArcGISScene scene = new ArcGISScene();
      scene.setBasemap(Basemap.createImagery());

      // add the SceneView to the stack pane
      sceneView = new SceneView();
      sceneView.setArcGISScene(scene);
      stackPane.getChildren().addAll(sceneView);

      // add base surface for elevation data
      Surface surface = new Surface();
      surface.getElevationSources().add(new ArcGISTiledElevationSource(ELEVATION_IMAGE_SERVICE));
      scene.setBaseSurface(surface);

      // initial position in Edinburgh
      Camera camera = new Camera(55.952486, -3.163775, 1800, 0.0, 0.0, 0.0);
      sceneView.setViewpointCamera(camera);

      // temp graphics overlay
      graphicsOverlay = new GraphicsOverlay();
      graphicsOverlay.getSceneProperties().setSurfacePlacement(LayerSceneProperties.SurfacePlacement.RELATIVE);
      sceneView.getGraphicsOverlays().add(graphicsOverlay);

      // create an outline for all rock types
      SimpleLineSymbol outlineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFFFFFFFF, 1);

      // SANDSTONE
      // create a symbol fill for sandstone
      SimpleFillSymbol sandFillSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, 0xFF005000,
              outlineSymbol);
      // define start point for sandstone layer to build from
      Point startPointSandstone = new Point(-352239.155088779, 7548867.338044916,0, SpatialReferences.getWebMercator());
      // make sandstone surface and store locally when button is pressed
      makeSurface(startPointSandstone, 500, 500, 16, 10, sandFillSymbol);

      // MUDSTONE
      // create a symbol fill for mudstone
      SimpleFillSymbol mudstoneFillSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, 0xFF500000,
              outlineSymbol);
      // define start point for mudstone layer to build from
      Point startPointMudstone = new Point (-352592.22360802896, 7548867.338044916, 0, SpatialReferences.getWebMercator());
      // make mudstone surface and store locally when button is pressed
      makeSurface(startPointMudstone, 1000, 500, 32, 20, mudstoneFillSymbol);

      // BASALT
      // create a symbol fill for basalt
      SimpleFillSymbol basaltFillSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, 0xFF000050,
              outlineSymbol);

      // define start point for basalt layer to build from
      Point startPointBasalt = new Point(-353102.0731684236, 7548867.338044916,0, SpatialReferences.getWebMercator());
      // make basalt surface and store locally when button is pressed
      makeSurface(startPointBasalt, 2000, 500, 64, 20, basaltFillSymbol);

    } catch (Exception e) {
      // on any error, display the stack trace.
      e.printStackTrace();
    }
  }

  private void makeSurface(Point startPoint,
                           int length,
                           int width,
                           double maxDepth,
                           double divisions,
                           SimpleFillSymbol fillSymbol
  ) {
    //symbols
    SimpleMarkerSymbol redDot = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, 0xFFFF0000, 10);
    SimpleMarkerSymbol blueDot = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, 0xFF0000FF, 10);

    // y=mx^2 + c (c = depth)
    double m = Math.pow(maxDepth,0.5);

    // increment of x depending on number of divisions.  Based on going from -2 to 2, width =4
    double increment = 4 / divisions;
    System.out.println("increment = " + increment);

    // work out the size of each increment in real space
    double stepSize = length / divisions;
    System.out.println("step size = " + stepSize);

    // work out the number of yIncrements given this step size
    double yIncremements = width / stepSize;
    System.out.println("yIncrements = " + yIncremements);


    // start position
    double startX = startPoint.getX();
    double startY = startPoint.getY();
    double currentX = startX;
    double currentY = startY;

    // 2D array for all the points of the surface
    Point surfacePoints[][] = new Point[(int)divisions+1][(int) yIncremements];

    // track x array position
    int xPos = 0;

    //make the points
    for (double x= -2; x<= 2; x=x+increment) {
      double depth = m * x * x - maxDepth;

      //hack it above the surface
      depth = depth + 100;

      System.out.println("x=" + x + " y=" + depth);

      // loop putting down along the y axis
      for (long y=0; y<yIncremements; y++) {
        // lay down a point at that depth
        Point surfacePoint = new Point(currentX,currentY, depth, SpatialReferences.getWebMercator());

        //store the point in the array
        //System.out.println("writing to array " + xPos + "," + (int) y);
        surfacePoints[xPos][(int)y] = surfacePoint;


        // move to the next y position
        currentY = currentY + stepSize;

      }

      //

      // reset the y position
      currentY = startY;

      // move the position for the next point
      currentX = currentX + stepSize;
      xPos++;
    }

    // work through the array to turn points into a set of polygons
    for (int xArrayPos=0; xArrayPos<divisions; xArrayPos++) {

      for (int yArrayPos = 0; yArrayPos < yIncremements - 1; yArrayPos++) {
        System.out.println("processing " + xArrayPos + "," + yArrayPos);

        // create a 2 point collections for polygons
        PointCollection points1 = new PointCollection(SpatialReferences.getWebMercator());
        PointCollection points2 = new PointCollection(SpatialReferences.getWebMercator());

        // points for 1st triangle
        points1.add(surfacePoints[xArrayPos][yArrayPos]);
        points1.add(surfacePoints[xArrayPos][yArrayPos + 1]);
        points1.add(surfacePoints[xArrayPos + 1][yArrayPos + 1]);

        // points for 2nd triangle
        points2.add(surfacePoints[xArrayPos][yArrayPos]);
        points2.add(surfacePoints[xArrayPos + 1][yArrayPos]);
        points2.add(surfacePoints[xArrayPos + 1][yArrayPos + 1]);

        // polygon for triangles
        Polygon triangle1 = new Polygon(points1);
        Polygon triangle2 = new Polygon(points2);

        // draw the polygons
        Graphic graphicTriangle1 = new Graphic(triangle1, fillSymbol);
        Graphic graphicTriangle2 = new Graphic(triangle2, fillSymbol);

        graphicsOverlay.getGraphics().add(graphicTriangle1);
        graphicsOverlay.getGraphics().add(graphicTriangle2);


      }
    }



  }





  /**
   * Stops and releases all resources used in application.
   */
  @Override
  public void stop() {

    if (sceneView != null) {
      sceneView.dispose();
    }
  }

  /**
   * Opens and runs application.
   *
   * @param args arguments passed to this application
   */
  public static void main(String[] args) {

    Application.launch(args);
  }



}
