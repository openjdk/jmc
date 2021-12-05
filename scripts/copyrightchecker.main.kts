@file:DependsOn("org.junit.jupiter:junit-jupiter:5.7.0")
@file:DependsOn("org.eclipse.jgit:org.eclipse.jgit:6.0.0.202111291000-r")

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry.ChangeType.*
import org.eclipse.jgit.lib.AbbreviatedObjectId
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import java.io.File
import java.time.Instant
import java.time.ZoneId
import kotlin.system.exitProcess

fun run() {
    // the script needs to run in the directory where jmc is checked out:
    // kotlinc -script scripts/copyrightchecker.main.kts
    val git = GitClient(".")
    CopyrightChecker.validateChanges(git.getCommitYear(), git.getChanges())
}

run()

class GitClient(jmcRepoPath: String) {
    private val git: Git
    private val repo: Repository
    private val head: RevCommit
    private val base: RevCommit
    init {
        val gitDir = File(jmcRepoPath)
        git = Git.open(gitDir)
        // TODO: fail if not in JMC repo
        repo = git.repository!!
        head = getHeadCommit()
        base = getBaseCommit()
        println("Checked out at: ${head.name} (base: ${base.name})")
        // TODO: maybe fail if there are unstashed changes?
        // TODO: exit with success if head commit is on master
    }

    private fun getHeadCommit(): RevCommit =
        RevWalk(repo).parseCommit(repo.resolve(Constants.HEAD))

    private fun getBaseCommit(): RevCommit {
        val walk = RevWalk(repo)
        val master = walk.parseCommit(repo.resolve("master"))
        walk.apply {
            revFilter = RevFilter.MERGE_BASE
            markStart(head)
            markStart(master)
        }
        return walk.next()
    }

    private fun getBytes(reader: ObjectReader, shortId: AbbreviatedObjectId): ByteArray {
        val objId = reader.resolve(shortId).single()
        return reader.open(objId).bytes
    }

    fun getChanges(): List<Change> {
        val headTree = repo.resolve("HEAD^{tree}")
        val baseTree = repo.resolve("${base.name}^{tree}")
        val reader: ObjectReader = repo.newObjectReader()
        val baseIter = CanonicalTreeParser().apply { reset(reader, baseTree) }
        val headIter = CanonicalTreeParser().apply { reset(reader, headTree) }
        return git.diff()
            .setNewTree(headIter)
            .setOldTree(baseIter)
            .call()
            .filterNot { it.changeType == DELETE }
            .map { diff ->
                when (diff.changeType) {
                    MODIFY, RENAME, COPY -> {
                        Change(
                            newPath = diff.newPath,
                            headBytes = getBytes(reader, diff.newId),
                            baseBytes = getBytes(reader, diff.oldId)
                        )
                    }
                    ADD -> Change(newPath = diff.newPath, headBytes = getBytes(reader, diff.newId))
                    else -> throw IllegalArgumentException("Unsupported: ${diff.changeType}")
                }
            }
    }

    fun getCommitYear(): Int =
        Instant.ofEpochSecond(head.commitTime.toLong()).atZone(ZoneId.of("Etc/UTC")).year
}

class Change(val newPath: String, val headBytes: ByteArray, val baseBytes: ByteArray? = null)

data class Range(val start: Int, val end: Int? = null) {
    companion object {
        fun update(range: Range, year: Int): Range =
            if (range.start >= year) {
                range
            } else {
                Range(range.start, year)
            }
    }
    override fun toString(): String =
        if (end != null) {
            "$start, $end"
        } else {
            "$start"
        }
}

data class CopyrightString(private val range: Range, private val holder: CopyrightHolders) {
    companion object {
        fun parse(line: String): CopyrightString? {
            if (!line.contains("Copyright")) return null
            return try {
                val parts: List<String> = line
                    .split(".").first()
                    .split("(c)")[1]
                    .split(",")
                    .map { part -> part.trim() }
                when (parts.size) {
                    2 -> CopyrightString(Range(parts[0].toInt()), CopyrightHolders.fromString(parts[1]))
                    3 -> CopyrightString(Range(parts[0].toInt(), parts[1].toInt()), CopyrightHolders.fromString(parts[2]))
                    else -> throw IllegalArgumentException()
                }
            } catch (e: Exception) {
                throw IllegalArgumentException("🟡 Failed to parse: $line", e)
            }
        }

        fun update(original: CopyrightString, endYear: Int): CopyrightString {
            return original.copy(range = Range.update(original.range, endYear))
        }
    }
    override fun toString() = "Copyright (c) $range, ${holder.displayName}. All rights reserved."
}

