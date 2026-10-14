// https://leetcode.com/problems/maximum-sum-obtained-of-any-permutation/description/$0
object Solution {
    def maxSumRangeQuery(nums: Array[Int], requests: Array[Array[Int]]): Int = {
        val n = nums.length
        val counts = new Array[Int](n + 1)
        
        // Step 1: Use a difference array to mark query ranges
        for (req <- requests) {
            counts(req(0)) += 1
            counts(req(1) + 1) -= 1
        }
        
        // Step 2: Calculate the frequency of each index using prefix sums
        var currentCount = 0
        val freq = new Array[Int](n)
        for (i <- 0 until n) {
            currentCount += counts(i)
            freq(i) = currentCount
        }
        
        // Step 3: Sort both nums and frequencies
        val sortedNums = nums.sorted
        val sortedFreq = freq.sorted
        
        // Step 4: Greedily multiply largest frequencies with largest numbers
        val mod = 1_000_000_007L
        var totalSum = 0L
        
        for (i <- 0 until n) {
            totalSum = (totalSum + sortedNums(i).toLong * sortedFreq(i)) % mod
        }
        
        totalSum.toInt
    }
}
