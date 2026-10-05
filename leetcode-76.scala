// https://leetcode.com/problems/minimum-window-substring/
object Solution {

  def minWindow(s: String, t: String): String = {
    if (s.isEmpty || t.isEmpty) return ""

    val dictT = scala.collection.mutable.Map[Char, Int]()
    for (ch <- t) {
      dictT(ch) = dictT.getOrElse(ch, 0) + 1
    }

    val required = dictT.size
    var formed = 0
    val windowCounts = scala.collection.mutable.Map[Char, Int]()

    var minLen = Int.MaxValue
    var ansLeft = 0
    var ansRight = 0

    var l = 0
    var r = 0

    while (r < s.length) {
      val char = s(r)
      windowCounts(char) = windowCounts.getOrElse(char, 0) + 1

      if (dictT.contains(char) && windowCounts(char) == dictT(char)) {
        formed += 1
      }

      while (l <= r && formed == required) {
        val currentLen = r - l + 1
        if (currentLen < minLen) {
          minLen = currentLen
          ansLeft = l
          ansRight = r
        }

        val leftChar = s(l)
        windowCounts(leftChar) -= 1
        if (dictT.contains(leftChar) && windowCounts(leftChar) < dictT(leftChar)) {
          formed -= 1
        }

        l += 1
      }

      r += 1
    }

    if (minLen == Int.MaxValue) "" else s.substring(ansLeft, ansRight + 1)
  }
  
}
