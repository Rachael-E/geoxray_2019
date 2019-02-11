import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.FeatureTable;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.*;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.layers.Layer;
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

public class BoreHoleBuilder extends Application {

  private GraphicsOverlay graphicsOverlay;
  private SceneView sceneView;
  private static final String ELEVATION_IMAGE_SERVICE =
      "http://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer";

  private ServiceFeatureTable sandstoneFeatureTable;
  private ServiceFeatureTable mudstoneFeatureTable;
  private ServiceFeatureTable basaltFeatureTable;
  private ServiceFeatureTable boreholeFeatureTable;

  private FeatureLayer sandstoneFeatureLayer;
  private FeatureLayer mudstoneFeatureLayer;
  private FeatureLayer basaltFeatureLayer;
  private FeatureLayer boreholeFeatureLayer;

  private SimpleLineSymbol sandLineSymbol;
  private SimpleLineSymbol mudLineSymbol;
  private SimpleLineSymbol basaltLineSymbol;

  //private double depth;

  private static final String SANDSTONE_FEATURE_LAYER_URL =
      "https://services1.arcgis.com/6677msI40mnLuuLr/arcgis/rest/services/GeoXRay_WFL1/FeatureServer/0";
  private static final String MUDSTONE_FEATURE_LAYER_URL =
      "https://services1.arcgis.com/6677msI40mnLuuLr/arcgis/rest/services/GeoXRay_WFL1/FeatureServer/1";
  private static final String BASALT_FEATURE_LAYER_URL =
      "https://services1.arcgis.com/6677msI40mnLuuLr/arcgis/rest/services/GeoXRay_WFL1/FeatureServer/2";
  private static final String BOREHOLE_FEATURE_LAYER_URL =
      "https://services1.arcgis.com/6677msI40mnLuuLr/arcgis/rest/services/GeoXRay_WFL1/FeatureServer/3";

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

      Basemap basemap = Basemap.createImageryWithLabels();

      for(Layer layer: basemap.getBaseLayers()) {
        layer.setOpacity(0.5f);
      }
      scene.setBasemap(basemap);

