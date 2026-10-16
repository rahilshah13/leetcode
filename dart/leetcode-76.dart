String minWindow(String s, String t) {
  if (s.length < t.length) {
    return "";
  }

  List<int> need = List.filled(128, 0);
  List<int> window = List.filled(128, 0);
  int required = 0;

  for (int i = 0; i < t.length; i++) {
    int charCode = t.codeUnitAt(i);
    if (need[charCode] == 0) {
      required++;
    }
    need[charCode]++;
  }

  int left = 0;
  int right = 0;
  int formed = 0;
  int minLen = s.length + 1;
  int minLeft = 0;

  while (right < s.length) {
    int c = s.codeUnitAt(right);
    window[c]++;
    if (need[c] > 0 && window[c] == need[c]) {
      formed++;
    }
    right++;

    while (left <= right && formed == required) {
      if (right - left < minLen) {
        minLen = right - left;
        minLeft = left;
      }

      int d = s.codeUnitAt(left);
      window[d]--;
      if (need[d] > 0 && window[d] < need[d]) {
        formed--;
      }
      left++;
    }
  }

  if (minLen > s.length) {
    return "";
  }

  return s.substring(minLeft, minLeft + minLen);
}

void main() {
  print(minWindow("ADOBECODEBANC", "ABC")); // Output: "BANC"
  print(minWindow("a", "a")); // Output: "a"
}
