// https://leetcode.com/problems/scramble-string/description/
object Solution {
  def isScramble(s1: String, s2: String): Boolean = {
    val memo = scala.collection.mutable.Map[(String, String), Boolean]()
    def solve(a: String, b: String): Boolean = {
      if (a == b) return true
      if (a.length != b.length) return false
      if (a.sorted != b.sorted) return false
      val key = (a, b)
      if (memo.contains(key)) return memo(key)
      val n = a.length
      var possible = false
      for (i <- 1 until n if !possible) {
        if (solve(a.substring(0, i), b.substring(0, i)) && solve(a.substring(i), b.substring(i))) {
          possible = true
        } else if (solve(a.substring(0, i), b.substring(n - i)) && solve(a.substring(i), b.substring(0, n - i))) {
          possible = true
        }
      }
      memo(key) = possible
      possible
    }
    solve(s1, s2)
  }
}
