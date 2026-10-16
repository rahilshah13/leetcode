func minWindow(s string, t string) string {
    if len(s) < len(t) {
        return ""
    }

    var need, window [128]int
    required := 0

    for i := 0; i < len(t); i++ {
        if need[t[i]] == 0 {
            required++
        }
        need[t[i]]++
    }

    left, right := 0, 0
    formed := 0
    minLen := len(s) + 1
    minLeft := 0

    for right < len(s) {
        c := s[right]
        window[c]++
        if need[c] > 0 && window[c] == need[c] {
            formed++
        }
        right++

        for left <= right && formed == required {
            if right - left < minLen {
                minLen = right - left
                minLeft = left
            }

            d := s[left]
            window[d]--
            if need[d] > 0 && window[d] < need[d] {
                formed--
            }
            left++
        }
    }

    if minLen > len(s) {
        return ""
    }

    return s[minLeft : minLeft+minLen]
}
