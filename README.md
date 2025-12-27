# EyeVector

A Minecraft Fabric mod that calculates stronghold coordinates using triangulation from Eye of Ender throw trajectories.

## Requirements

- **Minecraft**: 1.21 - 1.21.4
- **Fabric Loader**: 0.16.0+
- **Fabric API**: Required
- **Java**: 21+

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/mod/fabric-api)
2. Download EyeVector from [Releases](../../releases)
3. Place the `.jar` file in your `.minecraft/mods` folder

## Usage

1. Throw an Eye of Ender at your first location
2. Move at least 30 blocks away
3. Throw another Eye of Ender
4. The mod displays stronghold coordinates in chat

## Commands

All commands start with `/eyevector`:

- `/eyevector mode <2|3>` - Set measurement mode (2 or 3 throws)
- `/eyevector distance <16-500>` - Set minimum distance between throws (default: 30)
- `/eyevector precision <low|medium|high>` - Set precision level (default: medium)
- `/eyevector reset` - Clear recorded measurements
- `/eyevector status` - Show current settings
- `/eyevector` - Show help

## Tips

- **Spacing**: The further apart your throws, the more accurate the result
- **3-Point Mode**: Use for maximum accuracy (`/eyevector mode 3`)
- **Precision**: Medium is balanced for most users

## License

MIT License
