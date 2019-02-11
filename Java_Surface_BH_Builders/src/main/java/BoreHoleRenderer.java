import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.*;
import com.esri.arcgisruntime.geometry.*;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;

import java.util.concurrent.ExecutionException;

public class BoreHoleRenderer {
  public GraphicsOverlay graphicsOverlay;

  public ServiceFeatureTable sandstoneFeatureTable;
  public ServiceFeatureTable mudstoneFeatureTable;
  public ServiceFeatureTable basaltFeatureTable;
  public ServiceFeatureTable boreholeFeatureTable;

  private SimpleLineSymbol sandLineSymbol;
  private SimpleLineSymbol mudLineSymbol;
  private SimpleLineSymbol basaltLineSymbol;

  public void renderBoreholes() {


    // make line symbols for bore holes
    sandLineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFF0000DD, 10); // blue
    mudLineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFFDD0000, 10); // red
    basaltLineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFF00DD00, 10); // green


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


}
