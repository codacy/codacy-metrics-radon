package codacy.metrics

import better.files.File
import codacy.docker.api.metrics.{FileMetrics, LineComplexity, MetricsTool}
import codacy.docker.api.{MetricsConfiguration, Source}
import codacy.metrics.RadonResult._
import com.codacy.api.dtos.{Language, Languages}
import com.codacy.docker.api.utils.{CommandResult, CommandRunner}
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
        for {
          complexity <- calculateComplexity(source.path, files)
          loc <- calculateLoc(source.path, files)
        } yield {
          createFileMetrics(complexity, loc)
        }
    }
  }

  private def createFileMetrics(complexity: Seq[RadonFileComplexity], loc: Seq[RadonFileMetrics]): List[FileMetrics] = {
    loc.map { fileLoc =>
      val fileComplexity: Option[RadonFileComplexity] = complexity.find(_.filename == fileLoc.filename)

      val lineComplexities: Set[LineComplexity] =
        fileComplexity
          .to[Seq]
          .flatMap(_.methods.map(methodComplexity =>
            LineComplexity(methodComplexity.line, methodComplexity.complexity)))(collection.breakOut)

      val fileMaxComplexity = fileComplexity.flatMap(_.methods.map(_.complexity).reduceOption(_ max _))
      FileMetrics(
        fileLoc.filename,
        loc = Some(fileLoc.loc),
        cloc = Some(fileLoc.comments),
        complexity = fileMaxComplexity,
        nrMethods = fileComplexity.map(_.methods.length),
        lineComplexities = lineComplexities)
    }(collection.breakOut)
  }

  private def calculateMethodComplexity(methods: Seq[JsValue]): Seq[RadonMethodComplexity] = {
    methods.flatMap { method =>
      val closureMetrics = (method \ "closures").asOpt[Seq[JsValue]].map(calculateMethodComplexity).toSeq.flatten
      val methodMetrics = method
        .validate[RadonMethodOutput]
        .map { metric =>
          RadonMethodComplexity(
            metric.name,
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

  private def calculateComplexity(directory: String, files: Option[Set[Source.File]]): Try[Seq[RadonFileComplexity]] = {
    val command = getComplexityCommand(files)
    withToolParsedOutput[RadonFileComplexity](directory, command) { output =>
      val filesWithoutErrors = output.mapValues(_.asOpt[Seq[JsValue]].toSeq.flatten)

      filesWithoutErrors.map {
        case (file, jsValue) =>
          val filename = file.stripPrefix(directory).stripPrefix("/")
          val methods = calculateMethodComplexity(jsValue)
          RadonFileComplexity(filename, methods)
      }(collection.breakOut)
    }
  }

  private def calculateLoc(directory: String, files: Option[Set[Source.File]]): Try[Seq[RadonFileMetrics]] = {
    val command = getLocCommand(files)
    withToolParsedOutput[RadonFileMetrics](directory, command) { output =>
      output.flatMap {
        case (file, jsValue) =>
          jsValue
            .validate[RadonMetricsOutput]
            .map { output: RadonMetricsOutput =>
              val filename = file.stripPrefix(directory).stripPrefix("/")
              RadonFileMetrics(
                filename,
                output.loc,
                output.lloc,
                output.sloc,
                output.multi,
                output.comments,
                output.blank)
            }
            .asOpt
      }(collection.breakOut)
    }
  }

  private def withToolParsedOutput[T](directory: String, command: Seq[String])(
    parse: Map[String, JsValue] => Seq[T]): Try[Seq[T]] = {
    runTool(directory, command).map {
      _.stdout.flatMap { resultLines =>
        val parsedOutput = Json.parse(resultLines.mkString).asOpt[Map[String, JsValue]].getOrElse(Map())
        parse(parsedOutput)
      }
    }
  }

  private def runTool(directory: String, command: Seq[String]): Try[CommandResult] = {
    val directoryOpt = Option(File(directory).toJava)
    CommandRunner.exec(command.toList, directoryOpt) match {
      case Right(output) =>
        Success(output)
      case Left(s) =>
        Failure(new Exception(s"Radon::runTool could not run radon on $directory: $s"))
    }
  }

  private def getComplexityCommand(maybeFiles: Option[Set[Source.File]]): Seq[String] = {
    genericCommand("cc", Seq("-a", "-s"), maybeFiles)
  }

  private def getLocCommand(maybeFiles: Option[Set[Source.File]]): Seq[String] = {
    genericCommand("raw", Seq.empty, maybeFiles)
  }

  private def genericCommand(executionType: String,
                             flags: Seq[String],
                             files: Option[Set[Source.File]]): Seq[String] = {
    Seq("radon", executionType, "-j") ++ flags ++ files.map(_.map(_.path)).getOrElse(Set("."))
  }
}
