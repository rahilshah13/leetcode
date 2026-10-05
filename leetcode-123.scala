// https://leetcode.com/problems/best-time-to-buy-and-sell-stock-iii/
object Solution {
    def maxProfit(prices: Array[Int]): Int = {
        if (prices.isEmpty) return 0
        var buy1 = -prices(0)
        var sell1 = 0
        var buy2 = -prices(0)
        var sell2 = 0
        
        for (i <- 1 until prices.length) {
            val p = prices(i)
            buy1 = buy1.max(-p)
            sell1 = sell1.max(buy1 + p)
            buy2 = buy2.max(sell1 - p)
            sell2 = sell2.max(buy2 + p)
        }
        sell2
    }
}
