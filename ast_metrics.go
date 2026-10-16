package main

import (
	"fmt"
	"go/ast"
	"go/parser"
	"go/token"
	"io"
	"math"
	"net/http"
	"os"
	"reflect"
	"sort"
	"strconv"
	"strings"
)

type FileMetrics struct {
	Filename    string
	NodeCount   int
	MaxDepth    int
	WhileCount  int
	IfCount     int
	ValDefCount int
	VarDefCount int
	MatchCount  int
	Difficulty  string
}

func main() {
	// 1. Fetch and robustly parse the LeetCode question difficulty CSV mapping
	difficultyMap := make(map[int]string)
	func() {
		gistCsvUrl := "https://gist.githubusercontent.com/nzxwang/2b384550024ce86c4c9855a7f8811df4/raw/9d8619439bb1dc58697a04c8bbb703e2b1bfd189/all-leetcode-questions.csv"
		resp, err := http.Get(gistCsvUrl)
		if err != nil {
			fmt.Printf("Warning: Could not fetch difficulty CSV (%v). Marking difficulties as 'Unknown'.\n", err)
			return
		}
		defer resp.Body.Close()

		bodyBytes, err := io.ReadAll(resp.Body)
		if err != nil {
			fmt.Printf("Warning: Could not read difficulty CSV body (%v). Marking difficulties as 'Unknown'.\n", err)
			return
		}

		lines := strings.Split(string(bodyBytes), "\n")
		for _, line := range lines {
			trimmed := strings.TrimSpace(line)
			if trimmed != "" && !strings.HasPrefix(trimmed, "#") {
				parts := strings.FieldsFunc(trimmed, func(r rune) bool {
					return r == ',' || r == ' ' || r == '\t'
				})
				if len(parts) >= 2 {
					if probNum, err := strconv.Atoi(parts[0]); err == nil {
						difficultyMap[probNum] = parts[len(parts)-1]
					}
				}
			}
		}
	}()

	// 2. Find matching Go files, sort numerically by problem ID
	entries, err := os.ReadDir(".")
	if err != nil {
		fmt.Printf("Error reading current directory: %v\n", err)
		return
	}

	var files []string
	for _, entry := range entries {
		if !entry.IsDir() {
			name := entry.Name()
			if strings.HasSuffix(name, ".go") && strings.HasPrefix(name, "leetcode-") {
				files = append(files, name)
			}
		}
	}

	sort.Slice(files, func(i, j int) bool {
		return parseProblemID(files[i]) < parseProblemID(files[j])
	})

	var results []FileMetrics

	for _, fileNameStr := range files {
		contentBytes, err := os.ReadFile(fileNameStr)
		if err != nil {
			fmt.Printf("Failed to read %s: %v\n", fileNameStr, err)
			continue
		}

		problemIdOpt := parseProblemID(fileNameStr)
		difficulty := "Unknown"
		if d, ok := difficultyMap[problemIdOpt]; ok {
			difficulty = d
		}

		fset := token.NewFileSet()
		tree, err := parser.ParseFile(fset, fileNameStr, contentBytes, 0)
		if err != nil {
			fmt.Fprintf(os.Stderr, "Failed to parse %s: %v\n", fileNameStr, err)
			continue
		}

		nodeC := 0
		whileC := 0 // Go uses ForStmt for loops
		ifC := 0
		valDefC := 0 // Go Const specs
		varDefC := 0 // Go Var specs
		matchC := 0  // Go Switch statements

		ast.Inspect(tree, func(n ast.Node) bool {
			if n != nil {
				nodeC++
				switch node := n.(type) {
				case *ast.ForStmt:
					whileC++
				case *ast.IfStmt:
					ifC++
				case *ast.GenDecl:
					if node.Tok == token.CONST {
						valDefC += len(node.Specs)
					} else if node.Tok == token.VAR {
						varDefC += len(node.Specs)
					}
				case *ast.SwitchStmt, *ast.TypeSwitchStmt:
					matchC++
				}
			}
			return true
		})

		maxD := int(math.Max(float64(calcDepth(tree, 0)), 1))

		results = append(results, FileMetrics{
			Filename:    fileNameStr,
			NodeCount:   nodeC,
			MaxDepth:    maxD,
			WhileCount:  whileC,
			IfCount:     ifC,
			ValDefCount: valDefC,
			VarDefCount: varDefC,
			MatchCount:  matchC,
			Difficulty:  difficulty,
		})
	}

	// 3. Statistical computations
	stats := func(values []float64) (mean, min, max, stdDev float64) {
		if len(values) == 0 {
			return 0, 0, 0, 0
		}
		sum := 0.0
		min = values[0]
		max = values[0]
		for _, v := range values {
			sum += v
			if v < min {
				min = v
			}
			if v > max {
				max = v
			}
		}
		mean = sum / float64(len(values))
		varianceSum := 0.0
		for _, v := range values {
			diff := v - mean
			varianceSum += diff * diff
		}
		stdDev = math.Sqrt(varianceSum / float64(len(values)))
		return mean, min, max, stdDev
	}

	extractValues := func(getter func(FileMetrics) float64) []float64 {
		vals := make([]float64, len(results))
		for i, r := range results {
			vals[i] = getter(r)
		}
		return vals
	}

	nMean, nMin, nMax, nStd := stats(extractValues(func(m FileMetrics) float64 { return float64(m.NodeCount) }))
	dMean, dMin, dMax, dStd := stats(extractValues(func(m FileMetrics) float64 { return float64(m.MaxDepth) }))
	wMean, wMin, wMax, wStd := stats(extractValues(func(m FileMetrics) float64 { return float64(m.WhileCount) }))
	iMean, iMin, iMax, iStd := stats(extractValues(func(m FileMetrics) float64 { return float64(m.IfCount) }))
	vMean, vMin, vMax, vStd := stats(extractValues(func(m FileMetrics) float64 { return float64(m.ValDefCount) }))
	vrMean, vrMin, vrMax, vrStd := stats(extractValues(func(m FileMetrics) float64 { return float64(m.VarDefCount) }))
	mMean, mMin, mMax, mStd := stats(extractValues(func(m FileMetrics) float64 { return float64(m.MatchCount) }))

	var report strings.Builder
	report.WriteString("# Go AST Metrics\n\n")

	report.WriteString("## Statistics\n\n")
	report.WriteString("| Metric | Mean | Min | Max | StdDev |\n")
	report.WriteString("| :--- | :---: | :---: | :---: | :---: |\n")
	report.WriteString(fmt.Sprintf("| **Nodes** | %.2f | %d | %d | %.2f |\n", nMean, int(nMin), int(nMax), nStd))
	report.WriteString(fmt.Sprintf("| **Depth** | %.2f | %d | %d | %.2f |\n", dMean, int(dMin), int(dMax), dStd))
	report.WriteString(fmt.Sprintf("| **Loops (For)** | %.2f | %d | %d | %.2f |\n", wMean, int(wMin), int(wMax), wStd))
	report.WriteString(fmt.Sprintf("| **If Branches** | %.2f | %d | %d | %.2f |\n", iMean, int(iMin), int(iMax), iStd))
	report.WriteString(fmt.Sprintf("| **Const Defs** | %.2f | %d | %d | %.2f |\n", vMean, int(vMin), int(vMax), vStd))
	report.WriteString(fmt.Sprintf("| **Var Defs** | %.2f | %d | %d | %.2f |\n", vrMean, int(vrMin), int(vrMax), vrStd))
	report.WriteString(fmt.Sprintf("| **Switch Exprs** | %.2f | %d | %d | %.2f |\n", mMean, int(mMin), int(mMax), mStd))

	report.WriteString("\n## Per-File Metrics\n\n")
	report.WriteString("| Filename | Difficulty | Nodes | Depth | Loops | Ifs | ConstDefs | VarDefs | Switches |\n")
	report.WriteString("| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |\n")

	for _, m := range results {
		report.WriteString(fmt.Sprintf("| `%s` | **%s** | %d | %d | %d | %d | %d | %d | %d |\n",
			m.Filename, m.Difficulty, m.NodeCount, m.MaxDepth, m.WhileCount, m.IfCount, m.ValDefCount, m.VarDefCount, m.MatchCount))
	}

	err = os.WriteFile("README_go.md", []byte(report.String()), 0644)
	if err != nil {
		fmt.Printf("Failed to write README_go.md: %v\n", err)
		return
	}
	println("Generated AST-backed README_go.md sorted by problem number in the current directory.")
}

