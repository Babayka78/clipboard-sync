#!/usr/bin/env bash
# install.sh — Installs the Clipboard Sync server as a macOS LaunchAgent.
# Run once after cloning the repo or moving it to a new location.
# Usage: bash mac_app/install.sh  (from the project root)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
START_SCRIPT="$SCRIPT_DIR/start_server.sh"
PLIST_LABEL="com.babayka.clipboardsync"
PLIST_DIR="$HOME/Library/LaunchAgents"
PLIST_DEST="$PLIST_DIR/$PLIST_LABEL.plist"
LOG_OUT="$SCRIPT_DIR/server.log"
LOG_ERR="$SCRIPT_DIR/server_error.log"

echo "=== Clipboard Sync Installer ==="
echo "Project root : $PROJECT_ROOT"
echo "Start script : $START_SCRIPT"
echo "LaunchAgent  : $PLIST_DEST"
echo ""

# ── 1. Unload existing service if present ───────────────────────────────────
if launchctl list | grep -q "$PLIST_LABEL" 2>/dev/null; then
    echo "[1/4] Unloading existing LaunchAgent..."
    launchctl unload "$PLIST_DEST" 2>/dev/null || true
else
    echo "[1/4] No existing LaunchAgent found — skipping unload."
fi

# ── 2. Generate the plist ────────────────────────────────────────────────────
echo "[2/4] Writing plist to $PLIST_DEST..."
mkdir -p "$PLIST_DIR"

cat > "$PLIST_DEST" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>$PLIST_LABEL</string>
    <key>ProgramArguments</key>
    <array>
        <string>/bin/bash</string>
        <string>$START_SCRIPT</string>
    </array>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
    <key>StandardOutPath</key>
    <string>$LOG_OUT</string>
    <key>StandardErrorPath</key>
    <string>$LOG_ERR</string>
</dict>
</plist>
EOF

# ── 3. Load the new service ──────────────────────────────────────────────────
echo "[3/4] Loading LaunchAgent..."
launchctl load "$PLIST_DEST"

# ── 4. Verify ────────────────────────────────────────────────────────────────
echo "[4/4] Verifying..."
sleep 1
if launchctl list | grep -q "$PLIST_LABEL"; then
    echo ""
    echo "✅  Clipboard Sync server is now running as a LaunchAgent."
    echo "    It will start automatically on login."
    echo ""
    echo "To check server health:"
    IP=$(ipconfig getifaddr en0 2>/dev/null || echo "<your-mac-ip>")
    echo "    curl http://$IP:8766/ping"
else
    echo ""
    echo "⚠️  LaunchAgent registered, but the service may have exited immediately."
    echo "    Check $LOG_ERR for details."
fi
