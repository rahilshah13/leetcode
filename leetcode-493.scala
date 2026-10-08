/*
 * https://leetcode.com/problems/reverse-pairs
 * Uses a divide-and-conquer Merge Sort approach in O(N log N) time.
 */
object Solution {
  def reversePairs(nums: Array[Int]): Int = {
    val longNums = nums.map(_.toLong).toIndexedSeq

    def helper(seq: IndexedSeq[Long]): (Vector[Long], Int) = {
      if (seq.length <= 1) {
        (seq.toVector, 0)
      } else {
        val mid = seq.length / 2
        val (leftSorted, countLeft) = helper(seq.take(mid))
        val (rightSorted, countRight) = helper(seq.drop(mid))

        val (merged, crossCount) = countAndMerge(leftSorted.iterator)(rightSorted.iterator)

        (merged, countLeft + countRight + crossCount)
      }
    }

    helper(longNums)._2
  }

  private def countAndMerge(left: Iterator[Long])(right: Iterator[Long]): (Vector[Long], Int) = {
    val lVec = left.toVector
    val rVec = right.toVector

    @scala.annotation.tailrec
    def findR(lVal: Long, rIdx: Int): Int = {
      if (rIdx < rVec.length && lVal > 2 * rVec(rIdx)) findR(lVal, rIdx + 1)
      else rIdx
    }

    @scala.annotation.tailrec
    def loopCount(lIdx: Int, rIdx: Int, total: Int): Int = {
      if (lIdx >= lVec.length) total
      else {
        val newRIdx = findR(lVec(lIdx), rIdx)
        loopCount(lIdx + 1, newRIdx, total + newRIdx)
      }
    }

    val crossCount = loopCount(0, 0, 0)

    def merge(i: Int, j: Int, acc: List[Long]): List[Long] = {
      if (i >= lVec.length && j >= rVec.length) acc.reverse
      else if (i >= lVec.length) merge(i, j + 1, rVec(j) :: acc)
      else if (j >= rVec.length) merge(i + 1, j, lVec(i) :: acc)
      else if (lVec(i) <= rVec(j)) merge(i + 1, j, lVec(i) :: acc)
      else merge(i, j + 1, rVec(j) :: acc)
    }

    (merge(0, 0, Nil).toVector, crossCount)
  }
}
