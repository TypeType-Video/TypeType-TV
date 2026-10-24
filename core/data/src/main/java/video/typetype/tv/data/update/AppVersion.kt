package video.typetype.tv.data.update

// A version as the build and the release workflow write it: "x.y.z", or "x.y.z-beta.n" for a beta, which comes before
// the stable release of the same number.
data class AppVersion(val major: Int, val minor: Int, val patch: Int, val beta: Int? = null) : Comparable<AppVersion> {
    val isBeta: Boolean get() = beta != null

    override fun compareTo(other: AppVersion): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch }, { it.beta ?: Int.MAX_VALUE })

    override fun toString(): String = "$major.$minor.$patch" + (beta?.let { "-beta.$it" } ?: "")

    companion object {
        private val PATTERN = Regex("""v?(\d+)\.(\d+)\.(\d+)(?:-beta\.(\d+))?""")

        fun parse(text: String): AppVersion? {
            val (major, minor, patch, beta) = PATTERN.matchEntire(text.trim())?.destructured ?: return null
            return AppVersion(
                major.toIntOrNull() ?: return null,
                minor.toIntOrNull() ?: return null,
                patch.toIntOrNull() ?: return null,
                if (beta.isEmpty()) null else beta.toIntOrNull() ?: return null,
            )
        }
    }
}
