int numDistinct(String s, String t) {
  int m = s.length;
  int n = t.length;
  if (m < n) {
    return 0;
  }
  
  List<int> dp = List.filled(n + 1, 0);
  dp[0] = 1;
  
  for (int i = 1; i <= m; i++) {
    for (int j = n; j >= 1; j--) {
      if (s[i - 1] == t[j - 1]) {
        dp[j] += dp[j - 1];
      }
    }
  }
  
  return dp[n];
}

void main() {
  String s = "rabbbit";
  String t = "rabbit";
  print(numDistinct(s, t)); // Output: 3
}
