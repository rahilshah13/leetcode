#include <string.h>
#include <stdlib.h>

int numDistinct(char* s, char* t) {
    int m = strlen(s);
    int n = strlen(t);

    if (m < n) return 0;

    unsigned long long *dp = (unsigned long long *)calloc(n + 1, sizeof(unsigned long long));
    if (!dp) return 0;

    dp[0] = 1;

    for (int i = 1; i <= m; i++) {
        for (int j = n; j >= 1; j--) {
            if (s[i - 1] == t[j - 1]) {
                dp[j] += dp[j - 1];
            }
        }
    }

    int result = (int)dp[n];
    free(dp);
    return result;
}
