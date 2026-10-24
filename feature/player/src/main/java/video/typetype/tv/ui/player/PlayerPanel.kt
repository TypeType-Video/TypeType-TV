package video.typetype.tv.ui.player

enum class PlayerPanel { Menu, Chapters, Info, Comments, Queue, Quality, Audio, CaptionStyle, Playlists, Channel }

// The controls that open a panel: closing the panel gives the focus back to the one that opened it.
enum class ControlOrigin { Quality, Audio, Comments, Channel, Description, Menu }
