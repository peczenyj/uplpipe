import de.johoop.findbugs4sbt._
import FindBugsReportType._

class Project(info: ProjectInfo) extends DefaultProject(info) with FindBugs {
  override lazy val findbugsReportType = Html
  override lazy val findbugsReportName = "findbugsReport.html"
  override lazy val findbugsExcludeFilters = Some(
    <FindBugsFilter>
      <!-- don't care for performance -->
      <Match><Bug category="PERFORMANCE" /></Match>
    </FindBugsFilter>
  )
}