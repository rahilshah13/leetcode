//> using scala 3.3.3
//> using dep org.scalameta::scalameta:4.17.4

import java.nio.file.{Files, Paths}
import scala.jdk.CollectionConverters._
import scala.meta._

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
      valDefCount: Int,
      varDefCount: Int,
      matchCount: Int
    )

    val results = files.flatMap { path =>
      val content = Files.readString(path)
      
      content.parse[Source] match {
        case Parsed.Success(tree) =>
          var nodeC = 0
          var whileC = 0
          var ifC = 0
          var valDefC = 0
          var varDefC = 0
          var matchC = 0

          tree.traverse {
            case _: Tree => nodeC += 1
          }

          tree.traverse {
            case _: Term.While => whileC += 1
            case _: Term.If => ifC += 1
            case _: Defn.Val => valDefC += 1
            case _: Defn.Var => varDefC += 1
            case _: Term.Match => matchC += 1
            case _ =>
          }

          def calcDepth(t: Tree, current: Int = 0): Int = {
            val nextDepth = t match {
              case _: Term.Block | _: Template | _: Term.If | _: Term.While | _: Term.Match => current + 1
              case _ => current
            }
            val childDepths = t.children.map(c => calcDepth(c, nextDepth))
            if (childDepths.isEmpty) nextDepth else childDepths.max
          }

          val maxD = math.max(calcDepth(tree), 1)

          Some(FileMetrics(
            path.getFileName.toString, 
            nodeC, 
            maxD, 
            whileC, 
            ifC, 
            valDefC, 
            varDefC, 
            matchC
          ))

        case Parsed.Error(pos, message, _) =>
          System.err.println("Failed to parse " + path.getFileName + " at line " + pos.startLine + ": " + message)
          None
      }
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
    val varDefs = results.map(_.varDefCount.toDouble)
    val matches = results.map(_.matchCount.toDouble)

    val (nMean, nMin, nMax, nStd) = stats(nodeCounts)
    val (dMean, dMin, dMax, dStd) = stats(depths)
    val (wMean, wMin, wMax, wStd) = stats(whiles)
    val (iMean, iMin, iMax, iStd) = stats(ifs)
    val (vMean, vMin, vMax, vStd) = stats(valDefs)
    val (vrMean, vrMin, vrMax, vrStd) = stats(varDefs)
    val (mMean, mMin, mMax, mStd) = stats(matches)

    val report = new StringBuilder
    report.append("# Scala AST Metrics\n\n")

    report.append("## Statistics\n\n")
    report.append("| Metric | Mean | Min | Max | StdDev |\n")
    report.append("| :--- | :---: | :---: | :---: | :---: |\n")
    report.append("| **Nodes** | " + String.format("%.2f", nMean.asInstanceOf[AnyRef]) + " | " + nMin.toInt + " | " + nMax.toInt + " | " + String.format("%.2f", nStd.asInstanceOf[AnyRef]) + " |\n")
    report.append("| **Depth** | " + String.format("%.2f", dMean.asInstanceOf[AnyRef]) + " | " + dMin.toInt + " | " + dMax.toInt + " | " + String.format("%.2f", dStd.asInstanceOf[AnyRef]) + " |\n")
    report.append("| **While Loops** | " + String.format("%.2f", wMean.asInstanceOf[AnyRef]) + " | " + wMin.toInt + " | " + wMax.toInt + " | " + String.format("%.2f", wStd.asInstanceOf[AnyRef]) + " |\n")
    report.append("| **If Branches** | " + String.format("%.2f", iMean.asInstanceOf[AnyRef]) + " | " + iMin.toInt + " | " + iMax.toInt + " | " + String.format("%.2f", iStd.asInstanceOf[AnyRef]) + " |\n")
    report.append("| **Val Defs** | " + String.format("%.2f", vMean.asInstanceOf[AnyRef]) + " | " + vMin.toInt + " | " + vMax.toInt + " | " + String.format("%.2f", vStd.asInstanceOf[AnyRef]) + " |\n")
    report.append("| **Var Defs** | " + String.format("%.2f", vrMean.asInstanceOf[AnyRef]) + " | " + vrMin.toInt + " | " + vrMax.toInt + " | " + String.format("%.2f", vrStd.asInstanceOf[AnyRef]) + " |\n")
    report.append("| **Match Exprs** | " + String.format("%.2f", mMean.asInstanceOf[AnyRef]) + " | " + mMin.toInt + " | " + mMax.toInt + " | " + String.format("%.2f", mStd.asInstanceOf[AnyRef]) + " |\n")

    report.append("\n## Per-File Metrics\n\n")
    report.append("| Filename | Nodes | Depth | Whiles | Ifs | ValDefs | VarDefs | Matches |\n")
    report.append("| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |\n")
    
    results.sortBy(_.filename).foreach { m =>
      report.append("| `" + m.filename + "` | " + m.nodeCount + " | " + m.maxDepth + " | " + m.whileCount + " | " + m.ifCount + " | " + m.valDefCount + " | " + m.varDefCount + " | " + m.matchCount + " |\n")
    }

    Files.writeString(Paths.get("README.md"), report.toString())
    println("Successfully generated sound AST-backed README.md in the current directory.")
  }
}