// https://leetcode.com/problems/distinct-subsequences
object Solution {
  def numDistinct(s: String, t: String): Int = {
    val (m, n) = (s.length, t.length)
    if (m < n) return 0
    // dp(j) tracks the number of ways to form a prefix of length j.
    val dp = { val arr = new Array[Int](n + 1); arr(0) = 1; arr }
    for (i <- 0 until m) {
      val charS = s(i)
      for (j <- n to 1 by -1) {
        if (charS == t(j - 1)) {
          dp(j) += dp(j - 1)
        }
      }
    }
    dp(n)
  }
}
