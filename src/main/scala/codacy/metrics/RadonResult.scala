package codacy.metrics
import play.api.libs.json._

// refer to http://radon.readthedocs.org/en/latest/commandline.html
final case class RadonFileMetrics(filename: String,
                                  loc: Int,
                                  lloc: Int,
                                  sloc: Int,
                                  multi: Int,
                                  comments: Int,
                                  blank: Int)

final case class RadonFileComplexity(filename: String, methods: Seq[RadonMethodComplexity])

final case class RadonMethodComplexity(name: String,
                                       line: Int,
                                       colOffset: Int,
                                       rank: String,
                                       className: Option[String],
                                       complexity: Int,
                                       startLine: Int,
                                       endLine: Int)

//auxiliary parse classes
final case class RadonMetricsOutput(loc: Int, lloc: Int, sloc: Int, multi: Int, comments: Int, blank: Int)

final case class RadonMethodOutput(name: String,
                                   col_offset: Int,
                                   rank: String,
                                   classname: Option[String],
                                   complexity: Int,
                                   lineno: Int,
                                   endline: Int)

object RadonResult {
  implicit val complexityFmt: Reads[RadonMethodOutput] = Json.reads[RadonMethodOutput]
  implicit val metricsFmt: Reads[RadonMetricsOutput] = Json.reads[RadonMetricsOutput]
}
