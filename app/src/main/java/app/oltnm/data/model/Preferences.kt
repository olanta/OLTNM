package app.oltnm.data.model

enum class ThemeMode {
    System,
    Dark,
    Light
}

enum class Accent {
    Green,
    Orange,
    Purple,
    Blue,
    Pink
}

enum class LanguageChoice(val tag: String?) {
    System(null),
    English("en"),
    PortugueseBrazil("pt-BR")
}

enum class FontFamilyChoice {
    SpaceGrotesk,
    Atkinson,
    System,
    Serif,
    Monospace
}

enum class TextSizeChoice(val scale: Float) {
    Small(0.9f),
    Default(1.0f),
    Large(1.15f),
    ExtraLarge(1.3f)
}

enum class TextColorChoice {
    Default,
    HighContrast,
    Cream,
    Yellow,
    Mint,
    Blue,
    Pink
}
