// Copyright (C) 2015 - 2016 Sam Halliday
// Licence: Apache-2.0
package fommil

import java.util.concurrent.ConcurrentHashMap
import org.apache.ivy.core.module.descriptor.ModuleDescriptor
import org.apache.ivy.core.module.id.ModuleRevisionId
import sbt._
import Keys._
import sbt.inc.Analysis
import sbt.inc.LastModified

/**
 * Publicly exposed keys for settings and tasks that the user may wish
 * to use.
 */
object BigProjectKeys {
  /**
   * NOT IMPLEMENTED YET
   *
   * The user must tell us when a breaking change has been introduced
   * in a module. It will invalidate the caches of all dependent
   * project.
   */
  val breakingChange = TaskKey[Unit](
    "breakingChange",
    "Inform the build that a breaking change was introduced in this project."
  )

  /**
   * NOT IMPLEMENTED YET
   *
   * WORKAROUND: https://bugs.eclipse.org/bugs/show_bug.cgi?id=224708
   *
   * Teams that use Eclipse often put tests in separate packages.
   */
  val eclipseTestsFor = SettingKey[Option[ProjectReference]](
    "eclipseTestsFor",
    "When defined, points to the project that this project is testing."
  )

}

/*
 * All references to `.value` in a Task mean that the task is
 * aggressively invoked as a dependency to this task. Lazily call
 * dependent tasks from Dynamic Tasks:
 *
 *   http://www.scala-sbt.org/0.13/docs/Tasks.html
 */
object BigProjectSettings extends Plugin {
  import BigProjectKeys._

  /**
   * TrackLevel.TrackIfMissing will not invalidate or rebuild jar
   * files if the user explicitly recompiles a project. We delete the
   * packageBin associated to a project when compiling that project so
   * that we never have stale jars.
   */
  private def deletePackageBinTask = (artifactPath in packageBin, state).map { (jar, s) =>
    s.log.debug(s"Deleting $jar")
    jar.delete()
  }

  /**
   * packageBin causes traversals of dependency projects.
   *
   * Caching must be evicted for a project when:
   *
   * - anything (e.g. source, config, packageBin) changes
   *
   * which we implement by deleting the packageBinFile on every
   * compile.
   *
   * However, dependent project caches must only be evicted if a
   * dependency introduced a breaking change.
   *
   * We trust the developer to inform us of breaking API changes
   * manually using the breakingChange task.
   *
   * We use the file's existence as the cache.
   */
  private def dynamicPackageBinTask: Def.Initialize[Task[File]] = Def.taskDyn {
    val jar = (artifactPath in packageBin).value
    if (jar.exists()) Def.task {
      jar
    }
    else Def.task {
      val s = (streams in packageBin).value
      val c = (packageConfiguration in packageBin).value
      Package(c, s.cacheDirectory, s.log)
      jar
    }
  }

  /**
   * transitiveUpdate causes traversals of dependency projects
   *
   * Cache must be evicted for a project and all its dependents when:
   *
   * - changes to the ivy definitions
   * - any inputs to an update phase are changed (changes to generated inputs?)
   */
  private val transitiveUpdateCache = new ConcurrentHashMap[ProjectReference, Seq[UpdateReport]]()
  private def dynamicTransitiveUpdateTask: Def.Initialize[Task[Seq[UpdateReport]]] = Def.taskDyn {
    // doesn't have a Configuration, always project-level
    val key = LocalProject(thisProject.value.id)
    val cached = transitiveUpdateCache.get(key)

    // note, must be in the dynamic task
    val make = new ScopeFilter.Make {}
    val selectDeps = ScopeFilter(make.inDependencies(ThisProject, includeRoot = false))

    if (cached != null) Def.task {
      cached
    }
    else Def.task {
      val allUpdates = update.?.all(selectDeps).value
      val calculated = allUpdates.flatten ++ globalPluginUpdate.?.value
      transitiveUpdateCache.put(key, calculated)
      calculated
    }
  }