      // make line symbols for bore holes
      sandLineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFF0000DD, 10); // blue
      mudLineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFFDD0000, 10); // red
      basaltLineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFF00DD00, 10); // green

      // create service feature table
      sandstoneFeatureTable = new ServiceFeatureTable(SANDSTONE_FEATURE_LAYER_URL);
      mudstoneFeatureTable = new ServiceFeatureTable(MUDSTONE_FEATURE_LAYER_URL);
      basaltFeatureTable = new ServiceFeatureTable(BASALT_FEATURE_LAYER_URL);
      boreholeFeatureTable = new ServiceFeatureTable(BOREHOLE_FEATURE_LAYER_URL);

      // create a feature layer from tables
      sandstoneFeatureLayer = new FeatureLayer(sandstoneFeatureTable);
      sandstoneFeatureLayer.getSceneProperties().setSurfacePlacement(LayerSceneProperties.SurfacePlacement.RELATIVE);
      sandstoneFeatureLayer.setOpacity(0.5f);
      mudstoneFeatureLayer = new FeatureLayer(mudstoneFeatureTable);
      mudstoneFeatureLayer.getSceneProperties().setSurfacePlacement(LayerSceneProperties.SurfacePlacement.RELATIVE);
      mudstoneFeatureLayer.setOpacity(0.5f);
      basaltFeatureLayer = new FeatureLayer(basaltFeatureTable);
      basaltFeatureLayer.getSceneProperties().setSurfacePlacement(LayerSceneProperties.SurfacePlacement.RELATIVE);
      basaltFeatureLayer.setOpacity(0.5f);
      boreholeFeatureLayer = new FeatureLayer(boreholeFeatureTable);

      // add the feature layers to the scene
      scene.getOperationalLayers().add(basaltFeatureLayer);
      scene.getOperationalLayers().add(mudstoneFeatureLayer);
      scene.getOperationalLayers().add(sandstoneFeatureLayer);
      scene.getOperationalLayers().add(boreholeFeatureLayer);

      // add the SceneView to the stack pane
      sceneView = new SceneView();
      sceneView.setArcGISScene(scene);
      stackPane.getChildren().addAll(sceneView);


      // add base surface for elevation data
      Surface surface = new Surface();
      surface.getElevationSources().add(new ArcGISTiledElevationSource(ELEVATION_IMAGE_SERVICE));
      surface.getBackgroundGrid().setColor(0x55FFFFFF);
      surface.getBackgroundGrid().setGridLineWidth(0.5f);
      scene.setBaseSurface(surface);


      // initial position in Edinburgh
      Camera camera = new Camera(55.952486, -3.163775, 1800, 0.0, 0.0, 0.0);
      sceneView.setViewpointCamera(camera);

      // temp graphics overlay
      graphicsOverlay = new GraphicsOverlay();
      graphicsOverlay.getSceneProperties().setSurfacePlacement(LayerSceneProperties.SurfacePlacement.RELATIVE);
      sceneView.getGraphicsOverlays().add(graphicsOverlay);

      // set up class for drawing bore holes

      BoreHoleRenderer boreHoleRenderer = new BoreHoleRenderer();
      boreHoleRenderer.graphicsOverlay = graphicsOverlay;
      boreHoleRenderer.basaltFeatureTable = basaltFeatureTable;
      boreHoleRenderer.mudstoneFeatureTable = mudstoneFeatureTable;
      boreHoleRenderer.sandstoneFeatureTable = sandstoneFeatureTable;
      boreHoleRenderer.boreholeFeatureTable = boreholeFeatureTable;
      boreHoleRenderer.renderBoreholes();





      //drawBoreHoles();


    } catch (Exception e) {
      // on any error, display the stack trace.
      e.printStackTrace();
    }
  }


  private void drawBoreHoles () {

    // load the borehole feature table
    boreholeFeatureTable.loadAsync();
    // when loaded, get results
    boreholeFeatureTable.addDoneLoadingListener(() -> {

      QueryParameters query = new QueryParameters();
      // get everything
      query.setWhereClause("1=1");

      ListenableFuture<FeatureQueryResult> resultFuture = boreholeFeatureTable.queryFeaturesAsync(query);
      resultFuture.addDoneListener(() -> {

        try {

          FeatureQueryResult result = resultFuture.get();

          for (Feature feature : result) {
            System.out.println("Got result");
            Point boreholePosition = (Point) feature.getGeometry();

            renderBorehole(boreholePosition);

          }

        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (ExecutionException e) {
          e.printStackTrace();
        }

      });

    });

  }

  private void renderBorehole (Point boreHolePosition) {

    double currentDepth = 100.0;

    // query the sandstone depth
    double sandStoneDepth = getGeologyDepth(boreHolePosition, sandstoneFeatureTable);

    //draw sandstone line
    PointCollection sandPoints = new PointCollection(SpatialReferences.getWebMercator());
    Point topSandPint = new Point(
        boreHolePosition.getX(),
        boreHolePosition.getY(),
        currentDepth,
        SpatialReferences.getWebMercator());

    Point bottomSandPoint = new Point(
        boreHolePosition.getX(),
        boreHolePosition.getY(),
        sandStoneDepth,
        SpatialReferences.getWebMercator());

    sandPoints.add(topSandPint);
    sandPoints.add(bottomSandPoint);

    Polyline sandLine = new Polyline(sandPoints);
    Graphic sandGraphic = new Graphic(sandLine, sandLineSymbol);
    graphicsOverlay.getGraphics().add(sandGraphic);

    currentDepth = sandStoneDepth;


    // query the mudstone depth
    double mudStoneDepth = getGeologyDepth(boreHolePosition, mudstoneFeatureTable);

    //draw mudstone line
    PointCollection mudPoints = new PointCollection(SpatialReferences.getWebMercator());
    Point topMudPoint = new Point(
        boreHolePosition.getX(),
        boreHolePosition.getY(),
        currentDepth,
        SpatialReferences.getWebMercator());

    Point bottomMudPoint = new Point(
        boreHolePosition.getX(),
        boreHolePosition.getY(),
        mudStoneDepth,
        SpatialReferences.getWebMercator());

    mudPoints.add(topMudPoint);
    mudPoints.add(bottomMudPoint);

    Polyline mudLine = new Polyline(mudPoints);

    Graphic mudGraphic = new Graphic(mudLine, mudLineSymbol);
    graphicsOverlay.getGraphics().add(mudGraphic);

    currentDepth = mudStoneDepth;

    // query the basaltstone depth
    double basaltDepth = getGeologyDepth(boreHolePosition, basaltFeatureTable);

    //draw basalt line
    PointCollection basaltPoints = new PointCollection(SpatialReferences.getWebMercator());
    Point topBasaltPoint = new Point(
        boreHolePosition.getX(),
        boreHolePosition.getY(),
        currentDepth,
        SpatialReferences.getWebMercator());

    Point bottomBasaltPoint = new Point(
        boreHolePosition.getX(),
        boreHolePosition.getY(),
        basaltDepth,
        SpatialReferences.getWebMercator());

    basaltPoints.add(topBasaltPoint);
    basaltPoints.add(bottomBasaltPoint);

    Polyline basaltLine = new Polyline(basaltPoints);

    Graphic basaltGraphic = new Graphic(basaltLine, basaltLineSymbol);
    graphicsOverlay.getGraphics().add(basaltGraphic);


  }

  // get depth of lithological layer from feature table
  private double getGeologyDepth (Point locationPoint, FeatureTable lithologyFeatureTable) {

    double depth = 100.0;

    // get intersection of point with surface
    QueryParameters query = new QueryParameters();
    query.setGeometry(locationPoint);
    query.setSpatialRelationship(QueryParameters.SpatialRelationship.WITHIN);

    ListenableFuture<FeatureQueryResult> resultFuture = lithologyFeatureTable.queryFeaturesAsync(query);

    //resultFuture.addDoneListener(()-> {
      try {
        FeatureQueryResult result = resultFuture.get();

        // loop features (should be one)
        for (Feature feature: result) {
          System.out.println("got a feature");
          Polygon triangle = (Polygon) feature.getGeometry();

          ImmutablePartCollection parts = triangle.getParts();

          for (ImmutablePart part : parts) {
            for (Point point : part.getPoints()) {
              System.out.println("pt: x=" + point.getX() + " y=" + point.getY() + " z=" + point.getZ() );
              depth = point.getZ();
            }
          }
        }

      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
    //});

    System.out.println("Depth is " + depth);
    return depth;

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

