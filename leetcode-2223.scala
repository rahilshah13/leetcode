/*
    https://leetcode.com/problems/sum-of-scores-of-built-strings/$0
    The Z-algorithm computes the longest common prefix between the string and each suffix in O(n) time.
    https://www.geeksforgeeks.org/dsa/z-algorithm-linear-time-pattern-searching-algorithm/$0
*/
object Solution {
    def sumScores(s: String): Long = {
        val n = s.length
        val z = new Array[Int](n)
        z(0) = n
        
        var l = 0
        var r = 0
        var totalSum: Long = n
        
        for (i <- 1 until n) {
            if (i <= r) {
                z(i) = math.min(r - i + 1, z(i - l))
            }
            while (i + z(i) < n && s(z(i)) == s(i + z(i))) {
                z(i) += 1
            }
            if (i + z(i) - 1 > r) {
                l = i
                r = i + z(i) - 1
            }
            totalSum += z(i)
        }
        
        totalSum
    }
}
