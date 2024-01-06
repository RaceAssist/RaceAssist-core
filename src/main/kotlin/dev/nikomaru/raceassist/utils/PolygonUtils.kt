package dev.nikomaru.raceassist.utils

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import java.awt.Polygon
import java.awt.geom.Line2D
import java.awt.geom.Point2D

object PolygonUtils {

    fun Polygon.getIntersection(line1: Line2D): Result<Point2D, IntersectionError> {
        val polygon = this
        val xPoints = polygon.xpoints
        val yPoints = polygon.ypoints
        val nPoints = polygon.npoints

        val intersection = arrayListOf<Point2D>()

        for (i in 0 until nPoints) {
            if (i <= nPoints - 2) {
                val line2 = Line2D.Double(
                    xPoints[i].toDouble(),
                    yPoints[i].toDouble(),
                    xPoints[i + 1].toDouble(),
                    yPoints[i + 1].toDouble()
                )
                val point = lineIntersection(line1, line2)
                if (point != null) {
                    intersection.add(point)
                }
            } else {
                val line2 = Line2D.Double(
                    xPoints[i].toDouble(),
                    yPoints[i].toDouble(),
                    xPoints[0].toDouble(),
                    yPoints[0].toDouble()
                )
                val point = lineIntersection(line1, line2)
                if (point != null) {
                    intersection.add(point)
                }
            }
        }

        if (intersection.isEmpty()) return Err(IntersectionError.NotIntersection)
        if (intersection.size >= 2) return Err(IntersectionError.MultipleIntersection)
        return Ok(intersection[0])
    }

    private fun lineIntersection(line1: Line2D, line2: Line2D): Point2D? {
        val x1 = line1.x1
        val y1 = line1.y1
        val x2 = line1.x2
        val y2 = line1.y2
        val x3 = line2.x1
        val y3 = line2.y1
        val x4 = line2.x2
        val y4 = line2.y2

        val d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
        if (d == 0.0) return null

        val xi = ((x3 - x4) * (x1 * y2 - y1 * x2) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d
        val yi = ((y3 - y4) * (x1 * y2 - y1 * x2) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d

        return Point2D.Double(xi, yi)
    }


}

enum class IntersectionError {
    NotIntersection,
    MultipleIntersection
}