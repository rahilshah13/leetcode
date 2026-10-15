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

      var maxD = 0
      var currentDepth = 0
      var whileC = 0
      var ifC = 0
      var valDefC = 0
      var tokenCount = 0

      lines.foreach { line =>
        val trimmed = line.trim
        if (trimmed.nonEmpty && !trimmed.startsWith("//")) {
          tokenCount += trimmed.split("\\s+").length

          if (trimmed.startsWith("while") || trimmed.contains(" while ")) whileC += 1
          if (trimmed.startsWith("if") || trimmed.contains(" if ")) ifC += 1
          if (trimmed.startsWith("val ") || trimmed.contains(" val ")) valDefC += 1

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
    report.append("# Scala AST Metrics\n\n")
    report.append("** Files:** " + results.length + "\n\n")

    report.append("| Metric | Mean | Min | Max | StdDev |\n")
    report.append("| :--- | :---: | :---: | :---: | :---: |\n")
    report.append("| **Nodes** | " + String.format("%.2f", nMean.asInstanceOf[AnyRef]) + " | " + nMin.toInt + " | " + nMax.toInt + " | " + String.format("%.2f", nStd.asInstanceOf[AnyRef]) + " |\n")
    report.append("| **Depth** | " + String.format("%.2f", dMean.asInstanceOf[AnyRef]) + " | " + dMin.toInt + " | " + dMax.toInt + " | " + String.format("%.2f", dStd.asInstanceOf[AnyRef]) + " |\n")
    report.append("| **While Loops** | " + String.format("%.2f", wMean.asInstanceOf[AnyRef]) + " | " + wMin.toInt + " | " + wMax.toInt + " | " + String.format("%.2f", wStd.asInstanceOf[AnyRef]) + " |\n")
    report.append("| **If Branches** | " + String.format("%.2f", iMean.asInstanceOf[AnyRef]) + " | " + iMin.toInt + " | " + iMax.toInt + " | " + String.format("%.2f", iStd.asInstanceOf[AnyRef]) + " |\n")
    report.append("| **Val Defs** | " + String.format("%.2f", vMean.asInstanceOf[AnyRef]) + " | " + vMin.toInt + " | " + vMax.toInt + " | " + String.format("%.2f", vStd.asInstanceOf[AnyRef]) + " |\n")

    report.append("| Filename | Nodes | Depth | Whiles | Ifs | ValDefs |\n")
    report.append("| :--- | :---: | :---: | :---: | :---: | :---: |\n")
    
    results.sortBy(_.filename).foreach { m =>
      report.append("| `" + m.filename + "` | " + m.nodeCount + " | " + m.maxDepth + " | " + m.whileCount + " | " + m.ifCount + " | " + m.valDefCount + " |\n")
    }

    Files.writeString(Paths.get("README.md"), report.toString())
    println("Successfully generated formatted README.md in the current directory.")
  }
}