#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dirent.h>
#include <sys/stat.h>
#include <math.h>
#include <limits.h>
#include <clang-c/Index.h>

#define MAX_FILES 1024
#define MAX_LINE 2048
#define MAX_DIFF_ENTRIES 5000

typedef struct {
    int id;
    char difficulty[32];
} DifficultyEntry;

typedef struct {
    char filename[256];
    int total_node_count;
    int local_node_count;
    int max_depth;
    int loop_count;
    int if_count;
    int const_count;
    int var_count;
    int switch_count;
    char difficulty[32];
} FileMetrics;

typedef struct {
    int total_node_count;
    int local_node_count;
    int max_depth;
    int current_depth;
    int loop_count;
    int if_count;
    int const_count;
    int var_count;
    int switch_count;
} ASTVisitorContext;

enum CXChildVisitResult ast_visitor(CXCursor cursor, CXCursor parent, CXClientData client_data) {
    ASTVisitorContext *ctx = (ASTVisitorContext *)client_data;

    // Count every node in the entire translation unit
    ctx->total_node_count++;

    int is_sys = clang_Location_isInSystemHeader(clang_getCursorLocation(cursor));

    // Only collect metrics and local node count for non-system headers
    if (!is_sys) {
        ctx->local_node_count++;

        enum CXCursorKind kind = clang_getCursorKind(cursor);

        int is_structural = (kind == CXCursor_CompoundStmt || kind == CXCursor_IfStmt || 
                             kind == CXCursor_ForStmt || kind == CXCursor_WhileStmt || 
                             kind == CXCursor_DoStmt || kind == CXCursor_SwitchStmt);

        if (is_structural) {
            ctx->current_depth++;
            if (ctx->current_depth > ctx->max_depth) {
                ctx->max_depth = ctx->current_depth;
            }
        }

        if (kind == CXCursor_ForStmt || kind == CXCursor_WhileStmt || kind == CXCursor_DoStmt) {
            ctx->loop_count++;
        } else if (kind == CXCursor_IfStmt) {
            ctx->if_count++;
        } else if (kind == CXCursor_SwitchStmt) {
            ctx->switch_count++;
        } else if (kind == CXCursor_VarDecl) {
            CXType type = clang_getCursorType(cursor);
            if (clang_isConstQualifiedType(type)) {
                ctx->const_count++;
            } else {
                ctx->var_count++;
            }
        }
    }

    clang_visitChildren(cursor, ast_visitor, client_data);

    if (!is_sys) {
        enum CXCursorKind kind = clang_getCursorKind(cursor);
        int is_structural = (kind == CXCursor_CompoundStmt || kind == CXCursor_IfStmt || 
                             kind == CXCursor_ForStmt || kind == CXCursor_WhileStmt || 
                             kind == CXCursor_DoStmt || kind == CXCursor_SwitchStmt);
        if (is_structural) {
            ctx->current_depth--;
        }
    }

    return CXChildVisit_Continue;
}

int parse_problem_id(const char *filename) {
    int id = INT_MAX;
    if (sscanf(filename, "leetcode-%d.c", &id) == 1) {
        return id;
    }
    return id;
}

int compare_files(const void *a, const void *b) {
    const FileMetrics *fa = (const FileMetrics *)a;
    const FileMetrics *fb = (const FileMetrics *)b;
    return parse_problem_id(fa->filename) - parse_problem_id(fb->filename);
}

const char* get_difficulty(DifficultyEntry *entries, int count, int id) {
    for (int i = 0; i < count; i++) {
        if (entries[i].id == id) {
            return entries[i].difficulty;
        }
    }
    return "Unknown";
}

