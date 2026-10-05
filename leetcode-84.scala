// https://leetcode.com/problems/largest-rectangle-in-histogram/description/
object Solution {
  def largestRectangleArea(heights: Array[Int]): Int = {
    val stack = new java.util.ArrayDeque[Int]()
    var maxArea = 0
    val n = heights.length

    for (i <- 0 to n) {
      val h = if (i == n) 0 else heights(i)
      while (!stack.isEmpty && heights(stack.peek()) > h) {
        val height = heights(stack.pop())
        val width = if (stack.isEmpty) i else i - stack.peek() - 1
        maxArea = math.max(maxArea, height * width)
      }
      stack.push(i)
    }

    maxArea
  }
}
