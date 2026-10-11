object Solution {
  def countDistinct(nums: Array[Int], k: Int, p: Int): Int = {
    val distinctSubarrays = scala.collection.mutable.Set[List[Int]]()
    val n = nums.length

    for (i <- 0 until n) {
      var count = 0
      var currentSubarray = List[Int]()
      
      for (j <- i until n) {
        currentSubarray = currentSubarray :+ nums(j)
        
        if (nums(j) % p == 0) {
          count += 1
        }
        
        if (count <= k) {
          distinctSubarrays.add(currentSubarray)
        }
      }
    }
    distinctSubarrays.size
  }
}
