// https://leetcode.com/problems/trapping-rain-water-ii/
import scala.collection.mutable.PriorityQueue
object Solution {

  case class Cell(height: Int, r: Int, c: Int)

  def trapRainWater(heightMap: Array[Array[Int]]): Int = {
    if (heightMap == null || heightMap.length == 0 || heightMap(0).length == 0) return 0
    val m = heightMap.length
    val n = heightMap(0).length
    if (m < 3 || n < 3) return 0
    val visited = Array.ofDim[Boolean](m, n)
    implicit val cellOrdering: Ordering[Cell] = Ordering.by[Cell, Int](_.height).reverse
    val pq = PriorityQueue[Cell]()
    
    for (i <- 0 until m) {
      visited(i)(0) = true
      visited(i)(n - 1) = true
      pq.enqueue(Cell(heightMap(i)(0), i, 0))
      pq.enqueue(Cell(heightMap(i)(n - 1), i, n - 1))
    }
    
    for (j <- 0 until n) {
      visited(0)(j) = true
      visited(m - 1)(j) = true
      pq.enqueue(Cell(heightMap(0)(j), 0, j))
      pq.enqueue(Cell(heightMap(m - 1)(j), m - 1, j))
    }
    
    val directions = Array((-1, 0), (1, 0), (0, -1), (0, 1))
    var waterTrapped = 0
    
    while (pq.nonEmpty) {
      val current = pq.dequeue()
      for ((dr, dc) <- directions) {
        val nr = current.r + dr
        val nc = current.c + dc
        if (nr >= 0 && nr < m && nc >= 0 && nc < n && !visited(nr)(nc)) {
          visited(nr)(nc) = true
          if (heightMap(nr)(nc) < current.height) {
            waterTrapped += current.height - heightMap(nr)(nc)
          }
          val nextHeight = math.max(heightMap(nr)(nc), current.height)
          pq.enqueue(Cell(nextHeight, nr, nc))
        }
      }
    }
    
    waterTrapped
  }
}
