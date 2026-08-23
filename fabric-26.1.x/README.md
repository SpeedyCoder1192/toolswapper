# ToolSwapper

**Mine stone, hold a pickaxe. Chop wood, hold an axe. Never think about it again.**

ToolSwapper picks the right tool the instant you start breaking a block, and the right weapon the instant you swing at a mob. Works out of the box!

## Works on every block

Most auto-tool mods ship a lookup table: stone → pickaxe, dirt → shovel, and so on. Tables go stale, and they ignore modded blocks entirely.

ToolSwapper doesn't use one. It asks the game two questions about the block in front of you: *what breaks this fastest* and *what's the correct harvest tool*, then it picks the winner. That means it covers every vanilla block, every modded block, and anything a datapack adds, automatically!

| Tool | Handles |
|---|---|
| **Pickaxe** | Stone, ores, deepslate, obsidian, metal blocks, rails, concrete, terracotta… |
| **Shovel** | Dirt, sand, gravel, clay, snow, soul sand, mud… |
| **Axe** | Logs, planks, chests, barrels, pumpkins, bamboo blocks, beehives… |
| **Hoe** | Hay bales, sponges, nether wart blocks, moss, sculk… |
| **Shears** | Leaves, wool, cobwebs, vines, glow lichen… |
| **Sword** | Cobwebs, bamboo |

It stays out of the way where a swap wouldn't help: blocks that already break instantly bare-handed, unbreakable blocks, and anything you're already holding the best tool for.

## Smarter than "correct tool"

- **Silk Touch and Fortune awareness.** Silk for glass, ice and spawners; Fortune for ores and crops. It knows the two are mutually exclusive and picks accordingly.
- **Real break-time prediction.** Efficiency, Haste, Mining Fatigue, being underwater and being airborne all factor into the choice, not just the tool type.
- **Instant-mine detection.** If a different tool crosses the one-tick threshold, that tool wins.
- **Durability protection.** Won't burn your last five points on a netherite pickaxe.

## Combat too

Weapons are ranked by actual attack damage and attack speed read from the item itself, so **modded weapons work without extra work**. Smite, Bane of Arthropods and Impaling are matched against the target using the game's own entity tags, so retagging datapacks are respected.

Choose burst damage or DPS, avoid Knockback for mob farms, favour axes against shield users, or set per-mob overrides.

## Configure it, or don't :D

The defaults are the whole point, since most people never open the config.

If you do want to tune it, there's a lot to change!

Everything is editable live with `/toolswap set <path> <value>`, so no restart, no alt-tabbing.

## Controls

| Key | Action |
|---|---|
| `V` | Toggle on/off |
| *(unbound)* | Cycle profile, suspend while held, force swap, reload config |

A small HUD shows the current state. Turn it off with `/toolswap global set display.hud false`.

## Notes

- **Client-side only.** Nothing to install on the server.
- **Check your server's rules.** Many servers ban auto-tool mods. Inventory-wide search sends container packets that some anticheat plugins flag. `safety.antiCheatSafeMode` restricts the mod to plain hotbar switching if you need it.
- **Mod Menu** is optional, it just adds a settings button.