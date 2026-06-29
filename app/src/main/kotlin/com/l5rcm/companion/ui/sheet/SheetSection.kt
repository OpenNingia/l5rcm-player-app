package com.l5rcm.companion.ui.sheet

/** Sheet navigation sections with their Cinzel title and decorative kanji (docs §7). */
enum class SheetSection(val title: String, val kanji: String) {
    CHARACTER("Character", "侍"),
    SKILLS("Skills", "技"),
    TECHNIQUES("Techniques", "流"),
    SPELLS("Spells", "呪"),
    KATA_KIHO("Kata & Kiho", "型"),
    MERITS("Merits & Flaws", "縁"),
    EQUIPMENT("Equipment", "刀"),
    MODIFIERS("Modifiers", "雑"),
    NOTES("Notes", "記"),
    ABOUT("About", "設"),
}
