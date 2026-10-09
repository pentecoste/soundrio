# Soundrio on Linux

`install.sh` sets up everything Soundrio needs to be heard by other people in
voice apps (Discord, Mumble, browser calls) on a PipeWire desktop:

- three virtual audio devices, wired together automatically at every boot;
- Soundrio running headless as a systemd user service;
- a switch to turn the soundboard off (for calls where a stray hotkey must not
  play anything) and back on.

Everything is installed per user; root is never needed.

## Requirements

- PipeWire with `pipewire-pulse` and WirePlumber
- an X11 session (the global hotkeys are read through X)
- Maven and a JDK, only if the runtime image has to be built

## Install

```sh
linux/install.sh
```

With no options it builds the runtime image (`mvn clean javafx:jlink`), copies
it to `~/.local/share/soundrio`, and uses the current default source and sink
as microphone and speakers. Useful options:

| Option | Meaning |
|---|---|
| `--mic NAME` | source to mix with the soundboard (`pactl list short sources`) |
| `--speakers NAME` | sink where you hear the soundboard (`pactl list short sinks`) |
| `--dir DIR` | use or create the runtime image in `DIR` |
| `--update` | rebuild the image and copy it over; `bindings.json` is kept |
| `--keep-default-mic` | do not change the default source |
| `--no-start` | only write files |

The script can be re-run at any time. Microphone and speakers are remembered
from the previous run, and the audio devices are restarted only if their
configuration actually changed.

## What you get

```
microphone ---------------------------------.
                                            v
Soundrio -> MixedOutput --+--> SoundBoard --> SoundBoardMic -> voice apps
                          '--> speakers
```

- **SoundBoardMic** ("Microphone + Soundboard") becomes the default source, so
  apps that record from the default device get your voice plus the soundboard.
  Your plain microphone is still there to be picked explicitly.
- **MixedOutput** is where Soundrio plays: you hear it, and so do the others.
- **SoundBoard** is the sink in between; anything else played into it is also
  sent to the others, without reaching your speakers.

Files written:

| File | Purpose |
|---|---|
| `~/.config/pipewire/soundboard.conf` | the virtual devices |
| `~/.config/systemd/user/soundboard-audio.service` | runs them, follows PipeWire restarts |
| `~/.config/systemd/user/soundrio.service` | Soundrio headless |
| `~/.local/bin/soundrio-ctl` | on/off switch |
| `~/.local/bin/soundrio` | opens the GUI to edit bindings |

## Daily use

```sh
soundrio-ctl toggle     # or: on / off
soundrio-ctl status     # prints on / off
soundrio                # GUI; the service is paused while the window is open
```

When the soundboard is off the process is not running, so no hotkey can
trigger a sound. The choice is remembered across reboots.

`soundrio.service` is deliberately not enabled at login: it must start after
X is up. Start it from your window manager instead.

### i3

```
exec --no-startup-id soundrio-ctl autostart
bindsym $mod+Shift+m exec --no-startup-id soundrio-ctl toggle
```

### polybar

```ini
[module/soundrio]
type = custom/ipc
hook-0 = soundrio-ctl label
initial = 1
click-left = soundrio-ctl toggle
```

Add `soundrio` to the bar's modules and set `enable-ipc = true`.

## Using a noise-suppression filter as microphone

A virtual source such as an RNNoise filter chain works as `--mic`, but its own
capture must be pinned to the hardware microphone. Otherwise it follows the
default source, which is now SoundBoardMic, which is fed by the filter itself:

```
capture.props = {
    target.object      = "alsa_input.usb-..."
    node.dont-fallback = true
}
```

## Uninstall

```sh
systemctl --user disable --now soundboard-audio.service
systemctl --user stop soundrio.service
rm ~/.config/pipewire/soundboard.conf
rm ~/.config/systemd/user/soundboard-audio.service ~/.config/systemd/user/soundrio.service
rm ~/.local/bin/soundrio-ctl ~/.local/bin/soundrio
systemctl --user daemon-reload
```

Then pick your microphone again as default source, and remove
`~/.local/share/soundrio` if you no longer want the runtime image (your
`bindings.json` is in its `bin/` directory).