int main() {
    DifficultyEntry *diff_entries = malloc(MAX_DIFF_ENTRIES * sizeof(DifficultyEntry));
    if (!diff_entries) {
        perror("Failed to allocate memory for difficulty entries");
        return 1;
    }
    int diff_count = 0;

    const char *gist_url = "https://gist.githubusercontent.com/nzxwang/2b384550024ce86c4c9855a7f8811df4/raw/9d8619439bb1dc58697a04c8bbb703e2b1bfd189/all-leetcode-questions.csv";
    char curl_cmd[512];
    snprintf(curl_cmd, sizeof(curl_cmd), "curl -s \"%s\"", gist_url);

    FILE *csv_pipe = popen(curl_cmd, "r");
    if (!csv_pipe) {
        printf("Warning: Could not fetch difficulty CSV. Marking difficulties as 'Unknown'.\n");
    } else {
        char line[MAX_LINE];
        while (fgets(line, sizeof(line), csv_pipe) && diff_count < MAX_DIFF_ENTRIES) {
            line[strcspn(line, "\r\n")] = 0;
            if (line[0] == '\0' || line[0] == '#') continue;

            int prob_num = -1;
            char *token = strtok(line, ", \t");
            if (token) {
                prob_num = atoi(token);
            }
            char *last = token;
            while (token != NULL) {
                last = token;
                token = strtok(NULL, ", \t");
            }
            if (last && prob_num != -1) {
                diff_entries[diff_count].id = prob_num;
                strncpy(diff_entries[diff_count].difficulty, last, sizeof(diff_entries[diff_count].difficulty) - 1);
                diff_entries[diff_count].difficulty[sizeof(diff_entries[diff_count].difficulty) - 1] = '\0';
                diff_count++;
            }
        }
        pclose(csv_pipe);
    }

    DIR *dir = opendir(".");
    if (!dir) {
        perror("Error reading current directory");
        free(diff_entries);
        return 1;
    }

    FileMetrics *results = malloc(MAX_FILES * sizeof(FileMetrics));
    if (!results) {
        perror("Failed to allocate memory for file metrics");
        closedir(dir);
        free(diff_entries);
        return 1;
    }
    int result_count = 0;

    struct dirent *entry;
    while ((entry = readdir(dir)) != NULL) {
        if (entry->d_type == DT_REG || entry->d_type == DT_UNKNOWN) {
            char *name = entry->d_name;
            size_t len = strlen(name);
            if (len > 2 && strcmp(name + len - 2, ".c") == 0 && strncmp(name, "leetcode-", 9) == 0) {
                if (result_count < MAX_FILES) {
                    strncpy(results[result_count].filename, name, sizeof(results[result_count].filename) - 1);
                    results[result_count].filename[sizeof(results[result_count].filename) - 1] = '\0';
                    result_count++;
                }
            }
        }
    }
    closedir(dir);

    qsort(results, result_count, sizeof(FileMetrics), compare_files);

    CXIndex index = clang_createIndex(0, 0);

    for (int i = 0; i < result_count; i++) {
        int prob_id = parse_problem_id(results[i].filename);
        const char *diff = get_difficulty(diff_entries, diff_count, prob_id);
        strncpy(results[i].difficulty, diff, sizeof(results[i].difficulty) - 1);
        results[i].difficulty[sizeof(results[i].difficulty) - 1] = '\0';

        CXTranslationUnit tu = clang_parseTranslationUnit(
            index,
            results[i].filename,
            NULL, 0,
            NULL, 0,
            CXTranslationUnit_None
        );

        if (!tu) {
            fprintf(stderr, "Failed to parse AST for %s\n", results[i].filename);
            continue;
        }

        CXCursor cursor = clang_getTranslationUnitCursor(tu);
        ASTVisitorContext ctx = {0, 0, 1, 0, 0, 0, 0, 0, 0};

        clang_visitChildren(cursor, ast_visitor, &ctx);

        results[i].total_node_count = ctx.total_node_count > 0 ? ctx.total_node_count : 1;
        results[i].local_node_count = ctx.local_node_count > 0 ? ctx.local_node_count : 1;
        results[i].max_depth = ctx.max_depth > 0 ? ctx.max_depth : 1;
        results[i].loop_count = ctx.loop_count;
        results[i].if_count = ctx.if_count;
        results[i].const_count = ctx.const_count;
        results[i].var_count = ctx.var_count;
        results[i].switch_count = ctx.switch_count;

        clang_disposeTranslationUnit(tu);
    }

    clang_disposeIndex(index);

    double total_nodes_sum = 0, local_nodes_sum = 0, depth_sum = 0, loop_sum = 0, if_sum = 0, const_sum = 0, var_sum = 0, switch_sum = 0;
    int n = result_count;

    if (n > 0) {
        for (int i = 0; i < n; i++) {
            total_nodes_sum += results[i].total_node_count;
            local_nodes_sum += results[i].local_node_count;
            depth_sum += results[i].max_depth;
            loop_sum += results[i].loop_count;
            if_sum += results[i].if_count;
            const_sum += results[i].const_count;
            var_sum += results[i].var_count;
            switch_sum += results[i].switch_count;
        }
    }

    double totalMean = n > 0 ? total_nodes_sum / n : 0;
    double localMean = n > 0 ? local_nodes_sum / n : 0;
    double dMean = n > 0 ? depth_sum / n : 0;
    double wMean = n > 0 ? loop_sum / n : 0;
    double iMean = n > 0 ? if_sum / n : 0;
    double vMean = n > 0 ? const_sum / n : 0;
    double vrMean = n > 0 ? var_sum / n : 0;
    double mMean = n > 0 ? switch_sum / n : 0;

    FILE *readme = fopen("README_c.md", "w");
    if (!readme) {
        perror("Failed to write README_c.md");
        free(results);
        free(diff_entries);
        return 1;
    }

    fprintf(readme, "# C AST Metrics\n\n");
    fprintf(readme, "## Statistics\n\n");
    fprintf(readme, "| Metric | Mean |\n");
    fprintf(readme, "| :--- | :---: |\n");
    fprintf(readme, "| **Total AST Nodes** | %.2f |\n", totalMean);
    fprintf(readme, "| **Local AST Nodes** | %.2f |\n", localMean);
    fprintf(readme, "| **Depth** | %.2f |\n", dMean);
    fprintf(readme, "| **Loops** | %.2f |\n", wMean);
    fprintf(readme, "| **If Branches** | %.2f |\n", iMean);
    fprintf(readme, "| **Const Defs** | %.2f |\n", vMean);
    fprintf(readme, "| **Var Defs** | %.2f |\n", vrMean);
    fprintf(readme, "| **Switch Exprs** | %.2f |\n", mMean);

    fprintf(readme, "\n## Per-File Metrics\n\n");
    fprintf(readme, "| Filename | Difficulty | Total Nodes | Local Nodes | Depth | Loops | Ifs | ConstDefs | VarDefs | Switches |\n");
    fprintf(readme, "| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |\n");

    for (int i = 0; i < result_count; i++) {
        fprintf(readme, "| `%s` | **%s** | %d | %d | %d | %d | %d | %d | %d | %d |\n",
            results[i].filename, results[i].difficulty,
            results[i].total_node_count, results[i].local_node_count,
            results[i].max_depth, results[i].loop_count,
            results[i].if_count, results[i].const_count,
            results[i].var_count, results[i].switch_count);
    }

    fclose(readme);
    free(results);
    free(diff_entries);
    printf("Generated AST-backed README_c.md with total and local node metrics.\n");
    return 0;
}