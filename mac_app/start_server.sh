#!/bin/bash
cd "$(dirname "$0")"

# Create virtual env if not exists
if [ ! -d "venv" ]; then
    echo "Creating virtual environment..."
    python3 -m venv venv
    source venv/bin/activate
    echo "Installing requirements..."
    pip install -r requirements.txt
else
    source venv/bin/activate
fi

echo "Starting server..."
python server.py
