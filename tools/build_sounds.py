#!/usr/bin/env python3
"""Synthesises the mod's sounds and writes them as mono Ogg Vorbis (Minecraft only places mono sounds in space).

The officer's whistle is a pea whistle of the trenches (the kind blown to send men over the top): a loud tone near
2.9 kHz, broken up thirty-odd times a second by the pea spinning in the chamber, which shakes both its pitch and its
loudness; a little breath noise round the tone and a faint second harmonic. Each blast swells in over a few
hundredths of a second with the pitch rising to its note, holds, and falls away with the pitch dropping.

Needs numpy and ffmpeg (with libvorbis).

Usage: python3 tools/build_sounds.py [--wav DIR]   (--wav also keeps the uncompressed files, to listen to)
"""
import argparse
import os
import shutil
import subprocess
import tempfile
import wave

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
SOUNDS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "yperite", "sounds")
RATE = 44100


def _smooth_noise(rng, n, cutoff_hz):
    """Random wandering of roughly the given speed, between -1 and 1."""
    width = max(3, int(RATE / cutoff_hz))
    kernel = np.hanning(width)
    kernel /= kernel.sum()
    raw = rng.standard_normal(n + width)
    out = np.convolve(raw, kernel, mode="same")[:n]
    return out / (np.max(np.abs(out)) + 1e-9)


def _bandpass_noise(rng, n, centre, width):
    """Noise in a band round `centre`: slow noise moved up to the centre frequency."""
    t = np.arange(n) / RATE
    slow = _smooth_noise(rng, n, width) + 1j * _smooth_noise(rng, n, width)
    return np.real(slow * np.exp(2j * np.pi * centre * t))


def whistle_blast(seconds, pitch, seed, trill=33.0):
    rng = np.random.default_rng(seed)
    n = int(seconds * RATE)
    t = np.arange(n) / RATE
    attack, release = 0.03, 0.08
    # the note: rising into it at the start, sagging at the end as the breath runs out
    rise = 1.0 - 0.07 * np.exp(-t / 0.025)
    fall = 1.0 - 0.05 * np.clip((t - (seconds - release)) / release, 0.0, 1.0)
    # the pea: a trill whose speed wanders with the breath
    rate = trill * (1.0 + 0.12 * _smooth_noise(rng, n, 3.0))
    phase_pea = 2 * np.pi * np.cumsum(rate) / RATE
    pea = np.sin(phase_pea) + 0.35 * np.sin(2 * phase_pea + 0.7)
    pea /= np.max(np.abs(pea))
    freq = pitch * rise * fall * (1.0 + 0.045 * pea + 0.004 * _smooth_noise(rng, n, 8.0))
    phase = 2 * np.pi * np.cumsum(freq) / RATE
    tone = np.sin(phase) + 0.09 * np.sin(2 * phase + 0.3) + 0.025 * np.sin(3 * phase + 1.1)
    loud = 1.0 - 0.38 * (0.5 + 0.5 * pea)
    breath = 0.06 * _bandpass_noise(rng, n, pitch, 600.0) + 0.012 * rng.standard_normal(n)
    envelope = np.clip(t / attack, 0.0, 1.0) * np.clip((seconds - t) / release, 0.0, 1.0)
    envelope = envelope * envelope * (3 - 2 * envelope)          # smooth both ends
    signal = (tone * loud + breath) * envelope
    return signal / np.max(np.abs(signal)) * 10 ** (-3 / 20)     # peak at -3 dB


def _write_ogg(signal, path, keep_wav=None):
    pcm = (np.clip(signal, -1.0, 1.0) * 32767).astype("<i2")
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, "sound.wav")
        with wave.open(wav, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes(pcm.tobytes())
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-ac", "1", "-c:a", "libvorbis", "-q:a", "5",
                        path], check=True)
        if keep_wav:
            os.makedirs(keep_wav, exist_ok=True)
            shutil.copy(wav, os.path.join(keep_wav, os.path.basename(path).replace(".ogg", ".wav")))


# file name -> signal
WHISTLES = {
    "officer_whistle_1": lambda: whistle_blast(1.15, 2900.0, 11),
    "officer_whistle_2": lambda: whistle_blast(1.35, 3020.0, 12, trill=36.0),
}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--wav")
    args = ap.parse_args()
    out = os.path.join(SOUNDS, "item")
    os.makedirs(out, exist_ok=True)
    for name, make in WHISTLES.items():
        _write_ogg(make(), os.path.join(out, name + ".ogg"), args.wav)
        print(f"{name}.ogg written")


if __name__ == "__main__":
    main()