class CopyrightHeader(val holders: List<CopyrightString>) {
    companion object {
        fun parse(file: String): CopyrightHeader {
            val holders = file.split("\n").mapNotNull { CopyrightString.parse(it) }
            return CopyrightHeader(holders)
        }

        fun update(original: CopyrightHeader, endYear: Int): CopyrightHeader =
            CopyrightHeader(original.holders.map { CopyrightString.update(it, endYear) })
    }
    fun format(fileType: FileTypes): String {
        val prefix: String
        val before: List<String>
        val after: List<String>
        when (fileType) {
            FileTypes.JAVA -> {
                prefix = " * "
                before = listOf("/*")
                after = listOf(" *", " * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.")
            }
            FileTypes.JAVASCRIPT -> {
                prefix = " "
                before = listOf("/*")
                after = listOf("", " DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.")
            }
            FileTypes.XML -> {
                prefix = "\t"
                before = listOf("""<?xml version="1.0" encoding="UTF-8"?>""", "<!--")
                after = listOf("", "\tDO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.")
            }
            FileTypes.UNSUPPORTED -> throw IllegalArgumentException(fileType.toString())
        }
        val headerLines = before + holders.map { "$prefix$it" } + after
        return headerLines.joinToString("\n")
    }
}

class CopyrightChecker {
    companion object {
        fun validateChanges(commitYear: Int, changes: List<Change>) {
            if (changes.map { validateChange(it, commitYear) }.filterNot { it }.isNotEmpty()) {
                println()
                println("❌ Validation failed")
                exitProcess(1)
            } else {
                println("✅ Validation succeeded")
                exitProcess(0)
            }
        }

        private fun validateChange(change: Change, year: Int): Boolean {
            val fileType = FileTypes.fromPath(change.newPath)
            if (fileType == FileTypes.UNSUPPORTED) {
                println("⚪️ ${change.newPath}")
                return true
            }
            val header = if (change.baseBytes != null) {
                val baseContent = String(change.baseBytes, Charsets.UTF_8)
                try {
                    CopyrightHeader.update(CopyrightHeader.parse(baseContent), year)
                } catch (e: Exception) {
                    println("🔴 ${change.newPath}")
                    println(e.message)
                    return false
                }
            } else {
                CopyrightHeader(
                    listOf(
                        CopyrightString(Range(year), CopyrightHolders.ORACLE),
                        CopyrightString(Range(year), CopyrightHolders.DATADOG)
                    )
                )
            }
            val headContent = String(change.headBytes, Charsets.UTF_8)
            val expected = header.format(fileType)
            return if (headContent.startsWith(expected)) {
                println("🟢 ${change.newPath}")
                true
            } else {
                println("🔴 ${change.newPath}")
                println("Expected:")
                println(expected)
                println("Actual:")
                println(headContent.substring(0, expected.length + 1))
                println("(check whitespace if strings seem to match)")
                false
            }
        }
    }
}

enum class FileTypes {
    JAVA, JAVASCRIPT, XML, UNSUPPORTED;
    companion object {
        fun fromPath(path: String): FileTypes =
            if (isExcluded(path)) {
                UNSUPPORTED
            } else if (path.endsWith(".java")) {
                JAVA
            } else if (path.endsWith(".js")) {
                JAVASCRIPT
            } else if (path.endsWith(".xml")) {
                XML
            } else {
                UNSUPPORTED
            }

        private fun isExcluded(path: String): Boolean =
            path.endsWith("plugin.xml")
    }
}

enum class CopyrightHolders(val displayName: String) {
    ORACLE("Oracle and/or its affiliates"), DATADOG("Datadog, Inc");
    companion object {
        fun fromString(str: String) =
            values().find { it.displayName == str } ?: throw IllegalArgumentException(str)
    }
}

// We could use something similar to validate all copyright notices in the repo:
//
// fun treeWalk() {
//    val revWalk = RevWalk(repo)
//    val lastCommitId = repo.resolve(Constants.HEAD)!!
//    val commit: RevCommit = revWalk.parseCommit(lastCommitId)
//
//    val tree: RevTree = commit.tree
//    val treeWalk = TreeWalk(repo)
//    treeWalk.addTree(tree)
//    treeWalk.isRecursive = true
//
//    println("starting tree walk")
//    treeWalk.filter = PathSuffixFilter.create(".java")
//    val javaFiles = mutableListOf<Pair<String, Instant>>()
//    while (treeWalk.next()) {
//        git.log().addPath(treeWalk.pathString).setMaxCount(1).call().forEach {
//            javaFiles.add(Pair(treeWalk.pathString, Instant.ofEpochSecond(it.commitTime.toLong())))
//        }
//        if (javaFiles.size % 10 == 0) {
//            println(javaFiles.size)
//            break
//        }
//    }
//    println(javaFiles)
// }
