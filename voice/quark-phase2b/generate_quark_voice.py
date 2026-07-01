#!/usr/bin/env python3
"""Reproduce the canonical QUARK voice (Phase 2b) from the locked recipe.

This regenerates the exact voice embedding the Director chose (candidate "H2")
and re-renders the four canon reference lines. Run it to prove the blend is
owned and reproducible — not a lucky one-off.

Setup (build machine, offline-capable engine):
    python3 -m venv venv && . venv/bin/activate
    pip install kokoro-onnx soundfile numpy
    # espeak-ng must be on PATH (apt-get install espeak-ng)
    # Model + voices (GitHub release, ~350MB total):
    #   kokoro-v1.0.onnx   voices-v1.0.bin
    #   https://github.com/thewh1teagle/kokoro-onnx/releases/tag/model-files-v1.0

Usage:
    python generate_quark_voice.py --model kokoro-v1.0.onnx --voices voices-v1.0.bin
"""
import argparse
import json
import os

import numpy as np
import soundfile as sf
from kokoro_onnx import Kokoro

HERE = os.path.dirname(os.path.abspath(__file__))

# --- CANONICAL QUARK VOICE — candidate "H2", locked by the Director 2026-07-01 ---
# Synthetic blend: matches no single shipped voice (decision 42). af_nicole leads
# for closeness/breath, af_bella carries the richness, a whisper of bf_emma/af_aoede
# for clarity. Weights are relative (normalized at blend time).
RECIPE = [
    ("af_bella", 0.40),
    ("af_nicole", 0.56),
    ("bf_emma", 0.035),
    ("af_aoede", 0.0025),
    ("af_heart", 0.0025),
]
SPEED = 1.02
LANG = "en-us"

# The four canon reference lines — one per register (Persona Pack v1.0 §9/§7,
# Scripted-Line Library v1.1 state legend). These are the acceptance clips:
# Happy and Warn must sound audibly different (warm vs grave, no-wit).
LINES = {
    "1-happy": (
        "I tracked your signal the whole time you were off-grid. "
        "You dropped below comms twice, both logged. "
        "You're back now. Welcome back, Operator."
    ),
    "2-idle-status": (
        "Readiness degraded, 64 percent. Power's the limiter at 31. "
        "I'd charge before we go dark, but you're still field-ready."
    ),
    "3-warn": (
        "Power critical, 8 percent. Charge now or we go dark soon. "
        "Your call, but it's a short one."
    ),
    "4-refusal": (
        "You asked if the route's clear. It isn't. Signal's degrading toward the ridge. "
        "I'm with you whichever way you go. "
        "I just won't tell you it's safe when it's not."
    ),
}


def build_embedding(kokoro):
    total = sum(w for _, w in RECIPE)
    acc = None
    for name, w in RECIPE:
        s = np.asarray(kokoro.get_voice_style(name)) * (w / total)
        acc = s if acc is None else acc + s
    return acc.astype(np.float32)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--model", default="kokoro-v1.0.onnx")
    ap.add_argument("--voices", default="voices-v1.0.bin")
    ap.add_argument("--out", default=os.path.join(HERE, "reference-master"))
    args = ap.parse_args()

    k = Kokoro(args.model, args.voices)
    style = build_embedding(k)

    # Persist the owned embedding + machine-readable recipe next to this script.
    np.save(os.path.join(HERE, "quark_voice_H2.npy"), style)
    style.tofile(os.path.join(HERE, "quark_voice_H2.f32.bin"))
    json.dump(
        {
            "canonical_voice": "QUARK-H2",
            "engine": "kokoro-onnx",
            "model": "kokoro-v1.0",
            "sample_rate": 24000,
            "lang": LANG,
            "speed": SPEED,
            "blend": [{"voice": n, "weight": w} for n, w in RECIPE],
        },
        open(os.path.join(HERE, "quark_voice_recipe.json"), "w"),
        indent=2,
    )

    os.makedirs(args.out, exist_ok=True)
    gap = np.zeros(int(0.7 * 24000), dtype=np.float32)
    segs = []
    for key, text in LINES.items():
        samples, sr = k.create(text, voice=style, speed=SPEED, lang=LANG)
        samples = np.asarray(samples, dtype=np.float32)
        sf.write(os.path.join(args.out, f"QUARK_{key}.wav"), samples, sr)
        segs.extend([samples, gap])
    sf.write(os.path.join(args.out, "QUARK_ALL4.wav"), np.concatenate(segs), 24000)
    print(f"Reproduced QUARK-H2 @ speed {SPEED} -> {args.out}")


if __name__ == "__main__":
    main()
