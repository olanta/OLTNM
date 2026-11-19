package app.oltnm.ui.screens.search

data class DiscoverLanguage(
    val id: String,
    val label: String,
    val english: String,
    val query: String,
    val tags: List<String>,
    val keywords: List<String>
) {
    fun toCategory(): DiscoverCategory = DiscoverCategory(
        id = id,
        label = 0,
        query = query,
        tags = tags,
        keywords = keywords
    )
}

val DiscoverLanguages = listOf(
    DiscoverLanguage(
        id = "en",
        label = "English",
        english = "English",
        query = "english",
        tags = listOf("english", "anglophone"),
        keywords = listOf("english", "anglophone")
    ),
    DiscoverLanguage(
        id = "id",
        label = "Bahasa Indonesia",
        english = "Indonesian",
        query = "bahasa indonesia indonesian",
        tags = listOf("bahasa-indonesia", "indonesia", "indonesian"),
        keywords = listOf("indonesia", "indonesian", "bahasa")
    ),
    DiscoverLanguage(
        id = "br",
        label = "Brezhoneg",
        english = "Breton",
        query = "brezhoneg breton",
        tags = listOf("brezhoneg", "breton"),
        keywords = listOf("breton", "brezhoneg", "brittany")
    ),
    DiscoverLanguage(
        id = "ca",
        label = "Català",
        english = "Catalan",
        query = "catalana catalan catalunya",
        tags = listOf("catalana", "catalan", "catalunya"),
        keywords = listOf("catalan", "catalana", "catalunya")
    ),
    DiscoverLanguage(
        id = "cs",
        label = "Čeština",
        english = "Czech",
        query = "cestina czech",
        tags = listOf("cestina", "czech", "cesky"),
        keywords = listOf("czech", "cestina", "cesky")
    ),
    DiscoverLanguage(
        id = "cy",
        label = "Cymraeg",
        english = "Welsh",
        query = "cymraeg welsh",
        tags = listOf("cymru", "welsh", "cymraeg"),
        keywords = listOf("welsh", "cymraeg", "cymru")
    ),
    DiscoverLanguage(
        id = "da",
        label = "Dansk",
        english = "Danish",
        query = "dansk danish",
        tags = listOf("dansk", "danish", "danmark"),
        keywords = listOf("danish", "dansk", "danmark")
    ),
    DiscoverLanguage(
        id = "de",
        label = "Deutsch",
        english = "German",
        query = "deutsch german",
        tags = listOf("deutsch", "german", "germany"),
        keywords = listOf("german", "deutsch", "germany")
    ),
    DiscoverLanguage(
        id = "et",
        label = "Eesti",
        english = "Estonian",
        query = "eesti estonian",
        tags = listOf("eesti", "estonian", "eesti-keel"),
        keywords = listOf("estonian", "eesti")
    ),
    DiscoverLanguage(
        id = "es",
        label = "Español",
        english = "Spanish",
        query = "español spanish espanol",
        tags = listOf("espanol", "español", "spanish"),
        keywords = listOf("spanish", "espanol", "español", "españa", "latinoamerica")
    ),
    DiscoverLanguage(
        id = "eo",
        label = "Esperanto",
        english = "Esperanto",
        query = "esperanto",
        tags = listOf("esperanto"),
        keywords = listOf("esperanto")
    ),
    DiscoverLanguage(
        id = "fr",
        label = "Français",
        english = "French",
        query = "français french francais",
        tags = listOf("francais", "français", "french"),
        keywords = listOf("french", "francais", "français", "france", "quebec")
    ),
    DiscoverLanguage(
        id = "gl",
        label = "Galego",
        english = "Galician",
        query = "galego galician",
        tags = listOf("galego", "galician", "galicia"),
        keywords = listOf("galician", "galego", "galicia")
    ),
    DiscoverLanguage(
        id = "gd",
        label = "Gàidhlig",
        english = "Scots Gaelic",
        query = "gaidhlig gaelic",
        tags = listOf("gaidhlig", "gaelic", "scotland"),
        keywords = listOf("gaelic", "gaidhlig", "scotland")
    ),
    DiscoverLanguage(
        id = "ga",
        label = "Gaeilge",
        english = "Irish",
        query = "gaeilge irish",
        tags = listOf("gaeilge", "irish", "ireland"),
        keywords = listOf("irish", "gaeilge", "ireland")
    ),
    DiscoverLanguage(
        id = "hr",
        label = "Hrvatski",
        english = "Croatian",
        query = "hrvatski croatian",
        tags = listOf("hrvatski", "croatian", "croatia"),
        keywords = listOf("croatian", "hrvatski", "croatia")
    ),
    DiscoverLanguage(
        id = "it",
        label = "Italiano",
        english = "Italian",
        query = "italiano italian",
        tags = listOf("italiano", "italian", "italia"),
        keywords = listOf("italian", "italiano", "italia")
    ),
    DiscoverLanguage(
        id = "lv",
        label = "Latviešu valoda",
        english = "Latvian",
        query = "latviesu latvian",
        tags = listOf("latviesu", "latvian", "latvia"),
        keywords = listOf("latvian", "latviesu", "latvia")
    ),
    DiscoverLanguage(
        id = "lb",
        label = "Lëtzebuergesch",
        english = "Luxembourgish",
        query = "letzebuergesch luxembourgish",
        tags = listOf("letzebuergesch", "luxembourgish"),
        keywords = listOf("luxembourgish", "letzebuergesch", "luxembourg")
    ),
    DiscoverLanguage(
        id = "lt",
        label = "Lietuvių kalba",
        english = "Lithuanian",
        query = "lietuviu lithuanian",
        tags = listOf("lietuviu", "lithuanian", "lithuania"),
        keywords = listOf("lithuanian", "lietuviu", "lithuania")
    ),
    DiscoverLanguage(
        id = "hu",
        label = "Magyar nyelv",
        english = "Hungarian",
        query = "magyar hungarian",
        tags = listOf("magyar", "hungarian", "hungary"),
        keywords = listOf("hungarian", "magyar", "hungary")
    ),
    DiscoverLanguage(
        id = "nl",
        label = "Nederlands",
        english = "Dutch",
        query = "nederlands dutch",
        tags = listOf("nederlands", "dutch", "netherlands"),
        keywords = listOf("dutch", "nederlands", "netherlands")
    ),
    DiscoverLanguage(
        id = "no",
        label = "Norsk",
        english = "Norwegian",
        query = "norsk norwegian",
        tags = listOf("norsk", "norwegian", "norge"),
        keywords = listOf("norwegian", "norsk", "norge")
    ),
    DiscoverLanguage(
        id = "pl",
        label = "Polski",
        english = "Polish",
        query = "polski polish",
        tags = listOf("polsku", "polski", "polish"),
        keywords = listOf("polish", "polski", "polska")
    ),
    DiscoverLanguage(
        id = "pt",
        label = "Português",
        english = "Portuguese",
        query = "português portuguese brasil",
        tags = listOf("portugues", "português", "portuguese"),
        keywords = listOf("portuguese", "portugues", "português", "brasil", "brazil", "portugal")
    ),
    DiscoverLanguage(
        id = "ro",
        label = "Română",
        english = "Romanian",
        query = "romana romanian",
        tags = listOf("romana", "romanian"),
        keywords = listOf("romanian", "romana", "românia")
    ),
    DiscoverLanguage(
        id = "sk",
        label = "Slovenčina",
        english = "Slovak",
        query = "slovencina slovak",
        tags = listOf("slovencina", "slovak", "slovensko"),
        keywords = listOf("slovak", "slovencina", "slovensko")
    ),
    DiscoverLanguage(
        id = "sl",
        label = "Slovenščina",
        english = "Slovenian",
        query = "slovenscina slovenian",
        tags = listOf("slovenscina", "slovenian", "slovenija"),
        keywords = listOf("slovenian", "slovenscina", "slovenija")
    ),
    DiscoverLanguage(
        id = "fi",
        label = "Suomi",
        english = "Finnish",
        query = "suomi finnish",
        tags = listOf("suomi", "finnish", "finland"),
        keywords = listOf("finnish", "suomi", "finland")
    ),
    DiscoverLanguage(
        id = "sv",
        label = "Svenska",
        english = "Swedish",
        query = "svenska swedish",
        tags = listOf("svenska", "swedish", "sverige"),
        keywords = listOf("swedish", "svenska", "sverige")
    ),
    DiscoverLanguage(
        id = "tr",
        label = "Türkçe",
        english = "Turkish",
        query = "türkçe turkish",
        tags = listOf("turkce", "türkçe", "turkish"),
        keywords = listOf("turkish", "turkce", "türkçe", "turkiye")
    ),
    DiscoverLanguage(
        id = "el",
        label = "Ελληνικά",
        english = "Greek",
        query = "ελληνικά greek",
        tags = listOf("ελληνικά", "greek", "greece"),
        keywords = listOf("greek", "ελληνικά", "greece")
    ),
    DiscoverLanguage(
        id = "bg",
        label = "Български език",
        english = "Bulgarian",
        query = "български bulgarian",
        tags = listOf("български", "bulgarian"),
        keywords = listOf("bulgarian", "български")
    ),
    DiscoverLanguage(
        id = "ru",
        label = "Русский язык",
        english = "Russian",
        query = "русский russian",
        tags = listOf("русский", "russian"),
        keywords = listOf("russian", "русский", "russia")
    ),
    DiscoverLanguage(
        id = "uk",
        label = "Українська мова",
        english = "Ukrainian",
        query = "українська ukrainian",
        tags = listOf("українська", "ukrainian"),
        keywords = listOf("ukrainian", "українська", "ukraine")
    ),
    DiscoverLanguage(
        id = "hi",
        label = "हिन्दी",
        english = "Hindi",
        query = "हिन्दी hindi",
        tags = listOf("हिन्दी", "hindi"),
        keywords = listOf("hindi", "हिन्दी", "india")
    ),
    DiscoverLanguage(
        id = "ja",
        label = "日本語",
        english = "Japanese",
        query = "日本語 japanese",
        tags = listOf("日本語", "japanese", "nihongo"),
        keywords = listOf("japanese", "日本語", "nihongo", "日本")
    ),
    DiscoverLanguage(
        id = "zh-TW",
        label = "國語",
        english = "Taiwanese Mandarin",
        query = "國語 taiwanese mandarin",
        tags = listOf("國語", "taiwanese", "mandarin"),
        keywords = listOf("taiwanese", "mandarin", "國語", "taiwan")
    )
)
