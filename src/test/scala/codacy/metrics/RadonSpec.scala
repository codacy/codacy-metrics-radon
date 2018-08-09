package codacy.metrics

import codacy.docker.api.Source
import codacy.docker.api.metrics.{FileMetrics, LineComplexity}
import com.codacy.api.dtos.Languages
import org.specs2.mutable.Specification

class RadonSpec extends Specification {

  val expectedAStarFileMetric =
    FileMetrics(
      filename = "codacy/metrics/astar.py",
      complexity = Some(16),
      nrMethods = Some(28),
      lineComplexities = Set(
        LineComplexity(67, 2),
        LineComplexity(74, 2),
        LineComplexity(23, 1),
        LineComplexity(5, 1),
        LineComplexity(26, 1),
        LineComplexity(109, 1),
        LineComplexity(142, 2),
        LineComplexity(103, 1),
        LineComplexity(106, 1),
        LineComplexity(20, 1),
        LineComplexity(1, 1),
        LineComplexity(87, 1),
        LineComplexity(83, 1),
        LineComplexity(102, 1),
        LineComplexity(157, 6),
        LineComplexity(33, 1),
        LineComplexity(112, 1),
        LineComplexity(36, 16),
        LineComplexity(61, 1),
        LineComplexity(115, 6),
        LineComplexity(19, 1),
        LineComplexity(29, 1),
        LineComplexity(2, 1),
        LineComplexity(62, 1),
        LineComplexity(52, 3),
        LineComplexity(152, 1),
        LineComplexity(71, 1),
        LineComplexity(82, 1)))

  val expectedRBTreeFileMetric =
    FileMetrics(
      filename = "codacy/metrics/rb_tree.py",
      complexity = Some(12),
      nrMethods = Some(29),
      lineComplexities = Set(
        LineComplexity(164, 5),
        LineComplexity(23, 1),
        LineComplexity(5, 2),
        LineComplexity(52, 1),
        LineComplexity(27, 1),
        LineComplexity(58, 10),
        LineComplexity(157, 3),
        LineComplexity(20, 1),
        LineComplexity(173, 1),
        LineComplexity(185, 5),
        LineComplexity(240, 12),
        LineComplexity(126, 3),
        LineComplexity(55, 1),
        LineComplexity(12, 3),
        LineComplexity(44, 1),
        LineComplexity(30, 2),
        LineComplexity(47, 4),
        LineComplexity(93, 8),
        LineComplexity(150, 3),
        LineComplexity(176, 5),
        LineComplexity(41, 1),
        LineComplexity(219, 5),
        LineComplexity(1, 2),
        LineComplexity(202, 5),
        LineComplexity(120, 3),
        LineComplexity(141, 4),
        LineComplexity(48, 1),
        LineComplexity(132, 4),
        LineComplexity(36, 1)))

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
