// Copyright (C) 2015 Sam Halliday
// License: Apache-2.0

import sbt._
import Keys._
import Def.Initialize

import fommil.BigProjectPlugin
import fommil.BigProjectKeys

import fommil.BigProjectTestSupport

/**
 * Example large project structure. Specific challenges:
 *
 * 1. Be able to compile small changes in a top-level project without
 *    scanning the entire classpath:
 *
 *    obf-518286142/compile
 *    // implies deletion of its packageBin
 *
 * 2. Make an edit to a low-level project and run the app instantly:
 *
 *    obf--619038209/compile
 *    // implies deletion of its packageBin
 *    obf-518286142/run-main
 *    // rebuilds / scans obf--619038209 only
 *
 * 3. Run a test ensuring that the relevant tested codebase is re-compiled:
 *
 *    obf-518286142/test
 *    // implies rebuild obf-518286142 Compile and Test configurations.
 *
 * 4. Run a test in an Eclipse-style test project and ensure that the
 *    relevant tested codebase is re-compiled:
 *
 *    obf--1788614413/test
 *    // implies rebuild obf-518286142 and obf--1788614413 *only*
 *
 * 5. Make a breaking change in a low-level library and force
 *    recompile above:
 *
 *    obf--619038209/compile-breaking
 *    // all downstream packageBins are deleted
 *
 *
 * NOTE: DEV cycle use obf-1770460346 with tests obf--694432521
 */
object ClusterFsckBuild extends Build {

  override lazy val settings = super.settings ++ Seq(
    scalaVersion := "2.10.6",
    version := "v1",
    // doesn't catch everything https://github.com/sbt/sbt/issues/840
    ivyLoggingLevel := UpdateLogging.Quiet,
    updateOptions := updateOptions.value.withCachedResolution(true)
  )