func parseProblemID(filename string) int {
	base := strings.TrimPrefix(filename, "leetcode-")
	base = strings.TrimSuffix(base, ".go")
	id, err := strconv.Atoi(base)
	if err != nil {
		return math.MaxInt
	}
	return id
}

func calcDepth(n ast.Node, current int) int {
	if n == nil {
		return current
	}
	nextDepth := current
	switch n.(type) {
	case *ast.BlockStmt, *ast.IfStmt, *ast.ForStmt, *ast.SwitchStmt, *ast.TypeSwitchStmt, *ast.SelectStmt:
		nextDepth = current + 1
	}

	children := getChildren(n)
	if len(children) == 0 {
		return nextDepth
	}

	maxChild := nextDepth
	for _, child := range children {
		d := calcDepth(child, nextDepth)
		if d > maxChild {
			maxChild = d
		}
	}
	return maxChild
}

func getChildren(n ast.Node) []ast.Node {
	var children []ast.Node
	if n == nil {
		return children
	}
	val := reflect.ValueOf(n)
	if val.Kind() == reflect.Ptr {
		val = val.Elem()
	}
	if val.Kind() != reflect.Struct {
		return children
	}
	for i := 0; i < val.NumField(); i++ {
		inspectValue(val.Field(i), &children)
	}
	return children
}

func inspectValue(v reflect.Value, children *[]ast.Node) {
	if !v.IsValid() || !v.CanInterface() {
		return
	}

	switch v.Kind() {
	case reflect.Ptr, reflect.Interface:
		if !v.IsNil() {
			if node, ok := v.Interface().(ast.Node); ok {
				*children = append(*children, node)
			} else {
				inspectValue(v.Elem(), children)
			}
		}
	case reflect.Slice:
		for i := 0; i < v.Len(); i++ {
			inspectValue(v.Index(i), children)
		}
	}
}
