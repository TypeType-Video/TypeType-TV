package video.typetype.tv.data

internal object MockCatalog {
    private val reel = MockChannels.ReelSociety
    private val atlas = MockChannels.AtlasField
    private val orbit = MockChannels.LowOrbit
    private val tide = MockChannels.Tidewater
    private val north = MockChannels.Northlight
    private val blender = MockChannels.Blender

    val lastReel = Video(
        id = "last-reel",
        title = "The Last Reel",
        channel = reel,
        durationSeconds = 3134,
        views = "1.8M views",
        age = "3 weeks ago",
        genre = "Documentary",
        description = "A small-town projectionist threads the final print through a machine older than " +
            "most of its audience. A quiet portrait of the people who kept the lights low and the " +
            "stories running, shot over one last winter.",
        resumeFraction = 0.34f,
        chapters = listOf(
            Chapter("The booth", 0),
            Chapter("Threading the print", 412),
            Chapter("Empty houses", 1180),
            Chapter("Neon at closing time", 2210),
        ),
    )
    val centralCinema = video(
        "central-cinema", "Night Shift at Central Cinema", reel,
        2412, "940K views", "5 days ago", "Documentary",
        "After the last screening the street goes pink and silent. One night with the crew that resets " +
            "a single-screen cinema for tomorrow.",
        resume = 0.62f,
    )
    val projection = video(
        "projection", "Projection", north,
        1496, "2.1M views", "2 months ago", "Short film",
        "Dust, smoke and a beam of light. A wordless short about a projector that refuses to switch off.",
    )
    val emptyHouses = video(
        "empty-houses", "Empty Houses", reel,
        1788, "611K views", "1 month ago", "Documentary",
        "Rows of red velvet and the sound of a room waiting for its audience. Seven cinemas, seven " +
            "closing nights.",
        focusY = 0.88f,
    )
    val crimsonRidge = video(
        "crimson-ridge", "Crimson Ridge", atlas,
        2105, "1.3M views", "1 week ago", "Landscape",
        "Two days above the tree line while the last light turns the whole ridge the color of embers.",
        quality = "8K",
    )
    val mistyValley = video(
        "misty-valley", "Above the Mist", atlas,
        1654, "2.6M views", "4 weeks ago", "Landscape",
        "A valley that disappears under cloud every dawn and returns by noon. Filmed over twelve " +
            "mornings from one overlook.",
        resume = 0.18f,
    )
    val alpineValley = video(
        "alpine-valley", "Valley of Glass", atlas,
        1322, "774K views", "6 weeks ago", "Landscape",
        "Snow melt, cold air and a river the color of a blue bottle. A slow descent through an alpine " +
            "valley in early summer.",
    )
    val turquoiseLake = video(
        "turquoise-lake", "Slow Hours at the Lake", atlas,
        1950, "1.1M views", "2 months ago", "Landscape",
        "Sunrise on perfectly still water, with no narration and no music other than the wind.",
    )
    val greenCliffs = video(
        "green-cliffs", "Coastal Road, Green Cliffs", north,
        1210, "502K views", "3 months ago", "Travel",
        "A drive along cliffs that run straight into low cloud, with a long golden hour at the end.",
    )
    val tuscanRoad = video(
        "tuscan-road", "Tuscan Roads at Dusk", north,
        1740, "1.9M views", "2 weeks ago", "Travel",
        "Cypress lines, rolling fields and one very long road. Filmed in the hour after the sun leaves.",
        resume = 0.81f,
    )
    val violetDusk = video(
        "violet-dusk", "Violet Dusk", orbit,
        980, "3.4M views", "1 month ago", "Night sky",
        "A time-lapse of the Milky Way rising over a quiet horizon, edited to a single unbroken take.",
        quality = "8K",
    )
    val orbitalWeather = video(
        "orbital-weather", "Orbital Weather", orbit,
        2680, "4.2M views", "6 days ago", "Space",
        "Storm systems seen from four hundred kilometers up, paired with the people who forecast them.",
    )
    val nebulaStudy = video(
        "nebula-study", "Nebula Study No. 4", orbit,
        1105, "889K views", "2 months ago", "Night sky",
        "Long exposures of a stellar nursery, rendered in the colors the sensor actually recorded.",
        resume = 0.47f,
    )
    val winterStars = video(
        "winter-stars", "Winter Stars", orbit,
        1420, "1.5M views", "3 weeks ago", "Night sky",
        "Snow, silence and a shooting star over a mountain ridge at minus twenty.",
    )
    val twilightCoast = video(
        "twilight-coast", "Twilight Coast", tide,
        1860, "421K views", "9 days ago", "Ambient",
        "Tide lines and a violet sky. Ninety minutes of shoreline for long evenings.",
        isLive = true,
    )
    val sailboat = video(
        "sailboat-mirror", "Sailboat on Still Water", tide,
        1380, "712K views", "5 weeks ago", "Ambient",
        "A lone sailboat drifts through a perfect reflection under a full night sky.",
    )
    val harbor = video(
        "harbor-blue-hour", "Harbor at Blue Hour", tide,
        1175, "366K views", "2 weeks ago", "City",
        "The city lights come on one block at a time while the water goes from pink to navy.",
    )
    val redSunset = video(
        "red-sunset", "Red Sunset Sessions", tide,
        2040, "1.0M views", "1 month ago", "Ambient",
        "One sunset, recorded from five locations along the same coast.",
    )

