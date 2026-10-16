import 'dart:io';
import 'dart:math';
import 'package:analyzer/dart/ast/ast.dart';
import 'package:analyzer/dart/ast/visitor.dart';
import 'package:analyzer/dart/analysis/utilities.dart';

class FileMetrics {
  final String filename;
  final int nodeCount;
  final int maxDepth;
  final int loopCount;
  final int ifCount;
  final int constDefCount;
  final int varDefCount;
  final int switchCount;

  FileMetrics({
    required this.filename,
    required this.nodeCount,
    required this.maxDepth,
    required this.loopCount,
    required this.ifCount,
    required this.constDefCount,
    required this.varDefCount,
    required this.switchCount,
  });
}

void main() async {
  final dir = Directory('.');
  final files = <String>[];
  await for (var entity in dir.list()) {
    if (entity is File) {
      final name = entity.uri.pathSegments.last;
      if (name.endsWith('.dart') && name.startsWith('leetcode-')) {
        files.add(name);
      }
    }
  }

  files.sort((a, b) => parseProblemID(a).compareTo(parseProblemID(b)));

  final results = <FileMetrics>[];

  for (var fileNameStr in files) {
    String content;
    try {
      content = await File(fileNameStr).readAsString();
    } catch (e) {
      print('Failed to read $fileNameStr:$e');
      continue;
    }

    CompilationUnit tree;
    try {
      final parseResult = parseString(content: content);
      tree = parseResult.unit;
    } catch (e) {
      stderr.writeln('Failed to parse $fileNameStr:$e');
      continue;
    }

    int nodeC = 0;
    int loopC = 0;     
    int ifC = 0;       
    int constDefC = 0; 
    int varDefC = 0;   
    int switchC = 0;   

    tree.accept(_RecursiveVisitor(onNode: (n) {
      nodeC++;
      if (n is ForStatement || n is WhileStatement || n is DoStatement) {
        loopC++;
      } else if (n is IfStatement) {
        ifC++;
      } else if (n is VariableDeclarationList) {
        if (n.isConst) {
          constDefC += n.variables.length;
        } else {
          varDefC += n.variables.length;
        }
      } else if (n is SwitchStatement || n is SwitchExpression) {
        switchC++;
      }
    }));

    final maxD = max(calcDepth(tree, 0), 1);

    results.add(FileMetrics(
      filename: fileNameStr,
      nodeCount: nodeC,
      maxDepth: maxD,
      loopCount: loopC,
      ifCount: ifC,
      constDefCount: constDefC,
      varDefCount: varDefC,
      switchCount: switchC,
    ));
  }

  StatsResult stats(List<double> values) {
    if (values.isEmpty) return StatsResult(0, 0, 0, 0);
    double sum = 0.0;
    double minVal = values[0];
    double maxVal = values[0];
    for (var v in values) {
      sum += v;
      if (v < minVal) minVal = v;
      if (v > maxVal) maxVal = v;
    }
    double mean = sum / values.length;
    double varianceSum = 0.0;
    for (var v in values) {
      double diff = v - mean;
      varianceSum += diff * diff;
    }
    double stdDev = sqrt(varianceSum / values.length);
    return StatsResult(mean, minVal, maxVal, stdDev);
  }

  List<double> extractValues(double Function(FileMetrics) getter) {
    return results.map(getter).toList();
  }

  final sNodes  = stats(extractValues((m) => m.nodeCount.toDouble()));
  final sDepth  = stats(extractValues((m) => m.maxDepth.toDouble()));
  final sLoops  = stats(extractValues((m) => m.loopCount.toDouble()));
  final sIfs    = stats(extractValues((m) => m.ifCount.toDouble()));
  final sConst  = stats(extractValues((m) => m.constDefCount.toDouble()));
  final sVar    = stats(extractValues((m) => m.varDefCount.toDouble()));
  final sSwitch = stats(extractValues((m) => m.switchCount.toDouble()));

  final lines = <String>[
    '# Dart AST Metrics',
    '',
    '## Statistics',
    '',
    '\x7C Metric \x7C Mean \x7C Min \x7C Max \x7C StdDev \x7C',
    '\x7C :--- \x7C :---: \x7C :---: \x7C :---: \x7C :---: \x7C',
    '\x7C **Nodes** \x7C ' + sNodes.mean.toStringAsFixed(2) + ' \x7C ' + sNodes.min.toInt().toString() + ' \x7C ' + sNodes.max.toInt().toString() + ' \x7C ' + sNodes.stdDev.toStringAsFixed(2) + ' \x7C',
    '\x7C **Depth** \x7C ' + sDepth.mean.toStringAsFixed(2) + ' \x7C ' + sDepth.min.toInt().toString() + ' \x7C ' + sDepth.max.toInt().toString() + ' \x7C ' + sDepth.stdDev.toStringAsFixed(2) + ' \x7C',
    '\x7C **Loops** \x7C ' + sLoops.mean.toStringAsFixed(2) + ' \x7C ' + sLoops.min.toInt().toString() + ' \x7C ' + sLoops.max.toInt().toString() + ' \x7C ' + sLoops.stdDev.toStringAsFixed(2) + ' \x7C',
    '\x7C **If Branches** \x7C ' + sIfs.mean.toStringAsFixed(2) + ' \x7C ' + sIfs.min.toInt().toString() + ' \x7C ' + sIfs.max.toInt().toString() + ' \x7C ' + sIfs.stdDev.toStringAsFixed(2) + ' \x7C',
    '\x7C **Const Defs** \x7C ' + sConst.mean.toStringAsFixed(2) + ' \x7C ' + sConst.min.toInt().toString() + ' \x7C ' + sConst.max.toInt().toString() + ' \x7C ' + sConst.stdDev.toStringAsFixed(2) + ' \x7C',
    '\x7C **Var/Final Defs** \x7C ' + sVar.mean.toStringAsFixed(2) + ' \x7C ' + sVar.min.toInt().toString() + ' \x7C ' + sVar.max.toInt().toString() + ' \x7C ' + sVar.stdDev.toStringAsFixed(2) + ' \x7C',
    '\x7C **Switches** \x7C ' + sSwitch.mean.toStringAsFixed(2) + ' \x7C ' + sSwitch.min.toInt().toString() + ' \x7C ' + sSwitch.max.toInt().toString() + ' \x7C ' + sSwitch.stdDev.toStringAsFixed(2) + ' \x7C',
    '',
    '## Per-File Metrics',
    '',
    '\x7C Filename \x7C Nodes \x7C Depth \x7C Loops \x7C Ifs \x7C ConstDefs \x7C VarDefs \x7C Switches \x7C',
    '\x7C :--- \x7C :---: \x7C :---: \x7C :---: \x7C :---: \x7C :---: \x7C :---: \x7C :---: \x7C',
  ];

  for (var m in results) {
    lines.add('\x7C `' + m.filename + '` \x7C ' + m.nodeCount.toString() + ' \x7C ' + m.maxDepth.toString() + ' \x7C ' + m.loopCount.toString() + ' \x7C ' + m.ifCount.toString() + ' \x7C ' + m.constDefCount.toString() + ' \x7C ' + m.varDefCount.toString() + ' \x7C ' + m.switchCount.toString() + ' \x7C');
  }

  try {
    await File('README_dart.md').writeAsString(lines.join('\n') + '\n');
    print('Generated clean README_dart.md successfully!');
  } catch (e) {
    print('Failed to write README_dart.md: \$e');
  }
}

class StatsResult {
  final double mean;
  final double min;
  final double max;
  final double stdDev;
  StatsResult(this.mean, this.min, this.max, this.stdDev);
}

class _RecursiveVisitor extends UnifyingAstVisitor<void> {
  final void Function(AstNode) onNode;
  _RecursiveVisitor({required this.onNode});

  @override
  void visitNode(AstNode node) {
    onNode(node);
    super.visitNode(node);
  }
}

int parseProblemID(String filename) {
  final base = filename.replaceFirst('leetcode-', '').replaceAll('.dart', '');
  return int.tryParse(base) ?? 999999;
}

int calcDepth(AstNode? n, int current) {
  if (n == null) return current;
  int nextDepth = current;
  if (n is Block || n is IfStatement || n is ForStatement || n is WhileStatement || n is DoStatement || n is SwitchStatement || n is TryStatement) {
    nextDepth = current + 1;
  }

  final children = n.childEntities.whereType<AstNode>().toList();
  if (children.isEmpty) return nextDepth;

  int maxChild = nextDepth;
  for (var child in children) {
    int d = calcDepth(child, nextDepth);
    if (d > maxChild) {
      maxChild = d;
    }
  }
  return maxChild;
}
