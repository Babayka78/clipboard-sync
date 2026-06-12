import os
import tempfile
from flask import Flask, request, jsonify
from AppKit import NSPasteboard, NSPasteboardTypeString, NSImage

app = Flask(__name__)


def copy_text_to_clipboard(text):
    pb = NSPasteboard.generalPasteboard()
    pb.clearContents()
    pb.setString_forType_(text, NSPasteboardTypeString)


def copy_image_to_clipboard(image_path):
    pb = NSPasteboard.generalPasteboard()
    pb.clearContents()
    image = NSImage.alloc().initWithContentsOfFile_(image_path)
    if not image:
        return False
    pb.writeObjects_([image])
    return True


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
    print(f"[ClipSync] Text copied: {preview}")
    return jsonify({"status": "ok"})


@app.route('/share/image', methods=['POST'])
def share_image():
    if 'image' not in request.files:
        return jsonify({"error": "No image part in request"}), 400

    file = request.files['image']
    if not file.filename:
        return jsonify({"error": "No selected file"}), 400

    # Save to a temporary file, then copy to clipboard
    temp_path = os.path.join(tempfile.gettempdir(), file.filename)
    file.save(temp_path)

    success = copy_image_to_clipboard(temp_path)

    # Clean up temp file after NSImage has loaded it into memory
    try:
        os.remove(temp_path)
    except OSError:
        pass

    if success:
        print(f"[ClipSync] Image copied: {file.filename}")
        return jsonify({"status": "ok"})
    else:
        return jsonify({"error": "Failed to load image"}), 500


if __name__ == '__main__':
    print("=" * 40)
    print("  Clipboard Sync Server")
    print("  Listening on 0.0.0.0:8766")
    print("=" * 40)
    app.run(host='0.0.0.0', port=8766)
