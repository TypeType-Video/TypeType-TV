package video.typetype.tv.data

object MockChannels {
    val ReelSociety = Channel("reel-society", "Reel Society", "@reelsociety", "2.4M")
    val AtlasField = Channel("atlas-field", "Atlas Field Notes", "@atlasfield", "842K")
    val LowOrbit = Channel("low-orbit", "Low Orbit", "@loworbit", "560K")
    val Tidewater = Channel("tidewater", "Tidewater Sessions", "@tidewater", "318K")
    val Northlight = Channel("northlight", "Northlight Films", "@northlight", "1.2M")
    val Blender = Channel("blender", "Blender Foundation", "@blender", "4.9M")

    val All = listOf(ReelSociety, AtlasField, LowOrbit, Tidewater, Northlight, Blender)
}