  /**
   * dependencyClasspath causes traversals of dependency projects.
   *
   * Cache must be evicted for a project and all its dependents when:
   *
   * - anything (e.g. source, config) changes and the packageBin is not recreated
   *
   * we implement invalidation by checking that all files in the
   * cached classpath exist, if any are missing, we do the work.
   */
  private val dependencyClasspathCache = new ConcurrentHashMap[(ProjectReference, Configuration), Classpath]()
  private def dynamicDependencyClasspathTask: Def.Initialize[Task[Classpath]] = Def.taskDyn {
    val key = (LocalProject(thisProject.value.id), configuration.value)
    val cached = dependencyClasspathCache.get(key)

    if (cached != null && cached.forall(_.data.exists())) Def.task {
      cached
    }
    else Def.task {
      val calculated = internalDependencyClasspath.value ++ externalDependencyClasspath.value
      dependencyClasspathCache.put(key, calculated)
      calculated
    }
  }

  /**
   * Gets invoked when the dependencyClasspath cache misses. We use
   * this to avoid invoking compile:compile unless the jar file is
   * missing for ThisProject.
   */
  val exportedProductsCache = new ConcurrentHashMap[(ProjectReference, Configuration), Classpath]()
  def dynamicExportedProductsTask: Def.Initialize[Task[Classpath]] = Def.taskDyn {
    val key = (LocalProject(thisProject.value.id), configuration.value)
    val jar = (artifactPath in packageBin).value
    val cached = exportedProductsCache.get(key)

    if (jar.exists() && cached != null) Def.task {
      cached
    }
    else Def.task {
      val calculated = (Classpaths.exportProductsTask).value
      exportedProductsCache.put(key, calculated)
      calculated
    }
  }

  /**
   * projectDescriptors causes traversals of dependency projects.
   *
   * Cache must be evicted for a project and all its dependents when:
   *
   * - any project changes, all dependent project's caches must be cleared
   */
  private val projectDescriptorsCache = new ConcurrentHashMap[ProjectReference, Map[ModuleRevisionId, ModuleDescriptor]]()
  private def dynamicProjectDescriptorsTask: Def.Initialize[Task[Map[ModuleRevisionId, ModuleDescriptor]]] = Def.taskDyn {
    // doesn't have a Configuration, always project-level
    val key = LocalProject(thisProject.value.id)
    val cached = projectDescriptorsCache.get(key)

    if (cached != null) Def.task {
      cached
    }
    else Def.task {
      val calculated = Classpaths.depMap.value
      projectDescriptorsCache.put(key, calculated)
      calculated
    }
  }

  /**
   * Returns the exhaustive set of projects that depend on the given one
   * (not including itself).
   */
  private[fommil] def dependents(state: State, proj: ResolvedProject): Set[ResolvedProject] = {
    val extracted = Project.extract(state)
    val structure = extracted.structure

    // builds the full dependents tree
    val dependents = {
      for {
        proj <- structure.allProjects
        dep <- proj.dependencies
        resolved <- Project.getProject(dep.project, structure)
      } yield (resolved, proj)
    }.groupBy {
      case (child, parent) => child
    }.map {
      case (child, grouped) => (child, grouped.map(_._2).toSet)
    }

    def deeper(p: ResolvedProject): Set[ResolvedProject] = {
      val deps = dependents.getOrElse(p, Set.empty)
      deps ++ deps.flatMap(deeper)
    }

    deeper(proj)
  }

  /**
   * We want to be sure that this is the last collection of Settings
   * that runs on each project, so we require that the user manually
   * apply these overrides.
   */
  def overrideProjectSettings(configs: Configuration*): Seq[Setting[_]] = Seq(
    exportJars := true,
    trackInternalDependencies := TrackLevel.TrackIfMissing,
    transitiveUpdate := dynamicTransitiveUpdateTask.value,
    projectDescriptors := dynamicProjectDescriptorsTask.value
  ) ++ configs.flatMap { config =>
      inConfig(config)(
        Seq(
          packageBin := dynamicPackageBinTask.value,
          dependencyClasspath := dynamicDependencyClasspathTask.value,
          exportedProducts := dynamicExportedProductsTask.value,
          compile <<= compile dependsOn deletePackageBinTask
        )
      )
    }

}
