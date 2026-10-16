//> using scala 3.3.3
//> using dep org.scalameta::scalameta:4.17.4

import java.nio.file.{Files, Paths}
import java.net.URL
import scala.jdk.CollectionConverters._
import scala.meta._

object AstAnalyzer {
  def main(args: Array[String]): Unit = {
    // 1. Fetch and robustly parse the LeetCode question difficulty CSV mapping
    val difficultyMap: Map[Int, String] = try {
      val gistCsvUrl = "https://gist.githubusercontent.com/nzxwang/2b384550024ce86c4c9855a7f8811df4/raw/9d8619439bb1dc58697a04c8bbb703e2b1bfd189/all-leetcode-questions.csv"
      val csvContent = scala.io.Source.fromURL(new URL(gistCsvUrl)).getLines().toList
      
      csvContent.flatMap { line =>
        val trimmed = line.trim
        if (trimmed.nonEmpty && !trimmed.startsWith("#")) {
          val parts = trimmed.split("[,\\s]+")
          if (parts.length >= 2) {
            parts(0).toIntOption.map { problemNum =>
              problemNum -> parts.last
            }
          } else None
        } else None
      }.toMap
    } catch {
      case e: Exception =>
        println("Warning: Could not fetch difficulty CSV (" + e.getMessage + "). Marking difficulties as 'Unknown'.")
        Map.empty[Int, String]
    }

    val dir = Paths.get(".")
    
    // 2. Find matching files, convert to List first, then sort numerically by problem ID
    val files = Files.list(dir)
      .iterator()
      .asScala
      .filter(p => p.toString.endsWith(".scala") && p.getFileName.toString.startsWith("leetcode-"))
      .toList
      .sortBy { p =>
        p.getFileName.toString
          .stripPrefix("leetcode-")
          .stripSuffix(".scala")
          .toIntOption
          .getOrElse(Int.MaxValue)
      }

    case class FileMetrics(
      filename: String, 
      nodeCount: Int, 
      maxDepth: Int, 
      loopCount: Int, 
      ifCount: Int, 
      valDefCount: Int,
      varDefCount: Int,
      matchCount: Int,
      difficulty: String
    )

    val results = files.flatMap { path =>
      val content = Files.readString(path)
      val fileNameStr = path.getFileName.toString

      val problemIdOpt = fileNameStr.stripPrefix("leetcode-").stripSuffix(".scala").toIntOption
      val difficulty = problemIdOpt.flatMap(difficultyMap.get).getOrElse("Unknown")
      
      content.parse[Source] match {
        case Parsed.Success(tree) =>
          var nodeC = 0
          var loopC = 0
          var ifC = 0
          var valDefC = 0
          var varDefC = 0
          var matchC = 0

          tree.traverse {
            case _: Tree => nodeC += 1
          }

          tree.traverse {
            case _: Term.For      => loopC += 1
            case _: Term.ForYield => loopC += 1
            case _: Term.While    => loopC += 1
            case _: Term.Do       => loopC += 1
            case _: Term.If       => ifC += 1
            case _: Defn.Val      => valDefC += 1
            case _: Defn.Var      => varDefC += 1
            case _: Term.Match    => matchC += 1
            case _                =>
          }

          def calcDepth(t: Tree, current: Int = 0): Int = {
            val nextDepth = t match {
              case _: Term.Block | _: Template | _: Term.If | _: Term.While | _: Term.Do | _: Term.For | _: Term.ForYield | _: Term.Match => current + 1
              case _ => current
            }
            val childDepths = t.children.map(c => calcDepth(c, nextDepth))
            if (childDepths.isEmpty) nextDepth else childDepths.max
          }

          val maxD = math.max(calcDepth(tree), 1)

          Some(FileMetrics(
            fileNameStr, 
            nodeC, 
            maxD, 
            loopC, 
            ifC, 
            valDefC, 
            varDefC, 
            matchC,
            difficulty
          ))

        case Parsed.Error(pos, message, _) =>
          System.err.println("Failed to parse " + fileNameStr + " at line " + pos.startLine + ": " + message)
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
    val loops = results.map(_.loopCount.toDouble)
    val ifs = results.map(_.ifCount.toDouble)
    val valDefs = results.map(_.valDefCount.toDouble)
    val varDefs = results.map(_.varDefCount.toDouble)
    val matches = results.map(_.matchCount.toDouble)

    val (nMean, nMin, nMax, nStd) = stats(nodeCounts)
    val (dMean, dMin, dMax, dStd) = stats(depths)
    val (lMean, lMin, lMax, lStd) = stats(loops)
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
    report.append("| **Loops** | " + String.format("%.2f", lMean.asInstanceOf[AnyRef]) + " | " + lMin.toInt + " | " + lMax.toInt + " | " + String.format("%.2f", lStd.asInstanceOf[AnyRef]) + " |\n")
    report.append("| **If Branches** | " + String.format("%.2f", iMean.asInstanceOf[AnyRef]) + " | " + iMin.toInt + " | " + iMax.toInt + " | " + String.format("%.2f", iStd.asInstanceOf[AnyRef]) + " |\n")
    report.append("| **Val Defs** | " + String.format("%.2f", vMean.asInstanceOf[AnyRef]) + " | " + vMin.toInt + " | " + vMax.toInt + " | " + String.format("%.2f", vStd.asInstanceOf[AnyRef]) + " |\n")
    report.append("| **Var Defs** | " + String.format("%.2f", vrMean.asInstanceOf[AnyRef]) + " | " + vrMin.toInt + " | " + vrMax.toInt + " | " + String.format("%.2f", vrStd.asInstanceOf[AnyRef]) + " |\n")
    report.append("| **Match Exprs** | " + String.format("%.2f", mMean.asInstanceOf[AnyRef]) + " | " + mMin.toInt + " | " + mMax.toInt + " | " + String.format("%.2f", mStd.asInstanceOf[AnyRef]) + " |\n")

    report.append("\n## Per-File Metrics\n\n")
    report.append("| Filename | Difficulty | Nodes | Depth | Loops | Ifs | ValDefs | VarDefs | Matches |\n")
    report.append("| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |\n")
    
    // 3. Output results preserving the numerical sort order
    results.foreach { m =>
      report.append("| `" + m.filename + "` | **" + m.difficulty + "** | " + m.nodeCount + " | " + m.maxDepth + " | " + m.loopCount + " | " + m.ifCount + " | " + m.valDefCount + " | " + m.varDefCount + " | " + m.matchCount + " |\n")
    }

    Files.writeString(Paths.get("README.md"), report.toString())
    println("Generated AST-backed README.md sorted by problem number in the current directory.")
  }
}
