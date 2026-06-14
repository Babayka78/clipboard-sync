import os
import tempfile
from pathlib import Path
from flask import Flask, request, jsonify
from AppKit import NSPasteboard, NSPasteboardTypeString, NSImage, NSURL

app = Flask(__name__)

# Directory where files are saved when the user chooses "File"
SAVE_DIR = Path.home() / "Documents" / "ClipboardSync"


def copy_text_to_clipboard(text):
    pb = NSPasteboard.generalPasteboard()
    pb.clearContents()
    pb.setString_forType_(text, NSPasteboardTypeString)


def copy_image_to_clipboard(image_path):
    """Copy an image file to the Mac clipboard so Cmd+V pastes the image."""
    pb = NSPasteboard.generalPasteboard()
    pb.clearContents()
    image = NSImage.alloc().initWithContentsOfFile_(image_path)
    if not image:
        return False
    pb.writeObjects_([image])
    return True


def copy_file_url_to_clipboard(file_path):
    """Copy a file URL to the Mac clipboard so Cmd+V in Finder pastes the file."""
    pb = NSPasteboard.generalPasteboard()
    pb.clearContents()
    url = NSURL.fileURLWithPath_(file_path)
    pb.writeObjects_([url])


@app.route('/ping', methods=['GET'])
def ping():
    """Health-check endpoint — Android uses this to verify the server is alive."""
    return jsonify({"status": "ok"})


@app.route('/share/text', methods=['POST'])
def share_text():
    if request.is_json:
        data = request.json
        text = data.get('text', '')
    else:
        text = request.get_data(as_text=True)

    if not text:
        return jsonify({"error": "No text provided"}), 400

    copy_text_to_clipboard(text)
    preview = text[:50] + ('...' if len(text) > 50 else '')
    print(f"[ClipSync] Text copied to clipboard: {preview}")
    return jsonify({"status": "ok"})


@app.route('/share/file', methods=['POST'])
def share_file():
    """
    Universal file endpoint.
    Form fields:
      - file  : the uploaded file (required)
      - action: 'clipboard' (default) or 'save'
    """
    if 'file' not in request.files:
        return jsonify({"error": "No file part in request"}), 400

    file = request.files['file']
    if not file.filename:
        return jsonify({"error": "No selected file"}), 400

    action = request.form.get('action', 'clipboard')
    filename = file.filename

    if action == 'save':
        # Save the file to ~/Documents/ClipboardSync
        SAVE_DIR.mkdir(parents=True, exist_ok=True)
        dest_path = str(SAVE_DIR / filename)
        file.save(dest_path)
        print(f"[ClipSync] File saved: {dest_path}")
        return jsonify({"status": "ok", "saved_to": dest_path})

    else:
        # action == 'clipboard'
        mime = file.content_type or ''
        temp_path = os.path.join(tempfile.gettempdir(), filename)
        file.save(temp_path)

        if mime.startswith('image/'):
            # For images: copy the image data so Cmd+V pastes the image itself
            success = copy_image_to_clipboard(temp_path)
            try:
                os.remove(temp_path)
            except OSError:
                pass
            if success:
                print(f"[ClipSync] Image copied to clipboard: {filename}")
                return jsonify({"status": "ok"})
            else:
                return jsonify({"error": "Failed to load image"}), 500
        else:
            # For all other files: copy as a file URL so Cmd+V in Finder works
            copy_file_url_to_clipboard(temp_path)
            print(f"[ClipSync] File URL copied to clipboard: {temp_path}")
            return jsonify({"status": "ok"})


if __name__ == '__main__':
    print("=" * 40)
    print("  Clipboard Sync Server")
    print("  Listening on 0.0.0.0:8766")
    print("=" * 40)
    app.run(host='0.0.0.0', port=8766)
