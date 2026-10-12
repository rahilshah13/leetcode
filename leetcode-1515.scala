// https://leetcode.com/problems/best-position-for-a-service-centre/$0
object Solution {
    def getMinDistSum(positions: Array[Array[Int]]): Double = {
        val n = positions.length.toDouble
        var x = positions.map(_(0)).sum.toDouble / n
        var y = positions.map(_(1)).sum.toDouble / n
        
        var alpha = 1.0
        val decay = 0.999
        val eps = 1e-7
        
        while (alpha > eps) {
            var gradX = 0.0
            var gradY = 0.0
            
            var i = 0
            while (i < positions.length) {
                val px = positions(i)(0).toDouble
                val py = positions(i)(1).toDouble
                val dx = x - px
                val dy = y - py
                val dist = math.sqrt(dx * dx + dy * dy)
                
                if (dist > 1e-8) {
                    gradX += dx / dist
                    gradY += dy / dist
                }
                i += 1
            }
            
            x -= alpha * gradX
            y -= alpha * gradY
            alpha *= decay
        }
        
        var totalDist = 0.0
        var i = 0
        while (i < positions.length) {
            val dx = x - positions(i)(0).toDouble
            val dy = y - positions(i)(1).toDouble
            totalDist += math.sqrt(dx * dx + dy * dy)
            i += 1
        }
        
        totalDist
    }
}
