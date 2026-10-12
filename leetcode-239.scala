// https://leetcode.com/problems/sliding-window-maximum
import scala.collection.mutable.ArrayDeque

object Solution {
  def maxSlidingWindow(nums: Array[Int], k: Int): Array[Int] = {
        
    val result = new Array[Int](nums.length - k + 1)
    val deque = new ArrayDeque[Int]()
    var idx = 0

    for (i <- 0 until nums.length) {
      if (deque.nonEmpty && deque.head < i - k + 1) {
        deque.removeHead()
      }
      
      while (deque.nonEmpty && nums(deque.last) <= nums(i)) {
        deque.removeLast()
      }
      
      deque.append(i)
      
      if (i >= k - 1) {
        result(idx) = nums(deque.head)
        idx += 1
      }
    }
    
    result
  }
}
