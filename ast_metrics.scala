//> using scala 3.3.3

import java.nio.file.{Files, Paths}
import scala.jdk.CollectionConverters._

object AstAnalyzer {
  def main(args: Array[String]): Unit = {
    val dir = Paths.get(".")
    val files = Files.list(dir)
      .iterator()
      .asScala
      .filter(p => p.toString.endsWith(".scala") && p.getFileName.toString.startsWith("leetcode-"))
      .toList

    case class FileMetrics(
      filename: String, 
      nodeCount: Int, 
      maxDepth: Int, 
      whileCount: Int, 
      ifCount: Int, 
      valDefCount: Int
    )

    val results = files.map { path =>
      val content = Files.readString(path)
      val lines = content.split("\n").toList

      // Heuristic parsing metrics based on syntax tokens
      var maxD = 0
      var currentDepth = 0
      var whileC = 0
      var ifC = 0
      var valDefC = 0
      var tokenCount = 0

      lines.foreach { line =>
        val trimmed = line.trim
        if (trimmed.nonEmpty && !trimmed.startsWith("//")) {
          // Approximate node/token weight
          tokenCount += trimmed.split("\\s+").length

          // Count constructs
          if (trimmed.startsWith("while") || trimmed.contains(" while ")) whileC += 1
          if (trimmed.startsWith("if") || trimmed.contains(" if ")) ifC += 1
          if (trimmed.startsWith("val ") || trimmed.contains(" val ")) valDefC += 1

          // Track scope depth via braces
          trimmed.foreach {
            case '{' => 
              currentDepth += 1
              if (currentDepth > maxD) maxD = currentDepth
            case '}' => 
              if (currentDepth > 0) currentDepth -= 1
            case _ =>
          }
        }
      }

      // Fallback node count estimation if braces are sparse
      val estimatedNodes = math.max(tokenCount * 2, 10)

      FileMetrics(path.getFileName.toString, estimatedNodes, math.max(maxD, 1), whileC, ifC, valDefC)
    }

    def stats(values: Seq[Double]): (Double, Double, Double, Double) = {
      if (values.isEmpty) return (0.0, 0.0, 0.0, 0.0)
      val mean = values.sum / values.length
      val min = values.min
      val max = values.max
      val variance = values.map(v => math.pow(v - mean, 2)).sum / values.length
      val stdDev = math.sqrt(variance)
      (mean, min, max, stdDev)
    }

    val nodeCounts = results.map(_.nodeCount.toDouble)
    val depths = results.map(_.maxDepth.toDouble)
    val whiles = results.map(_.whileCount.toDouble)
    val ifs = results.map(_.ifCount.toDouble)
    val valDefs = results.map(_.valDefCount.toDouble)

    val (nMean, nMin, nMax, nStd) = stats(nodeCounts)
    val (dMean, dMin, dMax, dStd) = stats(depths)
    val (wMean, wMin, wMax, wStd) = stats(whiles)
    val (iMean, iMin, iMax, iStd) = stats(ifs)
    val (vMean, vMin, vMax, vStd) = stats(valDefs)

    val report = new StringBuilder
    report.append("=== LEETCODE SCALA AST METRICS REPORT ===\n\n")
    report.append(f"Total Files Analyzed: ${results.length}%d\n\n")

    report.append("--- PER-FILE METRICS ---\n")
    results.sortBy(_.filename).foreach { m =>
      report.append(f"File: ${m.filename}%-20s | Nodes: ${m.nodeCount}%-5d | Max Depth: ${m.maxDepth}%-3d | Whiles: ${m.whileCount}%-2d | Ifs: ${m.ifCount}%-2d | ValDefs: ${m.valDefCount}%-3d\n")
    }

    report.append("\n--- DISTRIBUTION STATISTICS ACROSS REPOSITORY ---\n")
    report.append(f"Total Nodes  -> Mean: $nMean%.2f, Min: $nMin%.0f, Max: $nMax%.0f, StdDev: $nStd%.2f\n")
    report.append(f"Max Depth    -> Mean: $dMean%.2f, Min: $dMin%.0f, Max: $dMax%.0f, StdDev: $dStd%.2f\n")
    report.append(f"While Loops  -> Mean: $wMean%.2f, Min: $wMin%.0f, Max: $wMax%.0f, StdDev: $wStd%.2f\n")
    report.append(f"If Branches  -> Mean: $iMean%.2f, Min: $iMin%.0f, Max: $iMax%.0f, StdDev: $iStd%.2f\n")
    report.append(f"Val Defs     -> Mean: $vMean%.2f, Min: $vMin%.0f, Max: $vMax%.0f, StdDev: $vStd%.2f\n")

    Files.writeString(Paths.get("ast_metrics_report.txt"), report.toString())
    println("Successfully generated ast_metrics_report.txt in the current directory.")
  }
}