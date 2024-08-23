package codacy.metrics

import better.files.File
import codacy.metrics.RadonResult._
import com.codacy.docker.api.utils.CommandRunner
import com.codacy.plugins.api.languages.{Language, Languages}
import com.codacy.plugins.api.metrics.{FileMetrics, LineComplexity, MetricsTool}
import com.codacy.plugins.api.{Options, Source}
import play.api.libs.json.{JsValue, Json}

import scala.util.{Failure, Success, Try}

object Radon extends MetricsTool {

  override def apply(source: Source.Directory,
                     language: Option[Language],
                     files: Option[Set[Source.File]],
                     options: Map[Options.Key, Options.Value]): Try[List[FileMetrics]] = {

    language match {
      case Some(lang) if lang != Languages.Python =>
        Failure(new Exception(s"Radon only supports Python. Provided language: $lang"))
      case _ =>
        calculateComplexity(source.path, files).map(createFileMetrics)
    }
  }
  private def createFileMetrics(fileComplexities: Seq[RadonFileComplexity]): List[FileMetrics] =
    fileComplexities.view
      .map { complexity =>
        val (lineComplexities, fileTotalComplexity) =
          complexity.methods.foldLeft((Set.empty[LineComplexity], 0)) {
            case ((lineCplx, fileTotalCplx), methodComplexity) =>
              val newLineComplexity = LineComplexity(methodComplexity.line, methodComplexity.complexity)
              val updatedTotalComplexity = fileTotalCplx + newLineComplexity.value
              (lineCplx + newLineComplexity, updatedTotalComplexity)
          }

        FileMetrics(
          filename = complexity.filename,
          complexity = Some(fileTotalComplexity),
          nrMethods = Some(complexity.methods.length),
          lineComplexities = lineComplexities
        )
      }
      .to(List)

  private def calculateComplexity(directory: String, files: Option[Set[Source.File]]): Try[Seq[RadonFileComplexity]] =
    runTool(directory, getComplexityCommand(files))
      .map { resultLines =>
        val output = Json
          .parse(resultLines.mkString)
          .asOpt[Map[String, JsValue]]
          .getOrElse(Map())
        val filesWithoutErrors = output.view.mapValues(_.asOpt[Seq[JsValue]].toSeq.flatten)
        filesWithoutErrors
          .map {
            case (file, jsValue) =>
              val filename = file.stripPrefix(directory).stripPrefix("/")
              val methods = calculateMethodComplexity(jsValue)
              RadonFileComplexity(filename, methods)
          }
          .to(Seq)
      }

  private def calculateMethodComplexity(methods: Seq[JsValue]): Seq[RadonMethodComplexity] = {
    methods.flatMap { method =>
      val closureMetrics = (method \ "closures")
        .asOpt[Seq[JsValue]]
        .map(calculateMethodComplexity)
        .to(Seq)
        .flatten
      val methodMetrics = method
        .validate[RadonMethodOutput]
        .map { metric =>
          RadonMethodComplexity(metric.name,
                                metric.lineno,
                                metric.col_offset,
                                metric.rank,
                                metric.classname,
                                metric.complexity,
                                metric.lineno,
                                metric.endline)
        }
        .asOpt

      methodMetrics ++ closureMetrics
    }
  }

  private def runTool(directory: String, command: Seq[String]): Try[Seq[String]] = {
    CommandRunner.exec(command.to(List), Option(File(directory).toJava)) match {
      case Right(output) =>
        Success(output.stdout)
      case Left(s) =>
        Failure(new Exception(s"Radon::runTool could not run radon on $directory: $s"))
    }
  }

  private def getComplexityCommand(maybeFiles: Option[Set[Source.File]]): Seq[String] = {
    Seq("radon", "cc", "--total-average", "-j", "-s") ++ maybeFiles.map(_.map(_.path)).getOrElse(Set("."))
  }

}