    val bigBuckBunny = Video(
        id = "big-buck-bunny",
        title = "Big Buck Bunny",
        channel = blender,
        durationSeconds = 596,
        views = "58M views",
        age = "Open movie",
        genre = "Animation",
        description = "A giant, gentle rabbit and three bullying rodents. Big Buck Bunny is the open movie " +
            "from the Blender Foundation, released under a Creative Commons license. This is the clip " +
            "that plays in the demo player.",
        chapters = listOf(
            Chapter("Opening", 0),
            Chapter("The meadow", 62),
            Chapter("The rodents", 118),
            Chapter("Trouble in the tree", 165),
        ),
    )
    val bbbScenes = listOf(
        bbbScene(2, "Scene 1"),
        bbbScene(3, "Scene 2"),
        bbbScene(4, "Scene 3"),
        bbbScene(5, "Scene 4"),
        bbbScene(6, "Scene 5"),
        bbbScene(7, "Scene 6"),
        bbbScene(8, "Scene 7"),
    )

    val all: List<Video> = listOf(
        lastReel, centralCinema, projection, emptyHouses, crimsonRidge, mistyValley, alpineValley,
        turquoiseLake, greenCliffs, tuscanRoad, violetDusk, orbitalWeather, nebulaStudy, winterStars,
        twilightCoast, sailboat, harbor, redSunset, bigBuckBunny,
    ) + bbbScenes

    val trendingSearches = listOf(
        "cinema", "night sky", "mountains", "documentary", "coast", "big buck bunny", "time-lapse",
    )

    private fun video(
        id: String,
        title: String,
        channel: Channel,
        durationSeconds: Int,
        views: String,
        age: String,
        genre: String,
        description: String,
        resume: Float? = null,
        isLive: Boolean = false,
        quality: String = "4K",
        focusY: Float = 0.5f,
    ) = Video(
        id, title, channel, durationSeconds, views, age, genre, description,
        resumeFraction = resume, isLive = isLive, quality = quality, focusY = focusY,
    )

    private fun bbbScene(index: Int, title: String) = Video(
        id = "bbb-scene-$index",
        title = "Big Buck Bunny, $title",
        channel = blender,
        durationSeconds = 60 + index * 11,
        views = "${index * 3 + 4}M views",
        age = "Open movie",
        genre = "Animation",
        description = "An excerpt from Big Buck Bunny, the Blender Foundation open movie, released under a " +
            "Creative Commons Attribution license.",
        quality = "HD",
    )
}
