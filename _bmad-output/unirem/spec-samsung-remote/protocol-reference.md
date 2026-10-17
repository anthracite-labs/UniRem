# Protocol reference (UniRem Samsung remote)

Companion to `spec-samsung-remote.md`. Load-bearing wire detail the kernel cites; owned by bmad-spec.

## Endpoints and ports

| Port / scheme | Use | Rule |
|---|---|---|
| `8001` HTTP / WS | Device info `GET /api/v2/`; legacy insecure remote on some models | Plain text; never silently fall back from WSS |
| `8002` HTTPS / WSS | Authenticated channel `wss://<ip>:8002/api/v2/channels/samsung.remote.control?name=<base64(appName)>[&token=<token>]` | Self-signed TV cert; pin per device |
| `55000` TCP | Pre-2016 Orsay legacy | Out of scope |

## Pairing lifecycle

1. SSDP M-SEARCH to `239.255.255.250:1900` under a held `MulticastLock`; then `GET <tv>:8001/api/v2/` for model, `TokenAuthSupport`, and `wifiMac` (persist the MAC for wake).
2. First connect without token: TV shows Allow/Deny for the physical remote to answer.
3. Allow yields `ms.channel.connect`, carrying the token on token-auth models; deny/timeout yields `ms.channel.unauthorized` or close.
4. Persist token plus certificate pin per TV keyed by stable device identity (MAC/device ID), never by IP alone.
5. Later connects attach `&token=` and skip the prompt; design for re-pairing since some firmware re-prompts.

## Message frames (method `ms.remote.control`, channel `samsung.remote.control`)

- Key click: `{"method":"ms.remote.control","params":{"Cmd":"Click","DataOfCmd":"KEY_ENTER","Option":"false","TypeOfRemote":"SendRemoteKey"}}`; hold uses `Press`/`Release` or a repeat loop with coalescing.
- Key names: `KEY_UP/DOWN/LEFT/RIGHT/ENTER/RETURN/HOME/MENU/SOURCE/GUIDE/INFO/TOOLS/POWER/VOLUP/VOLDOWN/MUTE/CHUP/CHDOWN/CH_LIST/0-9/PLAY/PAUSE/STOP/FF/REWIND/RED/GREEN/YELLOW/BLUE`; anything beyond the MVP set sits behind capability flags.
- Text: Base64 UTF-8 goes in `Cmd` with `DataOfCmd:"base64"`, `TypeOfRemote:"SendInputString"`, then `SendInputEnd`; emit `custom.remote.textReceived` before the first text after IME start.
- Pointer: `{"method":"ms.remote.control","params":{"Cmd":"Move","Position":{"x":10,"y":-4,"Time":"0"},"TypeOfRemote":"ProcessMouseDevice"}}`; support is per surface and per model.

## Channel events

`ms.channel.connect` (token carrier), `ms.channel.unauthorized` (re-pair), `ms.channel.ready`, `clientConnect`/`clientDisconnect`, `ms.remote.imeStart`/`imeEnd` (open/close phone keyboard UI, plus a manual type action since delivery is inconsistent), `ms.error`, `ed.installedApp.get` (where supported).

## TLS rule

Trust-on-first-use pinned to the leaf SPKI, confirmed in the pairing UI; every later connect throws on mismatch into a re-pair flow; one scoped client per TV; hostname check relaxed only inside the pinned client.

## Wake packet

102 bytes (`FF` x6 + MAC x16) over UDP to ports 9 and 7, sent to the subnet-directed broadcast plus `255.255.255.255`; requires the stored MAC and the TV's "Power On with Mobile" / Network Standby setting.

## DLNA flow

SSDP discovery of a MediaRenderer advertising AVTransport (plus RenderingControl for volume); SOAP `SetAVTransportURI` with phone URL plus DIDL-Lite metadata (`protocolInfo`, `DLNA.ORG_OP=01` for byte-seek); `Play`/`Pause`/`Stop`/`Seek`; TV fetches via `GET`/`HEAD`/`Range`. Subtitles ride extra DIDL-Lite metadata and vary by firmware; start validation with JPEG, MP3, H.264/AAC-in-MP4.