  override lazy val projects: Seq[Project] = structure.toSeq.map {
    case (name, depNames) =>
      // sidenote, it'd be nice if dependsOn could take a Seq
      depNames.foldLeft(Project(name, file(name))) {
        case (p, d) => p.dependsOn(LocalProject(d))
      }.enablePlugins(BigProjectPlugin).settings(
        // install BigProjectPlugin
        BigProjectPlugin.overrideProjectSettings(Compile, Test)
      ).settings(
        BigProjectTestSupport.testInstrumentation(Compile, Test)
      ).settings(
        updateOptions := updateOptions.value.withCachedResolution(true)
      )
  }.map { proj =>
    // generate the project
    BigProjectTestSupport.createSources(proj.id)
    // customise individual Projects
    proj.id match {
      case "obf--1788614413" => proj.settings(
        BigProjectKeys.eclipseTestsFor := Some(LocalProject("obf-518286142"))
      )
      case "obf--694432521" => proj.settings(
        BigProjectKeys.eclipseTestsFor := Some(LocalProject("obf-1770460346"))
      )
      case _ => proj
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // This bit might make your eyes bleed...
  val structure = Map(
    "obf-940122766" -> List(),
    "obf-99218" -> List("obf-1557503190", "obf-1557503190", "obf-106934911", "obf-940122766", "obf-66858790", "obf--840802615"),
    "obf-811927181" -> List(),
    "obf-1816614364" -> List("obf-1557503190", "obf--439243146", "obf-718599836", "obf-96948919", "obf-106934911", "obf-949122880", "obf--286050166", "obf-1557503190", "obf--1396265195", "obf-528331167"),
    "obf-3059615" -> List("obf--699715794", "obf-1935741769", "obf-1283151106", "obf--699715794", "obf--1955054804"),
    "obf--1955054804" -> List("obf--699715794", "obf-1935741769", "obf-1283151106", "obf--699715794"),
    "obf-359944002" -> List("obf-1557503190", "obf-3059615", "obf-106934911", "obf-528331167", "obf-1557503190", "obf-267894326", "obf-66858790", "obf-334034714"),
    "obf-1079734016" -> List("obf-1557503190", "obf-106934911", "obf-528331167", "obf--1224250592", "obf-1557503190", "obf-267894326", "obf-66858790", "obf--286050166"),
    "obf--610838920" -> List("obf-1557503190", "obf-106934911", "obf-267894326", "obf-1412953125", "obf-1557503190", "obf--1224250592", "obf-66858790", "obf--1935371539"),
    "obf--475274382" -> List(),
    "obf--1105191301" -> List("obf-1557503190", "obf-1557503190", "obf-1558627919", "obf-902494371"),
    "obf-512956774" -> List("obf-106934911", "obf-1558627919", "obf--1105191301", "obf--144985219"),
    "obf-1597193369" -> List("obf-3059615", "obf-106934911", "obf-1558627919", "obf-66858790", "obf--1105191301", "obf-512956774"),
    "obf-1085258647" -> List("obf-1558627919"),
    "obf--1102270990" -> List(),
    "obf-1558627919" -> List("obf--699715794", "obf-3059615", "obf--699715794"),
    "obf-194046995" -> List("obf-1557503190", "obf-1558627919", "obf-1557503190", "obf-106934911", "obf-267894326", "obf--1935371539", "obf-238788968", "obf--208228355", "obf-528331167"),
    "obf--208228355" -> List("obf-1557503190", "obf-1557503190", "obf-106934911"),
    "obf--928021571" -> List("obf-1557503190", "obf-106934911", "obf-528331167", "obf-1557503190", "obf--1935371539", "obf--1224250592", "obf-1412953125", "obf--432950194", "obf-66858790"),
    "obf--432950194" -> List("obf-267894326", "obf-238788968"),
    "obf--117814228" -> List("obf-1557503190", "obf-811927181", "obf-106934911", "obf-1557503190", "obf-796156596", "obf-318043232", "obf--670487542", "obf--1321425219", "obf--432950194"),
    "obf-796156596" -> List("obf-1557503190", "obf-106934911", "obf-1557503190", "obf-318043232", "obf-528331167"),
    "obf-318043232" -> List("obf-1557503190", "obf-1557503190", "obf-106934911", "obf-528331167", "obf-267894326", "obf-1558627919", "obf-66858790", "obf--1935371539", "obf-334034714", "obf-238788968", "obf-1935741769", "obf--1224250592", "obf-1412953125", "obf--286050166", "obf--1105191301"),
    "obf--95319061" -> List("obf-1557503190", "obf-1412953125", "obf--1935371539", "obf-238788968", "obf-318043232", "obf-1557503190"),
    "obf--144985219" -> List("obf-1557503190", "obf-1557503190", "obf-106934911", "obf--1105191301", "obf-1085258647"),
    "obf--1258661662" -> List("obf-1557503190", "obf-1557503190", "obf--144985219", "obf-1085258647", "obf--1224250592"),
    "obf-1089551601" -> List("obf-1557503190", "obf--904638642", "obf-1557503190", "obf-106934911", "obf-3732", "obf-528331167", "obf--208228355", "obf-66858790"),
    "obf--169471623" -> List("obf-1557503190", "obf-106934911", "obf-1557503190", "obf-1412953125", "obf-267894326", "obf--1935371539", "obf--1186638575", "obf-238788968", "obf--286050166"),
    "obf-503774505" -> List(),
    "obf-3083686" -> List("obf-1557503190", "obf-1557503190", "obf-106934911", "obf-811927181", "obf-66858790", "obf-528331167", "obf-1283151106"),
    "obf-476779026" -> List("obf-3059615"),
    "obf--954740883" -> List("obf-476779026"),
    "obf--286050166" -> List("obf-1557503190", "obf-1558627919", "obf-106934911", "obf-1557503190", "obf-1412953125", "obf-267894326", "obf--1935371539", "obf-336638985", "obf--1186638575", "obf-238788968"),
    "obf-267894326" -> List("obf-1557503190", "obf-1558627919", "obf-1557503190", "obf-1085258647", "obf-1625754290"),
    "obf-66858790" -> List("obf-1557503190", "obf-267894326", "obf-1557503190", "obf-1625754290"),
    "obf--1935371539" -> List("obf-1557503190", "obf-267894326", "obf-1557503190", "obf--1701257347"),
    "obf--1701257347" -> List("obf-267894326"),
    "obf-336638985" -> List("obf-1557503190", "obf-267894326", "obf-1557503190", "obf--1701257347"),
    "obf-810274774" -> List("obf--286050166", "obf-336638985", "obf-334034714"),
    "obf-268314823" -> List("obf-267894326", "obf--1935371539"),
    "obf--271777879" -> List("obf-718599836"),
    "obf-268374740" -> List("obf-1557503190", "obf-106934911", "obf--1334845253", "obf-1557503190"),
    "obf-1412953125" -> List("obf-1557503190", "obf-1557503190", "obf-106934911", "obf-949122880", "obf-267894326"),
    "obf-1450459450" -> List("obf-1557503190", "obf-1557503190", "obf-106934911", "obf-1412953125", "obf-66858790", "obf-334034714", "obf-528331167"),
    "obf-238788968" -> List("obf-1557503190", "obf-811927181", "obf-1558627919", "obf-267894326", "obf-1412953125", "obf--1186638575", "obf-268314823", "obf-106934911", "obf-1557503190", "obf--1935371539", "obf-336638985", "obf-66858790", "obf--208228355", "obf--271777879"),
    "obf--1186638575" -> List("obf-1557503190", "obf-1557503190", "obf-106934911", "obf-267894326"),
    "obf-334034714" -> List("obf-1557503190", "obf-267894326", "obf-1557503190", "obf-528331167"),
    "obf-1283151106" -> List(),
    "obf-1557503190" -> List("obf--699715794", "obf-1935741769", "obf--699715794"),
    "obf-69820201" -> List("obf--1321425219", "obf-1557503190", "obf-3059615", "obf-106934911"),
    "obf-568447805" -> List("obf-1557503190", "obf--904638642", "obf-1557503190", "obf-106934911", "obf--670487542", "obf--867509511", "obf--144985219", "obf--1334845253", "obf-949122880", "obf--1396265195", "obf--840802615", "obf-3083686", "obf-1123681415", "obf--70210913", "obf-528331167", "obf--1224250592", "obf-69820201", "obf-66858790"),
    "obf-1276180249" -> List("obf-1557503190", "obf-106934911", "obf-528331167", "obf-1557503190", "obf--670487542", "obf--867509511", "obf--1264416950", "obf-105286568", "obf-267894326", "obf-66858790", "obf-334034714"),
    "obf-2085613532" -> List("obf-1557503190", "obf-1557503190", "obf-3083686"),
    "obf-230864705" -> List("obf-1557503190", "obf-1557503190", "obf-106934911", "obf--1334845253", "obf--1396265195"),
    "obf-1802114503" -> List("obf-1557503190", "obf-1557503190", "obf-3083686", "obf-811927181"),
    "obf--1941961473" -> List("obf-1557503190", "obf-1557503190", "obf-96948919", "obf-3401", "obf-106934911", "obf--1396265195"),
    "obf-127851604" -> List("obf-1557503190", "obf-3059615", "obf-106934911", "obf-528331167", "obf-1557503190", "obf--670487542", "obf--867509511", "obf--144985219", "obf--1264416950", "obf-105286568", "obf--1030956553", "obf--1673674795", "obf-1680472250", "obf-1558627919", "obf--1105191301", "obf-512956774", "obf-267894326", "obf-66858790", "obf-334034714"),
    "obf-240850697" -> List("obf-1557503190", "obf-811927181", "obf--904638642", "obf-1557503190", "obf-106934911", "obf--670487542", "obf--867509511", "obf--144985219", "obf--1334845253", "obf-949122880", "obf--1396265195", "obf--840802615", "obf-3083686", "obf-1123681415", "obf--70210913", "obf-528331167", "obf--1224250592", "obf-69820201", "obf-66858790"),
    "obf-2086026661" -> List("obf-1557503190", "obf-1557503190", "obf--1334845253", "obf--1396265195"),
    "obf-217372164" -> List("obf-1557503190", "obf-1557503190", "obf-106934911", "obf--1334845253", "obf--1396265195", "obf--840802615", "obf-1123681415", "obf--1174896390", "obf--1224250592", "obf-69820201", "obf-66858790"),
    "obf-1123681415" -> List("obf-106934911", "obf--1321425219"),
    "obf--1941961142" -> List("obf-1557503190", "obf-1557503190", "obf-106934911", "obf-3732", "obf-96948919", "obf-66858790", "obf-528331167"),
    "obf--595191680" -> List("obf-1557503190", "obf--904638642", "obf-988366318", "obf-1557503190", "obf-106934911", "obf--70210913", "obf-528331167"),
    "obf-96948919" -> List("obf-1557503190", "obf-1558627919", "obf-1557503190", "obf-106934911", "obf-3732", "obf-503774505", "obf-528331167"),
    "obf--495803289" -> List("obf-3059615"),
    "obf--840802615" -> List("obf-106934911", "obf-528331167", "obf--1163802917"),
    "obf-1352064278" -> List("obf--840802615", "obf-66858790"),
    "obf-3401" -> List("obf-1557503190", "obf-1557503190", "obf-106934911", "obf--867509511", "obf--1321425219"),
    "obf-1897030328" -> List("obf--988473064", "obf-106934911", "obf-238788968", "obf-1085258647"),
    "obf--507054457" -> List("obf-1557503190", "obf-1897030328", "obf-796156596", "obf-238788968", "obf--432950194", "obf-1557503190"),
    "obf--1264416950" -> List("obf-1557503190", "obf-106934911", "obf-1557503190", "obf--670487542"),
    "obf--1076830923" -> List("obf-1557503190", "obf-106934911", "obf-528331167", "obf-1557503190", "obf--1264416950", "obf-105286568", "obf-370130649", "obf-267894326", "obf-66858790", "obf-334034714", "obf--1030956553", "obf--390058328", "obf-512956774", "obf-1597193369"),
    "obf-105286568" -> List("obf-1557503190", "obf-106934911", "obf-1557503190", "obf--1264416950"),
    "obf--1030956553" -> List("obf-512956774"),
    "obf--390058328" -> List("obf-1557503190", "obf-106934911", "obf-1557503190", "obf--1264416950", "obf-105286568", "obf--1030956553", "obf--1673674795", "obf-1680472250", "obf-1597193369"),
    "obf--1673674795" -> List("obf-1557503190", "obf-106934911", "obf-1557503190", "obf-512956774", "obf--1030956553"),
    "obf-1680472250" -> List("obf-1557503190", "obf-106934911", "obf-1557503190", "obf--144985219", "obf-512956774", "obf--1264416950", "obf-105286568"),
    "obf-370130649" -> List("obf-1557503190", "obf-106934911", "obf-1557503190", "obf--1264416950", "obf-105286568"),
    "obf--988473064" -> List("obf-106934911"),
    "obf-106934911" -> List("obf-1557503190", "obf-1557503190", "obf-1935741769", "obf-1625754290", "obf--1718151982"),
    "obf--1224250592" -> List("obf-1557503190", "obf-106934911", "obf-528331167", "obf-1557503190", "obf-267894326", "obf-66858790", "obf-334034714"),
    "obf--1321425219" -> List("obf-3059615"),
    "obf-528331167" -> List("obf-1557503190", "obf-106934911", "obf-1557503190", "obf--1321425219", "obf-66858790"),
    "obf-106935042" -> List("obf-1557503190", "obf-1557503190", "obf-3059615"),
    "obf--1768799107" -> List("obf-1557503190", "obf-106934911", "obf-528331167", "obf-1557503190", "obf-66858790", "obf--867509511", "obf--1935371539", "obf-69820201", "obf--1163802917"),
    "obf--1718151982" -> List("obf-1557503190", "obf-106935042", "obf-1557503190", "obf--1105191301"),
    "obf-1625754290" -> List("obf-1558627919", "obf-106935042"),
    "obf--1163802917" -> List("obf-1557503190", "obf-1557503190", "obf-106935042"),
    "obf--867509511" -> List("obf-1557503190", "obf-1557503190", "obf-1935741769", "obf-3059615", "obf-106934911"),
    "obf--33259802" -> List("obf-1557503190", "obf-1557503190", "obf--1321425219", "obf-69820201", "obf-106934911", "obf--867509511", "obf-528331167", "obf-66858790"),
    "obf-1431916525" -> List("obf-1557503190", "obf-1557503190", "obf--1264416950"),
    "obf--2097309276" -> List("obf-1557503190", "obf-106934911", "obf-528331167", "obf-1557503190", "obf--1264416950", "obf-370130649", "obf-267894326", "obf-66858790", "obf-334034714", "obf-1431916525"),
    "obf--502933456" -> List("obf-1557503190", "obf--1396265195", "obf--1321425219", "obf-528331167", "obf-3083686", "obf--1224250592", "obf-1557503190"),
    "obf--1334845253" -> List("obf-1557503190", "obf-1557503190", "obf-106934911", "obf-96948919", "obf-3732", "obf-528331167"),
    "obf--1396265195" -> List("obf--1334845253"),
    "obf-3500592" -> List(),
    "obf-109118246" -> List(),
    "obf-949122880" -> List("obf-1557503190", "obf-1557503190", "obf-106934911"),
    "obf--750356737" -> List("obf-1557503190", "obf-106934911", "obf-949122880", "obf-1557503190", "obf-66858790", "obf-528331167", "obf-69820201"),
    "obf--728452963" -> List("obf-1557503190", "obf-106934911", "obf-1557503190", "obf--1264416950", "obf-105286568", "obf--1076830923", "obf-1558627919"),
    "obf-1265799106" -> List("obf-1557503190", "obf-106934911", "obf-528331167", "obf-1557503190", "obf--1264416950", "obf-105286568", "obf--728452963", "obf--144985219"),
    "obf--699715794" -> List(),
    "obf--1174896390" -> List("obf-1557503190", "obf-1557503190", "obf-3059615", "obf-106934911", "obf-503774505", "obf-66858790", "obf--1163802917"),
    "obf-718599836" -> List("obf-3059615"),
    "obf-1320759587" -> List("obf-718599836"),
    "obf-3732" -> List("obf-1557503190", "obf-1557503190", "obf-106934911", "obf--867509511", "obf--1080558030", "obf-528331167", "obf-66858790"),
    "obf-988366318" -> List("obf-1557503190", "obf-1557503190", "obf--904638642"),
    "obf--439243146" -> List(),
    "obf--1080558030" -> List("obf-1557503190", "obf-1557503190", "obf-106934911", "obf--867509511", "obf--70210913"),
    "obf--70210913" -> List("obf-1557503190", "obf-1557503190", "obf-106934911", "obf--867509511"),
    "obf--904638642" -> List("obf-1557503190", "obf--439243146", "obf-1557503190", "obf-3059615", "obf-106934911", "obf-503774505", "obf--1935371539", "obf-66858790", "obf--867509511", "obf--1080558030", "obf-3732", "obf-528331167"),
    "obf-1935741769" -> List(),
    "obf--670487542" -> List("obf-1557503190", "obf-3059615", "obf--1105191301", "obf-1557503190"),
    "obf--292343691" -> List("obf-1557503190", "obf-106934911", "obf--670487542", "obf-1557503190", "obf-69820201", "obf-528331167", "obf-66858790"),
    "obf-902494371" -> List("obf-1557503190", "obf-1557503190", "obf-3059615"),
    "obf--1411282578" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880"),
    "obf-567421123" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--1411282578"),
    "obf-821507529" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--450167871", "obf-98615419", "obf--495803289"),
    "obf--1632567992" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-821507529"),
    "obf-824030935" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-821507529", "obf--1789680338", "obf--1571363309"),
    "obf--11078406" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-824030935", "obf--991417854"),
    "obf-1245178358" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-3496815", "obf--610838920"),
    "obf--1031086789" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-1245178358"),
    "obf--1691091250" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--1333625462", "obf-1443195271"),
    "obf--367870951" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--1691091250"),
    "obf--1333625462" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-821507529", "obf-988366318"),
    "obf--1888393689" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--1333625462", "obf--1111265206"),
    "obf--1411074041" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-1443195358", "obf-1443195271", "obf-821507529", "obf--619038209", "obf--298109998", "obf--1411282578", "obf-98615419", "obf--1333625462", "obf--1416516781"),
    "obf-1297380720" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--1416516781", "obf-518286142"),
    "obf--1416516781" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-1443195358", "obf--1333625462"),
    "obf-1388080641" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-1297380720", "obf-800141770"),
    "obf-800141770" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--1411074041", "obf--1422455818", "obf-370130649"),
    "obf--383883406" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--1411074041", "obf--447644465"),
    "obf-1227455679" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--619038296", "obf-821507529", "obf--1691091250"),
    "obf-518286142" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--1333625462", "obf--298109998", "obf-1443195358", "obf-988366318"),
    "obf--1788614413" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-518286142"),
    "obf-1063098317" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-1443195358", "obf-821507529", "obf--1411282578", "obf--619038296"),
    "obf--506774076" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-1063098317", "obf--619038296", "obf--1411282578"),
    "obf--1571363309" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-3500249", "obf-821507529"),
    "obf--86054214" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-3500249", "obf--1571363309"),
    "obf--1792203744" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--929116432", "obf-821507529", "obf--619038209", "obf--298109998", "obf--1571363309"),
    "obf-1232432465" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--1792203744"),
    "obf--1789680338" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--1792203744", "obf--447644465"),
    "obf--1441045245" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--1789680338"),
    "obf-1443195271" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--1792203744", "obf--1571363309", "obf--619038296"),
    "obf--1111265206" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-1443195271", "obf-1837141193"),
    "obf-1443195358" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--619038209", "obf--1792203744", "obf--1571363309"),
    "obf-1379470931" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-1443195358"),
    "obf-445276124" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880"),
    "obf-411414754" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880"),
    "obf-98615419" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-3500249"),
    "obf-395452658" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--619038296", "obf--1411282578", "obf--1411074041", "obf-98615419", "obf--144985219"),
    "obf-287810495" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-395452658"),
    "obf-535581142" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-98615419"),
    "obf-1163408594" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--450167871"),
    "obf--832525351" -> List(),
    "obf-1913573688" -> List("obf--832525351"),
    "obf--929116432" -> List(),
    "obf-1770460346" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880"),
    "obf--694432521" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-1770460346"),
    "obf--450167871" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-3500249"),
    "obf-1682059856" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--450167871"),
    "obf--447644465" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--450167871"),
    "obf--991417854" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--928500680", "obf--619038209", "obf--447644465"),
    "obf--619038296" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-1770460346", "obf--450167871", "obf-3500249"),
    "obf-1837141193" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-1443195271", "obf--619038296", "obf--1422455905", "obf--928500680", "obf-1227455679", "obf--447644465", "obf--298109998"),
    "obf--298109998" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--619038296", "obf--619038209", "obf--447644465"),
    "obf-832003295" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--298109998", "obf-1837141193", "obf--991417854"),
    "obf--619038209" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--929116432", "obf-1163408594", "obf-98615419", "obf-3500249", "obf-1770460346", "obf-1431916525"),
    "obf-32910034" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-1443195358", "obf--928500680"),
    "obf-3496815" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--1411074041", "obf-824030935"),
    "obf--456757406" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-3496815"),
    "obf-3500249" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--832525351", "obf-105286568", "obf-445276124"),
    "obf-859949192" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--450167871", "obf-3496815", "obf--367870951", "obf--1691091250", "obf-395452658", "obf-824030935", "obf-1163408594", "obf--832525351", "obf--1411074041", "obf--1792203744", "obf--1789680338", "obf--1333625462", "obf--619038209", "obf--447644465", "obf--929116432", "obf-1063098317", "obf-1443195358", "obf-1227455679", "obf--1416516781", "obf-411414754", "obf-445276124", "obf-1245178358", "obf-518286142", "obf--298109998", "obf-821507529", "obf--1571363309", "obf-1770460346", "obf-1443195271", "obf-3500249", "obf-1297380720", "obf--619038296", "obf--1411282578", "obf--383883406", "obf-98615419"),
    "obf--928500680" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf-3500249", "obf-1770460346", "obf-98615419", "obf--619038296", "obf--619038209"),
    "obf--703595976" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--1792203744", "obf--928500680", "obf--447644465", "obf--298109998", "obf-98615419", "obf-1625754290"),
    "obf--1422455905" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--619038296", "obf-1443195358"),
    "obf--1422455818" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880", "obf--447644465", "obf--1411282578", "obf--1411074041", "obf--928500680", "obf-3401", "obf-370130649"),
    "obf-2070443063" -> List("obf-1557503190", "obf--904638642", "obf-3083686", "obf-66858790", "obf-268374740", "obf-1557503190", "obf-96948919", "obf--1264416950", "obf-106934911", "obf-528331167", "obf--1334845253", "obf--502933456", "obf--1174896390", "obf-3732", "obf-1935741769", "obf--670487542", "obf-949122880"),
    "obf--646304850" -> List(),
    "obf--1513104345" -> List("obf--646304850"),
    "obf--197787518" -> List("obf--646304850"),
    "obf--197784185" -> List("obf-1557503190", "obf--646304850", "obf-106934911", "obf-1557503190", "obf--929116432"),
    "obf-270719206" -> List("obf--646304850")
  )
}