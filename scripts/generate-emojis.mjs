/* Generates app/src/main/res/raw/emojis_{en,pt}.json from Emojibase.
   Usage:
     node scripts/generate-emojis.mjs
   By default it reads ../OLTN/node_modules/emojibase-data (the reference
   checkout). Override with EMOJIBASE_DIR=/path/to/emojibase-data. */

import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const DEST = resolve(ROOT, "app/src/main/res/raw");
const EMOJIBASE_DIR = process.env.EMOJIBASE_DIR ?? resolve(ROOT, "../OLTN/node_modules/emojibase-data");

mkdirSync(DEST, { recursive: true });

const GROUP_LABELS = {
    en: {
        0: "Smileys & emotion",
        1: "People & body",
        3: "Animals & nature",
        4: "Food & drink",
        5: "Travel & places",
        6: "Activities",
        7: "Objects",
        8: "Symbols",
        9: "Flags",
    },
    pt: {
        0: "Sorrisos e emoções",
        1: "Pessoas e corpo",
        3: "Animais e natureza",
        4: "Comida e bebida",
        5: "Viagens e lugares",
        6: "Atividades",
        7: "Objetos",
        8: "Símbolos",
        9: "Bandeiras",
    },
};

for (const locale of ["en", "pt"]) {
    const source = JSON.parse(
        readFileSync(resolve(EMOJIBASE_DIR, `${locale}/compact.json`), "utf8"),
    );

    const byGroup = new Map();
    for (const entry of source) {
        if (typeof entry.group !== "number" || entry.group === 2) continue;
        if (!entry.unicode) continue;
        const item = { c: entry.unicode, l: entry.label };
        if (entry.tags?.length) item.t = entry.tags.slice(0, 10);
        const skins = entry.skins?.map((skin) => skin.unicode).filter(Boolean);
        if (skins?.length) item.s = skins;
        if (!byGroup.has(entry.group)) byGroup.set(entry.group, []);
        byGroup.get(entry.group).push(item);
    }

    const groups = [...byGroup.keys()]
        .sort((a, b) => a - b)
        .map((id) => ({
            id,
            label: GROUP_LABELS[locale][id] ?? String(id),
            emojis: byGroup.get(id),
        }));

    writeFileSync(resolve(DEST, `emojis_${locale}.json`), JSON.stringify(groups));
    const count = groups.reduce((sum, group) => sum + group.emojis.length, 0);
    console.log(`${locale}: ${count} emojis in ${groups.length} groups`);
}
