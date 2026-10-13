/*  
    https://leetcode.com/problems/utf-8-validation
    Iterate over `data`; track the number of expected continuation bytes. 
    Inspect the leading bits of each byte to determine character length or ensure a correct continuation prefix.
*/
object Solution {
    def validUtf8(data: Array[Int]): Boolean = {
        var cnt = 0
        var i = 0
        
        while (i < data.length) {
            val v = data(i)
            if (cnt == 0) {
                if ((v >> 7) == 0) {
                    // 1-byte character (0xxxxxxx)
                    cnt = 0
                } else if ((v >> 5) == 6) {
                    // 2-byte character (110xxxxx)
                    cnt = 1
                } else if ((v >> 4) == 14) {
                    // 3-byte character (1110xxxx)
                    cnt = 2
                } else if ((v >> 3) == 30) {
                    // 4-byte character (11110xxx)
                    cnt = 3
                } else {
                    return false
                }
            } else {
                // Check if it's a valid continuation byte (10xxxxxx)
                if ((v >> 6) != 2) {
                    return false
                }
                cnt -= 1
            }
            i += 1
        }

        cnt == 0
    }
}
