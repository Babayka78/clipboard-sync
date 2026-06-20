# Clipboard Sync (Android to Mac)

Clipboard Sync is a seamless, local-network tool that allows you to instantly send text, images, and arbitrary files from your Android device directly to your Mac. 

> **Note:** This is a one-way transfer tool (Android -> Mac). Sending files or clipboard content from Mac back to Android is not currently supported.

You can choose to either **copy the payload to your Mac's clipboard** (ready to be pasted with `Cmd+V`) or **save it as a file** on your Mac.

## Features

- **Universal File Support**: Send text, images, PDFs, ZIPs, or any other files.
- **CP or File**: Every time you share, you are prompted to either copy the content to the Mac's clipboard ("CP") or save it to `~/Documents/ClipboardSync` ("File").
- **Android Share Menu**: Appears in the native Android share sheet for all file types.
- **Quick Settings Tile**: A convenient pull-down tile to instantly send your current Android clipboard to the Mac.
- **Background Mac Server**: Runs silently as a macOS LaunchAgent and starts automatically on login.

## Prerequisites

- **Mac**: Python 3.x, macOS.
- **Android**: Android 7.0+ (for Quick Settings Tile support).
- Both devices must be on the **same local network (Wi-Fi)**.

## Installation

### 1. Mac Server Setup

The Mac component is a lightweight Python Flask server. 

1. Clone or place this repository on your Mac (e.g., in `~/Projects/clipboard-sync`).
2. Open Terminal and navigate to the `mac_app` folder:
   ```bash
   cd clipboard-sync/mac_app
   ```
3. Set up a Python virtual environment and install dependencies:
   ```bash
   python3 -m venv venv
   source venv/bin/activate
   pip install -r requirements.txt
   ```
4. Run the installation script to register the server as a background LaunchAgent:
   ```bash
   # Go back to the project root and run the install script
   cd ..
   bash mac_app/install.sh
   ```
   *(The server will start immediately and automatically launch on future logons).*

### 2. Android App Setup

1. Open the project in Android Studio and build the APK, or install a pre-built APK.
2. Launch the **Clipboard Sync** app on your Android device.
3. Enter your **Mac's Local IP Address** (e.g., `192.168.1.50`). You can find this in your Mac's Network Settings, or by running `ipconfig getifaddr en0` in Terminal.
4. Tap **Save**. 

## Usage

You have two primary ways to send data to your Mac:

### A. The Share Menu (For any file or text)
1. Select text, an image, or any file on your Android device.
2. Tap the native **Share** button.
3. Select **Send to MAC**.
4. A dialog will appear asking if you want to:
   - **CP (copy to clipboard)**: Places the item in your Mac's clipboard. You can immediately hit `Cmd+V` on your Mac in any app or Finder window.
   - **File (save to Documents)**: Saves the item to `~/Documents/ClipboardSync/`.

### B. Quick Settings Tile (For quick clipboard sync)
1. Pull down the Quick Settings panel on your Android device and edit it to add the **Send to MAC** tile.
2. Whenever you copy text on your Android phone, pull down and tap the tile.
3. Choose whether to copy it to the Mac's clipboard or save it as a text file.

## Troubleshooting

- **Target Mac Unreachable**: Ensure the Mac is awake, connected to the same Wi-Fi, and the IP address is correct. Test the server from the Mac terminal using: `curl http://<your-mac-ip>:8766/ping`
- **Server Logs**: Check `mac_app/server.log` and `mac_app/server_error.log` for any errors on the Mac side.
- **Reinstalling**: If you move the project folder to a new location on your Mac, simply run `bash mac_app/install.sh` again to update the paths.
