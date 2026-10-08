// https://leetcode.com/problems/perfect-rectangle
object Solution {
  def isRectangleCover(rectangles: Array[Array[Int]]): Boolean = {
    val initialState = (Int.MaxValue, Int.MaxValue, Int.MinValue, Int.MinValue, 0L, Map[(Int, Int), Int]())

    val (minX, minY, maxX, maxY, totalArea, cornerCounts) = 
      rectangles.foldLeft(initialState) { case ((mnX, mnY, mxX, mxY, area, counts), r) =>
        val (x1, y1, x2, y2) = (r(0), r(1), r(2), r(3))
        val newArea = area + (x2.toLong - x1) * (y2.toLong - y1)
        val corners = List((x1, y1), (x2, y1), (x1, y2), (x2, y2))
        
        val updatedCounts = corners.foldLeft(counts) { (acc, c) =>
          acc.updated(c, acc.getOrElse(c, 0) + 1)
        }
        
        (mnX min x1, mnY min y1, mxX max x2, mxY max y2, newArea, updatedCounts)
      }

    val expectedArea = (maxX.toLong - minX) * (maxY.toLong - minY)

    if (expectedArea != totalArea) {
      false
    } else {
      val expectedCorners = Set((minX, minY), (maxX, minY), (minX, maxY), (maxX, maxY))
      cornerCounts.forall { case (corner, count) =>
        if (expectedCorners.contains(corner)) count % 2 == 1
        else count % 2 == 0
      }
    }
  }
}
