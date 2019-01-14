package com.esri.android.geoxray

import com.esri.arcgisruntime.data.FeatureTable
import com.esri.arcgisruntime.data.QueryParameters
import com.esri.arcgisruntime.data.ServiceFeatureTable
import com.esri.arcgisruntime.geometry.*
import com.esri.arcgisruntime.mapping.view.Graphic
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay
import com.esri.arcgisruntime.symbology.SimpleLineSymbol
import java.util.concurrent.ExecutionException

class BoreHoleRenderer {
    var graphicsOverlay: GraphicsOverlay? = null

    var sandstoneFeatureTable: ServiceFeatureTable? = null
    var mudstoneFeatureTable: ServiceFeatureTable? = null
    var basaltFeatureTable: ServiceFeatureTable? = null
    var boreholeFeatureTable: ServiceFeatureTable? = null

    private var sandLineSymbol: SimpleLineSymbol? = null
    private var mudLineSymbol: SimpleLineSymbol? = null
    private var basaltLineSymbol: SimpleLineSymbol? = null

    fun renderBoreholes() {


        // make line symbols for bore holes
        sandLineSymbol = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, -0xffff23, 10f) // blue
        mudLineSymbol = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, -0x230000, 10f) // red
        basaltLineSymbol = SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, -0xff2300, 10f) // green


        // load the borehole feature table
        boreholeFeatureTable!!.loadAsync()
        // when loaded, get results
        boreholeFeatureTable!!.addDoneLoadingListener {

            val query = QueryParameters()
            // get everything
            query.whereClause = "1=1"

            val resultFuture = boreholeFeatureTable!!.queryFeaturesAsync(query)
            resultFuture.addDoneListener {

                try {

                    val result = resultFuture.get()

                    for (feature in result) {
                        println("Got result")
                        val boreholePosition = feature.geometry as Point

                        renderBorehole(boreholePosition)

                    }

                } catch (e: InterruptedException) {
                    e.printStackTrace()
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                }


            }

        }
    }

    private fun renderBorehole(boreHolePosition: Point) {

        var currentDepth = 100.0

        // query the sandstone depth
        val sandStoneDepth = getGeologyDepth(boreHolePosition, sandstoneFeatureTable!!)

        //draw sandstone line
        val sandPoints = PointCollection(SpatialReferences.getWebMercator())
        val topSandPint = Point(
                boreHolePosition.x,
                boreHolePosition.y,
                currentDepth,
                SpatialReferences.getWebMercator())

        val bottomSandPoint = Point(
                boreHolePosition.x,
                boreHolePosition.y,
                sandStoneDepth,
                SpatialReferences.getWebMercator())

        sandPoints.add(topSandPint)
        sandPoints.add(bottomSandPoint)

        val sandLine = Polyline(sandPoints)
        val sandGraphic = Graphic(sandLine, sandLineSymbol!!)
        graphicsOverlay!!.graphics.add(sandGraphic)

        currentDepth = sandStoneDepth


        // query the mudstone depth
        val mudStoneDepth = getGeologyDepth(boreHolePosition, mudstoneFeatureTable!!)

        //draw mudstone line
        val mudPoints = PointCollection(SpatialReferences.getWebMercator())
        val topMudPoint = Point(
                boreHolePosition.x,
                boreHolePosition.y,
                currentDepth,
                SpatialReferences.getWebMercator())

        val bottomMudPoint = Point(
                boreHolePosition.x,
                boreHolePosition.y,
                mudStoneDepth,
                SpatialReferences.getWebMercator())

        mudPoints.add(topMudPoint)
        mudPoints.add(bottomMudPoint)

        val mudLine = Polyline(mudPoints)

        val mudGraphic = Graphic(mudLine, mudLineSymbol!!)
        graphicsOverlay!!.graphics.add(mudGraphic)

        currentDepth = mudStoneDepth

        // query the basaltstone depth
        val basaltDepth = getGeologyDepth(boreHolePosition, basaltFeatureTable!!)

        //draw basalt line
        val basaltPoints = PointCollection(SpatialReferences.getWebMercator())
        val topBasaltPoint = Point(
                boreHolePosition.x,
                boreHolePosition.y,
                currentDepth,
                SpatialReferences.getWebMercator())

        val bottomBasaltPoint = Point(
                boreHolePosition.x,
                boreHolePosition.y,
                basaltDepth,
                SpatialReferences.getWebMercator())

        basaltPoints.add(topBasaltPoint)
        basaltPoints.add(bottomBasaltPoint)

        val basaltLine = Polyline(basaltPoints)

        val basaltGraphic = Graphic(basaltLine, basaltLineSymbol!!)
        graphicsOverlay!!.graphics.add(basaltGraphic)


    }

    // get depth of lithological layer from feature table
    private fun getGeologyDepth(locationPoint: Point, lithologyFeatureTable: FeatureTable): Double {

        var depth = 100.0

        // get intersection of point with surface
        val query = QueryParameters()
        query.geometry = locationPoint
        query.spatialRelationship = QueryParameters.SpatialRelationship.WITHIN

        val resultFuture = lithologyFeatureTable.queryFeaturesAsync(query)

        //resultFuture.addDoneListener(()-> {
        try {
            val result = resultFuture.get()

            // loop features (should be one)
            for (feature in result) {
                println("got a feature")
                val triangle = feature.geometry as Polygon

                val parts = triangle.parts

                for (part in parts) {
                    for (point in part.points) {
                        println("pt: x=" + point.x + " y=" + point.y + " z=" + point.z)
                        depth = point.z
                    }
                }
            }

        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        }

        //});

        println("Depth is $depth")
        return depth

    }


}
