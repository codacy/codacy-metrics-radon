package codacy.metrics

import better.files.File
import codacy.docker.api.metrics.{FileMetrics, LineComplexity, MetricsTool}
import codacy.docker.api.{MetricsConfiguration, Source}
import codacy.metrics.RadonResult._
import com.codacy.api.dtos.{Language, Languages}
import com.codacy.docker.api.utils.CommandRunner
import play.api.libs.json.{JsValue, Json}

import scala.util.{Failure, Success, Try}

object Radon extends MetricsTool {

  override def apply(source: Source.Directory,
                     language: Option[Language],
                     files: Option[Set[Source.File]],
                     options: Map[MetricsConfiguration.Key, MetricsConfiguration.Value]): Try[List[FileMetrics]] = {

    language match {
      case Some(lang) if lang != Languages.Python =>
        Failure(new Exception(s"Radon only supports Python. Provided language: $lang"))
      case _ =>
        calculateComplexity(source.path, files).map(createFileMetrics)
    }
  }

  private def createFileMetrics(fileComplexities: Seq[RadonFileComplexity]): List[FileMetrics] = {
    fileComplexities.map { complexity =>
      val (lineComplexities, fileMaxComplexity) =
        complexity.methods.foldLeft((Set.empty[LineComplexity], Option.empty[Int])) {
          case ((lineCplx, fileMaxCplxOpt), methodComplexity) =>
            val newLineComplexity = LineComplexity(methodComplexity.line, methodComplexity.complexity)
            val fileMaxCplx = Some(fileMaxCplxOpt.fold(newLineComplexity.value)(_.max(newLineComplexity.value)))
            (lineCplx + newLineComplexity, fileMaxCplx)
        }

      FileMetrics(filename = complexity.filename,
                  complexity = fileMaxComplexity,
                  nrMethods = Some(complexity.methods.length),
                  lineComplexities = lineComplexities)
    }(collection.breakOut)
  }

  private def calculateComplexity(directory: String, files: Option[Set[Source.File]]): Try[Seq[RadonFileComplexity]] = {
    runTool(directory, getComplexityCommand(files))
      .map { resultLines =>
        Json
          .parse(resultLines.mkString)
          .asOpt[Map[String, JsValue]]
          .getOrElse(Map())
      }
      .map { output =>
        val filesWithoutErrors = output.mapValues(_.asOpt[Seq[JsValue]].toSeq.flatten)

        filesWithoutErrors.map {
          case (file, jsValue) =>
            val filename = file.stripPrefix(directory).stripPrefix("/")
            val methods = calculateMethodComplexity(jsValue)
            RadonFileComplexity(filename, methods)
        }(collection.breakOut)
      }
  }

  private def calculateMethodComplexity(methods: Seq[JsValue]): Seq[RadonMethodComplexity] = {
    methods.flatMap { method =>
      val closureMetrics = (method \ "closures")
        .asOpt[Seq[JsValue]]
        .map(calculateMethodComplexity)
        .to[Seq]
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
    CommandRunner.exec(command.to[List], Option(File(directory).toJava)) match {
      case Right(output) =>
        Success(output.stdout)
      case Left(s) =>
        Failure(new Exception(s"Radon::runTool could not run radon on $directory: $s"))
    }
  }

  private def getComplexityCommand(maybeFiles: Option[Set[Source.File]]): Seq[String] = {
    Seq("radon", "cc", "-j", "-a", "-s") ++ maybeFiles.map(_.map(_.path)).getOrElse(Set("."))
  }

}