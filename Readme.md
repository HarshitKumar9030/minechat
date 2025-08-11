# MineChat

Minecraft chat plugin with ranks, friends, groups, MongoDB logging, and a real-time web interface (REST + WebSocket).

• Build on top: see docs/PLUGIN_API.md for FriendAPI & GroupAPI services

## Features
- Chat formatting with ranks (Vault/LuckPerms/PowerRanks supported)
- Private messages with reply and database logging
- Friend system: requests, accepts/denies, list, remove
- Group chat: create, join (name/code), invite flow, roles, moderation, MOTD, announcements
- MongoDB logging for chats and messages
- Embedded REST API and WebSocket server for the web app
- Web UI (Next.js) with real-time chat, friends, groups

## Installation
1. Build the plugin with Maven and place the shaded jar in your server's plugins folder.
2. Configure `src/main/resources/config.yml` or the generated one after first run.
3. Ensure MongoDB is running and connection details are correct.
4. Restart the server.

## Configuration Highlights (`config.yml`)
- mongodb: connection-string, database-name, collection-name
- chat: enable-logging, format, filter, default rank, max-message-length
- ranks: preferred-system (auto/vault/luckperms/powerranks), debug
- web: enable-api, port, websocket-port, require-authentication, interface-url
- private-messages: aliases, format
- friends: max-friends, notifications
- chat-groups: max per player, members per group, format

## Player Commands

- /minechat
	- setpassword <password> — enable web access
	- removepassword — disable web access
	- status — check web access and URL
	- weburl — show the web interface URL
	- reload — reload config (permission: minechat.admin)
	- rankdebug [player] — rank system debug (permission: minechat.ranks.debug)
	- Aliases: /mc, /mchat

- Private Messages
	- /msg <player> <message>
	- /pm <player> <message>
	- /tell <player> <message>
	- /whisper <player> <message> (alias: /w)
	- /reply <message> (alias: /r)

- Friends (/friend | aliases: /friends, /f)
	- add <player>
	- accept <player>
	- deny <player>
	- remove <player>
	- list
	- requests

- Groups (/group | aliases: /groups, /g)
	- create <name> [description] [--private]
	- join <name|code>
	- leave <group>
	- delete|disband <group>
	- invite <group> <player>
	- accept <group>
	- deny <group>
	- kick <group> <player> [reason]
	- ban <group> <player> [reason]
	- unban <group> <player>
	- promote <group> <player>
	- demote <group> <player>
	- admin <group> <player>
	- mod|moderator <group> <player>
	- chat|msg <group> <message>
	- announce|announcement <group> <message>
	- motd <group> [message]
	- mute <group> <player> [duration]
	- unmute <group> <player>
	- clear <group>
	- list
	- members <group>
	- info <group>
	- invites|pending
	- search <query>
	- settings|config <group>
	- private <group>
	- public <group>

## Permissions
- minechat.admin — admin commands (/minechat reload)
- minechat.use — base usage
- minechat.web — set web passwords
- minechat.pm — private messages
- minechat.friends.add — send friend requests
- minechat.friends.unlimited — bypass friend limit
- minechat.groups.create — create groups
- minechat.groups.unlimited — bypass group limits
- minechat.ranks.debug — rank debug
- minechat.bypass.filter — bypass chat filter

## REST API and WebSocket
- REST base: http://localhost:8080/api
- WebSocket: ws://localhost:8081/ws

## Endpoints:
- POST /auth
- Friends: /friends, /friend-requests, /send-friend-request, /accept-friend-request, /reject-friend-request, /remove-friend, /cancel-friend-request
- Groups: /groups, /create-group, /delete-group, /join-group, /join-group-by-code, /leave-group, /group-members, /group-invites, /accept-group-invite, /reject-group-invite, /add-announcement, /group-details, /update-group, moderation endpoints
- Messages: /messages, /private-messages, /send-message, /group-messages, /send-group-message
- Users/Players: /users, /players, /search-players, /ranks

WebSocket supports: auth, friend_message, group_message, online players, friend requests, group invites, ping/pong.

## Web UI (Next.js)
- repo path: `web/`
- Dev: `pnpm install` then `pnpm dev`
- Configure `web/src/lib/constants.ts` for API/WS URLs

## Troubleshooting
- Port conflicts: adjust `web.port` or `web.websocket-port`
- MongoDB connection errors: verify connection-string and DB is running
- Auth fails: ensure `/minechat setpassword <pwd>` was used and `web.require-authentication` is true
- CORS: `web.enable-cors` should be true for dev

## License
MIT