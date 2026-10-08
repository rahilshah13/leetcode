// https://leetcode.com/problems/k-th-smallest-in-lexicographical-order
object Solution {
    def findKthNumber(n: Int, k: Int): Int = {
        var curr = 1L
        var remK = k.toLong - 1L // 0-indexed tracking as Long
        
        while (remK > 0) {
            val steps = countSteps(n, curr, curr + 1)
            if (remK >= steps) {
                // Skip the entire subtree of 'curr'
                curr += 1
                remK -= steps
            } else {
                // Descend into the subtree of 'curr'
                curr *= 10
                remK -= 1
            }
        }
        
        curr.toInt
    }
    
    private def countSteps(n: Int, n1: Long, n2: Long): Long = {
        var steps = 0L
        var currN1 = n1
        var currN2 = n2
        val maxN = n.toLong
        
        // Count how many numbers are prefixed by 'currN1' up to 'n'
        while (currN1 <= maxN) {
            steps += Math.min(maxN + 1, currN2) - currN1
            currN1 *= 10
            currN2 *= 10
        }
        
        steps
    }
}
