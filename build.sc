import $ivy.`com.goyeau::mill-scalafix::0.2.10`
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version::0.3.0`
import $ivy.`io.chris-kipp::mill-ci-release::0.1.1`

import mill._
import mill.scalalib._
import mill.scalalib.scalafmt._
import mill.scalalib.publish._
import mill.scalalib.api.ZincWorkerUtil
import mill.scalalib.api.Util.scalaNativeBinaryVersion

import com.goyeau.mill.scalafix.ScalafixModule
import de.tobiasroeser.mill.vcs.version.VcsVersion
import io.kipp.mill.ci.release.CiReleaseModule

// Used versions
val millVersion = "0.10.8"
val scala213 = "2.13.8"
val millnativeimage_plugin = "0.1.21"
val millvcsversion_plugin = "0.2.0"

val pluginName = "mill-docker-nativeimage"

def millBinaryVersion(millVersion: String) = scalaNativeBinaryVersion(
  millVersion
)

object plugin
    extends ScalaModule
    with CiReleaseModule
    with ScalafixModule
    with ScalafmtModule {

  override def scalaVersion = scala213

  override def artifactName =
    s"${pluginName}_mill${millBinaryVersion(millVersion)}"

  def pomSettings = PomSettings(
    description =
      "A Mill plugin to generate Docker images with Native Image (GraalVM binary).",
    organization = "com.carlosedp",
    url = "https://github.com/carlosedp/mill-docker-nativeimage",
    licenses = Seq(License.MIT),
    versionControl =
      VersionControl.github("carlosedp", "mill-docker-nativeimage"),
    developers = Seq(
      Developer(
        "carlosedp",
        "Carlos Eduardo de Paula",
        "https://github.com/carlosedp"
      )
    )
  )

  def publishVersion: T[String] = T {
    val isTag = T.ctx().env.get("GITHUB_REF").exists(_.startsWith("refs/tags"))
    val state = VcsVersion.vcsState()
    if (state.commitsSinceLastTag == 0 && isTag) {
      state.stripV(state.lastTag.get)
    } else {
      val v = state.stripV(state.lastTag.get).split('.')
      s"${v(0)}.${(v(1).toInt) + 1}-SNAPSHOT"
    }
  }

  override def sonatypeUri = "https://s01.oss.sonatype.org/service/local"
  override def sonatypeSnapshotUri =
    "https://s01.oss.sonatype.org/content/repositories/snapshots"

  override def compileIvyDeps = super.compileIvyDeps() ++ Agg(
    ivy"com.lihaoyi::mill-scalalib:${millVersion}"
  )

  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"de.tototec::de.tobiasroeser.mill.vcs.version_mill0.10::${millvcsversion_plugin}",
    ivy"io.github.alexarchambault.mill::mill-native-image_mill0.10::${millnativeimage_plugin}"
  )
  override def scalacOptions = Seq("-Ywarn-unused", "-deprecation")

  override def scalafixScalaBinaryVersion =
    ZincWorkerUtil.scalaBinaryVersion(scala213)

  override def scalafixIvyDeps = Agg(
    ivy"com.github.liancheng::organize-imports:0.6.0"
  )
}

// Toplevel commands and aliases
def runTasks(t: Seq[String])(implicit ev: eval.Evaluator) = T.task {
  mill.main.MainModule.evaluateTasks(
    ev,
    t.flatMap(x => x +: Seq("+")).flatMap(x => x.split(" ")).dropRight(1),
    mill.define.SelectMode.Separated
  )(identity)
}
def lint(implicit ev: eval.Evaluator) = T.command {
  runTasks(
    Seq(
      "plugin.fix",
      "mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources"
    )
  )
}
def deps(implicit ev: eval.Evaluator) = T.command {
  mill.scalalib.Dependency.showUpdates(ev)
}
def testall(implicit ev: eval.Evaluator) = T.command {
  runTasks(Seq("plugin.test"))
}
def pub(implicit ev: eval.Evaluator) = T.command {
  runTasks(Seq("io.kipp.mill.ci.release.ReleaseModule/publishAll"))
}
