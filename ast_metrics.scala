//> using scala 3.3.3
//> using dep org.scalameta::scalameta_3::4.9.3

import scala.meta._
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
      val tree = content.parse[Source].get

      var count = 0
      var maxD = 0

      // Traverse AST to count total nodes and determine maximum nesting depth
      def traverse(t: Tree, depth: Int): Unit = {
        count += 1
        if (depth > maxD) maxD = depth
        t.children.foreach(child => traverse(child, depth + 1))
      }
      traverse(tree, 0)

      // Count specific structural construct occurrences
      val whileCount = tree.collect { case _: Term.While => 1 }.sum
      val ifCount = tree.collect { case _: Term.If => 1 }.sum
      val valDefCount = tree.collect { case _: Defn.Val => 1 }.sum

      FileMetrics(path.getFileName.toString, count, maxD, whileCount, ifCount, valDefCount)
    }

    // Helper to calculate descriptive distribution statistics
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

    // Build report content
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
