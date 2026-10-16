#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <limits.h>

char* minWindow(char* s, char* t) {
    if (!s || !t || *s == '\0' || *t == '\0') {
        char* empty = malloc(1);
        empty[0] = '\0';
        return empty;
    }

    int t_freq[128] = {0};
    int window_freq[128] = {0};

    int t_len = strlen(t);
    int s_len = strlen(s);

    // Count character frequencies for string t
    for (int i = 0; i < t_len; i++) {
        t_freq[(unsigned char)t[i]]++;
    }

    // Count how many unique characters in t need to be matched
    int required = 0;
    for (int i = 0; i < 128; i++) {
        if (t_freq[i] > 0) {
            required++;
        }
    }

    int formed = 0;
    int l = 0, r = 0;
    int min_len = INT_MAX;
    int min_left = 0;

    while (r < s_len) {
        char c = s[r];
        window_freq[(unsigned char)c]++;

        if (t_freq[(unsigned char)c] > 0 && window_freq[(unsigned char)c] == t_freq[(unsigned char)c]) {
            formed++;
        }

        // Contract the window from the left until it ceases to be 'valid'
        while (l <= r && formed == required) {
            if (r - l + 1 < min_len) {
                min_len = r - l + 1;
                min_left = l;
            }

            char left_char = s[l];
            window_freq[(unsigned char)left_char]--;
            if (t_freq[(unsigned char)left_char] > 0 && window_freq[(unsigned char)left_char] < t_freq[(unsigned char)left_char]) {
                formed--;
            }
            l++;
        }
        r++;
    }

    if (min_len == INT_MAX) {
        char* res = malloc(1);
        res[0] = '\0';
        return res;
    }

    char* result = malloc(min_len + 1);
    if (!result) return NULL;
    
    strncpy(result, s + min_left, min_len);
    result[min_len] = '\0';
    return result;
}
