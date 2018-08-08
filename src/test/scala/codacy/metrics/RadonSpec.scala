package codacy.metrics

import codacy.docker.api.Source
import codacy.docker.api.metrics.{FileMetrics, LineComplexity}
import com.codacy.api.dtos.Languages
import org.specs2.mutable.Specification

class RadonSpec extends Specification {

  val expectedAStarFileMetric =
    FileMetrics(
      "codacy/metrics/astar.py",
      Some(8),
      Some(85),
      Some(21),
      Some(7),
      None,
      Set(
        LineComplexity(3, 1),
        LineComplexity(18, 8),
        LineComplexity(16, 1),
        LineComplexity(2, 2),
        LineComplexity(12, 4),
        LineComplexity(65, 4),
        LineComplexity(9, 2)))

  val expectedRBTreeFileMetric =
    FileMetrics("codacy/metrics/rb_tree.py", None, Some(525), Some(38), Some(0), None, Set())

  val expectedFileMetrics = List(expectedAStarFileMetric, expectedRBTreeFileMetric)

  val targetDir = "src/test/resources"

  "Radon" should {
    "get metrics" in {
      "all files within a directory" in {

        val fileMetricsMap =
          Radon(source = Source.Directory(targetDir), language = None, files = None, options = Map.empty)

        fileMetricsMap should beSuccessfulTry.withValue(containTheSameElementsAs(expectedFileMetrics))
      }

      "specific files" in {

        val fileMetricsMap = Radon(
          source = Source.Directory(targetDir),
          language = None,
          files = Some(Set(Source.File(expectedAStarFileMetric.filename))),
          options = Map.empty)

        fileMetricsMap should beSuccessfulTry.withValue(containTheSameElementsAs(List(expectedAStarFileMetric)))
      }
    }

    "fail if the language isn't Python" in {
      val rubyLang = Languages.Ruby
      val fileMetricsMap = Radon(
        source = Source.Directory(targetDir),
        language = Some(rubyLang),
        files = Some(Set(Source.File(expectedRBTreeFileMetric.filename))),
        options = Map.empty)

      fileMetricsMap should beFailedTry[List[FileMetrics]]
        .withThrowable[Exception](pattern = s"Radon only supports Python. Provided language: $rubyLang")
    }
  }
}
