package codacy.metrics

import com.codacy.plugins.api.Source
import com.codacy.plugins.api.languages.Languages
import com.codacy.plugins.api.metrics.FileMetrics
import org.specs2.mutable.Specification
import scala.util.Try

class RadonSpec extends Specification {
  
  val targetDir = "src/test/resources"
  val astarFileName = "codacy/metrics/astar.py"
  val rbTreeFileName = "codacy/metrics/b_tree.py"
  
  def check(fileMetricsMap: Try[List[FileMetrics]], length: Int) = fileMetricsMap should beSuccessfulTry.which { fileMetrics => 
    fileMetrics.length === length
    forall(fileMetrics)( fileMetric =>
      fileMetric.complexity.isDefined &&
      fileMetric.nrMethods.isDefined &&
      fileMetric.lineComplexities.nonEmpty
    )
  } 

  "Radon" should {
    "get metrics" in {
      "all files within a directory" in {

        val fileMetricsMap =
          Radon(source = Source.Directory(targetDir), language = None, files = None, options = Map.empty)
        check(fileMetricsMap, length = 2)
      }

      "specific files" in {

        val fileMetricsMap = Radon(source = Source.Directory(targetDir),
                                   language = None,
                                   files = Some(Set(Source.File(astarFileName))),
                                   options = Map.empty)

        check(fileMetricsMap, length = 1)
      }
    }

    "fail if the language isn't Python" in {
      val rubyLang = Languages.Ruby
      val fileMetricsMap = Radon(source = Source.Directory(targetDir),
                                 language = Some(rubyLang),
                                 files = Some(Set(Source.File(rbTreeFileName))),
                                 options = Map.empty)

      fileMetricsMap should beFailedTry[List[FileMetrics]]
        .withThrowable[Exception](pattern = s"Radon only supports Python. Provided language: $rubyLang")
    }
  }
}
