#!/bin/sh
# Linux integration for Soundrio: PipeWire virtual devices, systemd user
# services and the enable/disable switch. User-level only, no root.
# Safe to re-run; see linux/README.md.

set -eu

usage() {
    cat <<USAGE
usage: install.sh [options]

  --dir DIR         where the Soundrio runtime image lives
                    (default: \$XDG_DATA_HOME/soundrio). If DIR has no image,
                    one is built with Maven and copied there.
  --update          rebuild the image and copy it over DIR (bindings.json is kept)
  --mic NAME        source to mix with the soundboard
                    (default: the one already configured, else the default source)
  --speakers NAME   sink where you hear the soundboard
                    (default: the one already configured, else the default sink)
  --keep-default-mic  do not make "Microphone + Soundboard" the default source
  --no-start        only write files; do not touch systemd or PipeWire
USAGE
}

die() { echo "install.sh: $*" >&2; exit 1; }

here=$(cd "$(dirname "$0")" && pwd)
repo=$(dirname "$here")
conf="${XDG_CONFIG_HOME:-$HOME/.config}"
bin="$HOME/.local/bin"
dir="${XDG_DATA_HOME:-$HOME/.local/share}/soundrio"
mic= speakers= update=0 start=1 default_mic=1

while [ $# -gt 0 ]; do
    case "$1" in
        --dir)      dir=$2; shift ;;
        --mic)      mic=$2; shift ;;
        --speakers) speakers=$2; shift ;;
        --update)   update=1 ;;
        --keep-default-mic) default_mic=0 ;;
        --no-start) start=0 ;;
        -h|--help)  usage; exit 0 ;;
        *)          usage >&2; exit 2 ;;
    esac
    shift
done

case "$dir" in /*) ;; *) dir="$PWD/$dir" ;; esac

for cmd in pipewire pactl systemctl; do
    command -v "$cmd" >/dev/null || die "$cmd not found (PipeWire with pipewire-pulse is required)"
done

# ---- runtime image ---------------------------------------------------------

image_changed=0
if [ ! -x "$dir/bin/java" ] || [ "$update" = 1 ]; then
    built="$repo/target/Soundrio"
    if [ ! -x "$built/bin/java" ] || [ "$update" = 1 ]; then
        command -v mvn >/dev/null ||
            die "no image in $dir and no Maven to build one (install maven and a JDK, or pass --dir)"
        echo "Building the runtime image..."
        (cd "$repo" && mvn -q -B clean javafx:jlink)
    fi
    mkdir -p "$dir"
    cp -a "$built/." "$dir/"
    image_changed=1
fi

# ---- which microphone, which speakers --------------------------------------

existing="$conf/pipewire/soundboard.conf"
if [ -f "$existing" ]; then
    [ -n "$mic" ] || mic=$(sed -n '/soundboard\.mic\.capture/,/}/s/.*target\.object *= *"\(.*\)".*/\1/p' "$existing" | head -n 1)
    [ -n "$speakers" ] || speakers=$(sed -n '/stream\.rules/,$p' "$existing" |
        sed -n '/node\.name/s/.*"\(.*\)".*/\1/p' | grep -vx SoundBoard | head -n 1)
fi
[ -n "$mic" ] || mic=$(pactl get-default-source)
[ -n "$speakers" ] || speakers=$(pactl get-default-sink)

case "$mic" in
    ""|SoundBoardMic|*.monitor)
        die "cannot use '$mic' as microphone; pass --mic NAME (see: pactl list short sources)" ;;
esac
case "$speakers" in
    ""|SoundBoard|MixedOutput)
        die "cannot use '$speakers' as speakers; pass --speakers NAME (see: pactl list short sinks)" ;;
esac
pactl list short sources | cut -f 2 | grep -qxF "$mic" ||
    echo "Warning: source '$mic' is not there right now; it will be linked when it appears."
pactl list short sinks | cut -f 2 | grep -qxF "$speakers" ||
    echo "Warning: sink '$speakers' is not there right now; it will be linked when it appears."

# ---- files -----------------------------------------------------------------

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

sed -e "s|@MIC@|$mic|" -e "s|@SPEAKERS@|$speakers|" "$here/soundboard.conf.in" > "$tmp/soundboard.conf"
sed -e "s|@DIR@|$dir|g" -e "s|@BIN@|$bin|g" "$here/soundrio.service.in" > "$tmp/soundrio.service"
cat > "$tmp/soundrio" <<GUI
#!/bin/sh
# Soundrio GUI, to edit the bindings. The headless service is stopped while
# the window is open (two instances would both react to the hotkeys) and
# started again afterwards, so it picks up the new bindings.json.
was_active=0
systemctl --user is-active --quiet soundrio.service && was_active=1
[ \$was_active = 1 ] && systemctl --user stop soundrio.service
cd "$dir/bin" && PULSE_SINK=MixedOutput PIPEWIRE_NODE=MixedOutput ./Soundrio "\$@"
[ \$was_active = 1 ] && systemctl --user start soundrio.service
exit 0
GUI

# The audio service only needs a restart when the configuration really
# changed, not when just comments did.
same_conf() {
    [ -f "$2" ] || return 1
    if command -v spa-json-dump >/dev/null; then
        [ "$(spa-json-dump "$1" 2>/dev/null)" = "$(spa-json-dump "$2" 2>/dev/null)" ]
    else
        cmp -s "$1" "$2"
    fi
}
audio_changed=1
same_conf "$tmp/soundboard.conf" "$existing" && audio_changed=0
unit_changed=1
cmp -s "$tmp/soundrio.service" "$conf/systemd/user/soundrio.service" && unit_changed=0

install -Dm644 "$tmp/soundboard.conf" "$existing"
install -Dm644 "$here/soundboard-audio.service" "$conf/systemd/user/soundboard-audio.service"
install -Dm644 "$tmp/soundrio.service" "$conf/systemd/user/soundrio.service"
install -Dm755 "$here/soundrio-ctl" "$bin/soundrio-ctl"
install -Dm755 "$tmp/soundrio" "$bin/soundrio"

echo "Microphone: $mic"
echo "Speakers:   $speakers"
echo "Image:      $dir"

[ "$start" = 1 ] || { echo "Files written (--no-start: services left untouched)."; exit 0; }

# ---- services --------------------------------------------------------------

systemctl --user daemon-reload
systemctl --user enable --quiet soundboard-audio.service
if [ "$audio_changed" = 1 ]; then
    systemctl --user restart soundboard-audio.service
else
    systemctl --user start soundboard-audio.service
fi

if [ "$default_mic" = 1 ]; then
    n=0
    until pactl list short sources | cut -f 2 | grep -qx SoundBoardMic; do
        n=$((n + 1))
        [ "$n" -le 20 ] || die "SoundBoardMic did not show up; check: journalctl --user -u soundboard-audio"
        sleep 0.5
    done
    pactl set-default-source SoundBoardMic
fi

if systemctl --user is-active --quiet soundrio.service; then
    if [ "$image_changed" = 1 ] || [ "$unit_changed" = 1 ]; then
        systemctl --user restart soundrio.service
    fi
elif [ -n "${DISPLAY:-}" ]; then
    "$bin/soundrio-ctl" autostart
fi

case "$mic" in
    alsa_input.*|bluez_input.*) ;;
    *) echo
       echo "Note: '$mic' is a virtual source. If it captures from the default"
       echo "source, pin it to the hardware microphone (target.object): the default"
       echo "source is now SoundBoardMic, which is fed by it." ;;
esac
echo
echo "Done. Start Soundrio from your window manager with: soundrio-ctl autostart"
