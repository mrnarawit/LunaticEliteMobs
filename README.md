# Lunatic Elite Mobs

![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-62B47A)
![NeoForge 21.1](https://img.shields.io/badge/NeoForge-21.1.x-E0662E)
![License MIT](https://img.shields.io/badge/License-MIT-blue)

Hostile mobs sometimes spawn as **Elites**: tougher, faster, named after their tier and wrapped in an aura that gets
more menacing at every tier. Elites give more XP and drop extra loot, from enchanted gear to RPG Series equipment.

## Features

- **4 elite tiers** (Elite, Empowered, Warlord, Diabolic) with more health, damage, speed, armor and XP.
- **Tiered auras**: red particles, orbiting orbs, spectral blades, and a magic sigil with shadow wings. Auras scale with
  the mob's hitbox, so they fit modded mobs of any size.
- **Name prefixes** in tier colors, e.g. *Diabolic Zombie*.
- **Bonus loot**: enchanted weapons, RPG Series gear ranked by its stats, a weighted pool of materials, and rare extras.
- **Detailed filters**: blacklist and whitelist by id, tag or whole mod, dimension blacklist, guaranteed elites,
  boss exclusion.
- **Plays well with In Control** and other health-scaling mods: the elite roll happens after they finish.
- Commands to summon, create, clear and inspect elites.

## Tiers

| Tier | Prefix | Share of elites | Aura | Health | Attack | Speed | Armor | Knockback res. | XP |
|:---:|---|:---:|---|:---:|:---:|:---:|:---:|:---:|:---:|
| 1 | Elite (red) | 55% | Red particles | +40% | +5 | +15% | +10 | 20% | x2 |
| 2 | Empowered (gold) | 25% | Orbiting orbs | +60% | +7 | +17% | +12 (+2 toughness) | 40% | x3 |
| 3 | Warlord (purple) | 13% | Spectral blades | +80% | +9 | +20% | +15 (+4) | 60% | x4 |
| 4 | Diabolic (dark red, bold) | 7% | Sigil + shadow wings | +100% | +12 | +22% | +18 (+6) | 80% | x5 |

- Base chance for a hostile mob to become an elite: **1%** (`eliteChancePercent`, 0.01% to 100%).
- Strong mobs have a minimum tier: if (max health x 1.4) is above 500 / 1000 / 1500, the elite is at least
  tier 2 / 3 / 4. Above that minimum the tier is picked with `randomTierWeight`.
- Elites always spawn at full health.
- The prefix changes the mob's name (shown in Jade, death messages and so on) without a floating name tag.
  Set `alwaysShowName = true` on a tier to show it above the mob.

### Which mobs can become elites

- Only hostile mobs. Neutral mobs, villagers, animals and tamed mobs never do.
- Bosses and mobs with more than 1000 max health are excluded, unless listed in `guaranteedElites`.
- Mobs from monster spawners are excluded by default (`allowFromSpawners`).
- The blacklist always wins.

### In Control and health-scaling mods

The elite roll waits **5 ticks** after a mob spawns (`rollDelayTicks`). In Control and similar mods set the mob's
health first, and the elite bonus is applied on top: a mob that In Control sets to 924 health becomes 1848 as a
tier 4 elite. Increase `rollDelayTicks` if another mod changes health later than that.

## Loot

The mob's normal drops are kept. Elites killed by a player drop these **on top**:

1. **Enchanted weapons** (`gearItems`): swords, axes, bows, crossbows, tridents.
2. **RPG Series gear** (`rpg*`): 75% armor, the rest weapons and accessories, ranked automatically by their stats.
   Without RPG mods, enchanted vanilla armor drops instead (`rpgFallbackItems`).
3. **Treasure pool** (`treasurePool`): weighted materials, rolled several times.
4. **Extras** (`extraDrops`): each item rolled separately.

| | Tier 1 | Tier 2 | Tier 3 | Tier 4 |
|---|---|---|---|---|
| Enchanted weapon | 1 roll, 15% (iron, lv 5-12) | 1 roll, 20% (iron, lv 10-18) | 1 roll, 25% (diamond, lv 15-24) | 1 roll, 30% (diamond, lv 20-30) |
| RPG gear | 1 roll, 15% | 1 roll, 25% | 2 rolls, 35% each | 2 rolls, 45% each |
| RPG gear power range | 0-45% | 15-65% | 35-85% | 55-100% |
| Treasure pool | 2 rolls, 55% each | 2 rolls, 70% each | 3 rolls, 80% each | 4 rolls, 85% each |
| Orb of Oblivion | 1% | 3% | 6% | 12% |
| Heart of the Sea | - | - | 1% | 3% |
| Enchanted Golden Apple | - | - | - | 2% |

<details>
<summary>Treasure pool contents</summary>

Amount per roll, with the chance of that entry when a roll succeeds.

| Item | Tier 1 | Tier 2 | Tier 3 | Tier 4 |
|---|---|---|---|---|
| Gold Ingot | 1-3 (20%) | 2-4 (17%) | 2-6 (13%) | 3-8 (11%) |
| Lapis Lazuli | 2-6 (20%) | 3-8 (15%) | 4-10 (11%) | 6-12 (9%) |
| Amethyst Shard | 1-4 (18%) | 2-5 (15%) | 3-6 (11%) | 4-8 (9%) |
| Nether Quartz | 2-6 (18%) | 3-8 (15%) | 4-10 (11%) | 6-12 (9%) |
| Emerald | 1-2 (12%) | 1-3 (13%) | 2-4 (13%) | 3-6 (12%) |
| Enchanted Book (random) | 1 (6%) | 1 (10%) | 1 (13%) | 1 (16%) |
| Armor Trim Template (random) | 1 (3%) | 1 (4%) | 1 (6%) | 1 (7%) |
| Nautilus Shell | 1 (3%) | 1 (5%) | 1 (7%) | 1-2 (7%) |
| Diamond | - | 1 (5%) | 1-2 (9%) | 1-3 (12%) |
| Golden Apple | - | - | 1 (5%) | 1-2 (7%) |
| Netherite Scrap | - | - | - | 1 (2%) |

Enchanted books use the tier's enchant levels and never contain Mending. Trim templates come from
`#minecraft:trim_templates` (armor trims only, no Netherite Upgrade).
</details>

## Configuration

| File | Contents |
|---|---|
| `config/lunaticelitemobs-common.toml` | All gameplay: chances, tiers, stats, filters, loot |
| `config/lunaticelitemobs-client.toml` | Auras: on/off, quality (LOW / MEDIUM / HIGH), render distance, colors, size, speed |

Both can also be edited in game: **Mods → Lunatic Elite Mobs → Config**.
Aura pieces can be mixed freely per tier: `styles = ["particles", "orbs", "blades", "sigil", "wings"]`.

Entity lists accept an id (`"minecraft:zombie"`), a tag (`"#minecraft:raiders"`) or a whole mod (`"@alexsmobs"`):

```toml
[filters]
entityBlacklist = ["minecraft:piglin", "#minecraft:raiders"]
entityWhitelist = ["mowziesmobs:foliaath"]
whitelistOnly = false               # true = only whitelisted mobs can become elites
guaranteedElites = [
  "minecraft:wither_skeleton",      # always elite, random tier
  "minecraft:evoker;3",             # always tier 3
  "#minecraft:raiders;random;50",   # 50% chance, random tier
  "@mowziesmobs;4"                  # every mob from this mod is tier 4
]
```

Loot entry formats:

```toml
gearItems        = ["minecraft:diamond_sword;3", "#minecraft:swords"]               # id or #tag ; weight
treasurePool     = ["minecraft:gold_ingot;2;6;14", "minecraft:book;1;1;14;enchant"] # id ; min ; max ; weight [; enchant]
extraDrops       = ["skill_tree_rpgs:orb_of_oblivion;1;1;6"]                        # id ; min ; max ; chance% [; enchant]
rpgFallbackItems = ["minecraft:diamond_helmet", "#c:armors;2"]
rpgItemBlacklist = ["wizards:staff_arcane", "@relics"]
rpgExtraItems    = ["@simplyswords", "#c:armors"]
```

Unknown ids and empty tags are skipped quietly. Modpacks can also use the datapack tags
`#lunaticelitemobs:blacklist` and `#lunaticelitemobs:whitelist`.

Config files keep their old values when the mod is updated. After an update, delete
`config/lunaticelitemobs-common.toml` so the new defaults are written.

## Commands

Operator only (permission level 2).

```
/lem summon minecraft:zombie 4             Summon a tier 4 elite where you stand
/lem summon minecraft:skeleton random      Summon with a random tier (normal rules)
/lem summon minecraft:husk 3 ~ ~ ~5 10     Summon 10 tier 3 elites at a position
/lem make <targets> <tier>                 Turn existing mobs into elites
/lem clear <targets>                       Remove elite status (restores the name)
/lem info <target>                         Show tier, health and eligibility
/lem reloadloot                            Rescan RPG items for the loot pool
```

## Compatibility

- **RPG Series / RPG Series Plus**: gear from `wizards`, `paladins`, `rogues`, `archers`, `jewelry`, `relics`,
  `arsenal`, `armory`, `druids`, `bard`, `elemental_wizards`, `forcemaster`, `berserker`, `more_rpg_classes`,
  `spellbladenext`, `deathknights` and `witcher` is ranked and dropped automatically (`rpgModNamespaces`).
  Mods that are not installed are ignored.
- **Skill Tree RPGs**: Orb of Oblivion is in `extraDrops` by default.
- **In Control** and health-scaling mods: see [In Control and health-scaling mods](#in-control-and-health-scaling-mods).
- **Jade**: shows the prefixed name.

No other mod is required.

## Installation

- Minecraft **1.21.1** with **NeoForge 21.1.x**
- Install on **both the client and the server** (auras are drawn on the client and synced from the server).

## Building from source

Requires JDK 21.

```
Windows:      .\gradlew.bat build
macOS/Linux:  ./gradlew build
```

The jar is written to `build/libs/lunaticelitemobs-1.0.0.jar`. Use `gradlew runClient` to test in a dev client.

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## License

[MIT](LICENSE). Made by naravit.
