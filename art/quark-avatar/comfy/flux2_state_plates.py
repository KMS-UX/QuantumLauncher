"""
Generate QUARK's expression state plates with Flux 2 Klein -- Phase 12.

`reference/QUARK_Turnaround1.png` panel 04 defines four states and their voice:

    IDLE  (STATIC AT REST)   Quiet. Ready. Observant.
    SCAN  (THINKING)         Focused. Analyzing. One moment.
    HAPPY (POSITIVE)         Warmth. Approval. Light wit allowed.
    WARN  (ALERT / DENIED)   Clipped. Grave. No wit. No softening.

These are STATES, not an animation, and that distinction is the whole reason this script exists
rather than a video pipeline. The house style is explicit that QUARK is "static at rest (zero idle
redraw)" and that motion is "reactive, not ambient" and "stepped, not interpolated" -- so what the
design needs is four plates and a stepped transition between them, which no video model is required
for. See the Phase 11 entry for the full investigation.

**IDLE is not generated.** `reference/QUARK_HOLOGRAMBUST_FRONT.png` already IS the idle plate --
neutral, level, observant. Regenerating it would only introduce drift from the reference the other
three are measured against, so it is used as-is and only the three that genuinely differ are made.

The graph is ComfyUI's own `image_flux2_klein_image_edit_4b_distilled` template flattened out of its
subgraph so it can be posted to `/prompt`: Flux 2 Klein 4B fp8 + qwen_3_4b text encoder + flux2-vae,
`ReferenceLatent` on both the positive and the zeroed negative conditioning, CFG 1, euler, 4 steps.
Everything it needs is already installed -- no download.

**The magenta chroma key is fed in and expected back out.** Editing the KEYED (transparent) plate
would put the character on black and leave no way to re-matte the result; keeping the key in frame
means each output goes straight back through `chroma_key.py` on the same exact R-G threshold. The
prompts therefore say to preserve the background explicitly, and `--check` measures whether it
survived.

Usage:
    python flux2_state_plates.py ../reference/QUARK_HOLOGRAMBUST_FRONT.png OUTDIR [--state=warn]
"""
import json
import os
import sys
import time
import urllib.request
import uuid

from comfy_client import COMFY, post, upload

UNET = "flux-2-klein-4b-fp8.safetensors"
CLIP = "qwen_3_4b.safetensors"
VAE = "flux2-vae.safetensors"

# Template values, not hand-tuned: the DISTILLED variant runs at cfg 1 in 4 steps.
STEPS = 4
CFG = 1
SAMPLER = "euler"
MEGAPIXELS = 1.0

# DENOISE is the whole ballgame, and it is the same trade-off this log measured back in the ComfyUI
# polish phase. The template starts from an EMPTY latent, i.e. it regenerates the image with the
# source only as conditioning -- and the first WARN plate proved what that costs: the expression and
# the red accents were exactly right, and the face, the hair and the hologram's own scanline
# treatment all drifted into a different piece of art. Starting from the ENCODED SOURCE and denoising
# only partway makes it a true edit. Raise it if a state will not take; lower it if identity slips.
DENOISE = 0.45

# Per-state overrides. HAPPY needs a LIGHTER touch than the others: at 0.45 the model reads "smile"
# as a broad, open, toothy one no matter how the prompt is worded, which is out of register with a
# character whose voice is "wit rationed". At 0.30 the same prompt lands as a restrained half-smile
# -- Director's pick from a side-by-side.
STATE_DENOISE = {"happy": 0.30}

# Every prompt is written as "change ONLY <x>, keep <everything else>" and names the background
# explicitly, because the key has to survive for the plate to be re-mattable.
PRESERVE = (
    "Keep her identity, face shape, hair, headband, ear modules, armour, pose, framing and "
    "lighting exactly as they are, and keep the flat magenta background completely unchanged."
)

STATES = {
    "scan": (
        "Change only her expression to focused analysis: eyes narrowed very slightly in "
        "concentration, gaze fixed directly forward, brows a fraction drawn in, mouth closed and "
        "neutral. Make the cyan circuit lines on her neck and the ring of her ear module glow "
        "brighter. " + PRESERVE
    ),
    # Rewritten after the first pass: "a gentle closed-lip smile" still produced a broad, open,
    # toothy smile, which is out of register with a character whose voice is "wit rationed" and
    # "peer-grade aide, never servile". The instruction now says what to do AND what not to.
    "happy": (
        "Change only her expression to restrained warmth: the corners of her mouth lift only very "
        "slightly, lips closed and together, eyes softened and a little warmer, brows relaxed. A "
        "subtle, elegant, dignified half-smile. Do NOT open her mouth, do NOT show teeth, do NOT "
        "make it a broad or beaming smile. The change should be barely perceptible. "
        + PRESERVE
    ),
    "warn": (
        "Change only two things. First, her expression becomes grave and clipped: mouth set in a "
        "flat line, brows level and lowered, gaze hard and direct, no warmth. Second, the glow of "
        "the circular ear module and the circuit lines on her neck changes from cyan to red. "
        + PRESERVE
    ),
}

# Fixed per state so a re-run reproduces the same plate rather than a new one.
# ── THINKING candidates (Director, 2026-08-22 Fold 6 round) ───────────────────────────────────
# SCAN *is* the THINKING state (panel 04: "SCAN (THINKING) -- Focused. Analyzing. One moment."), so
# this is a revision of that plate, not a fifth state. What is being revised: the shipped `scan`
# prompt above says "brows a fraction drawn in", and the Director has ruled the furrowed brow out --
# it reads as displeasure rather than thought, and QUARK is a peer-grade aide, not a disapproving
# one. Every candidate below therefore states the relaxed brow as an explicit NOT, because the model
# reaches for a furrow on its own whenever the word "concentration" appears.
#
# The thinking now has to live in the MOUTH instead, which is the Director's three directions. All
# three keep the eyes doing the work they already do (fixed, forward, engaged) and change only the
# mouth, so the candidates are comparable to each other and to the shipped plate.
THINKING = {
    # A -- the most conservative, and closest to what ships today minus the furrow.
    "think_a": (
        "Change only her expression to quiet concentration held in the mouth: her lips are lightly "
        "pressed together into a firm, straight, level line, relaxed and untensed -- no pursing, no "
        "downturn, no pout. Her gaze stays fixed directly forward and engaged. Her brows stay "
        "completely relaxed, level and smooth: do NOT furrow, draw in, lower or raise her brows, and "
        "do NOT put any crease or line between them. Make the cyan circuit lines on her neck and the "
        "ring of her ear module glow brighter. "
    ),
    # B -- the most characterful, and the highest-risk: "smirk" is one word away from smug.
    "think_b": (
        "Change only her expression to an unhurried, considering look: one corner of her mouth is "
        "nudged very slightly upward in a subtle, questioning half-smirk, lips closed and together, "
        "the other corner level -- barely perceptible and clearly asymmetric. Her gaze stays fixed "
        "directly forward and engaged. Her brows stay completely relaxed, level and smooth: do NOT "
        "furrow, draw in, lower or raise her brows, and do NOT raise one eyebrow. Do NOT open her "
        "mouth, do NOT show teeth, and do NOT make it a broad smile, a grin or a smug expression. "
        "Make the cyan circuit lines on her neck and the ring of her ear module glow brighter. "
    ),
    # C -- the Director's third direction. The tongue itself is not visible, so the prompt describes
    # what a camera would actually see, and names the tongue only as the cause. Flagged as the
    # riskiest of the three: the same shape reads as "chewing" if it goes even slightly too far.
    "think_c": (
        "Change only her expression to an absorbed, working-it-out look: her tongue is pressed "
        "gently against the inside of one cheek from within, so that cheek shows a soft, subtle "
        "rounded fullness and her closed mouth is pushed very slightly off to that side. Lips stay "
        "closed and relaxed. Her gaze stays fixed directly forward and engaged. Her brows stay "
        "completely relaxed, level and smooth: do NOT furrow, draw in, lower or raise her brows. Do "
        "NOT open her mouth, do NOT show the tongue or teeth, and do NOT make her look like she is "
        "chewing or grimacing. Keep it subtle. Make the cyan circuit lines on her neck and the ring "
        "of her ear module glow brighter. "
    ),
}
# C, second attempt. The first wording ("tongue pressed gently against the inside of one cheek")
# produced NO change at 0.40 or 0.52 -- identical output to the neutral plate both times. Reading:
# the model cannot act on an instruction about a structure it cannot see, and "subtle" plus the
# PRESERVE clause's "keep her face exactly as it is" cancel what little is left. This rewrite names
# no tongue at all and asks only for the visible geometry, with the restraint words removed.
THINKING["think_c2"] = (
    "Change only the shape of her closed mouth and one cheek. Her lips stay closed and together but "
    "are pushed distinctly across to her left, so the whole mouth sits noticeably off-centre and "
    "asymmetric. Her left cheek is visibly rounded and full, bulging outward, while her right cheek "
    "stays flat. Her gaze stays fixed directly forward and engaged. Her brows stay completely "
    "relaxed, level and smooth: do NOT furrow, draw in, lower or raise her brows. Do NOT open her "
    "mouth and do NOT show teeth. Make the cyan circuit lines on her neck and the ring of her ear "
    "module glow brighter. "
)
STATES.update({k: v + PRESERVE for k, v in THINKING.items()})

# Fixed per state so a re-run reproduces the same plate rather than a new one.
SEEDS = {"scan": 10_001, "happy": 10_002, "warn": 10_003,
         "think_a": 10_004, "think_b": 10_005, "think_c": 10_006, "think_c2": 10_007}

# These are subtle MOUTH edits, which is the same restraint problem HAPPY hit: at the 0.45 the other
# states use, a mouth instruction lands as a whole new face. 0.30 is the value HAPPY was tuned to for
# exactly this, so the two mouth-only candidates start there. C asks for an actual asymmetric change
# of shape rather than a micro-adjustment, so it gets a little more room to take at all.
STATE_DENOISE.update({"think_a": 0.30, "think_b": 0.30, "think_c": 0.40})


def graph(image_name, prompt, seed, out_prefix, denoise=None):
    denoise = DENOISE if denoise is None else denoise
    return {
        "1":  {"class_type": "LoadImage", "inputs": {"image": image_name}},
        "2":  {"class_type": "ImageScaleToTotalPixels",
               "inputs": {"image": ["1", 0], "upscale_method": "nearest-exact",
                          "megapixels": MEGAPIXELS, "resolution_steps": 1}},
        "3":  {"class_type": "GetImageSize", "inputs": {"image": ["2", 0]}},
        "4":  {"class_type": "UNETLoader",
               "inputs": {"unet_name": UNET, "weight_dtype": "default"}},
        "5":  {"class_type": "CLIPLoader",
               "inputs": {"clip_name": CLIP, "type": "flux2", "device": "default"}},
        "6":  {"class_type": "VAELoader", "inputs": {"vae_name": VAE}},
        "7":  {"class_type": "CLIPTextEncode", "inputs": {"clip": ["5", 0], "text": prompt}},
        "8":  {"class_type": "ConditioningZeroOut", "inputs": {"conditioning": ["7", 0]}},
        "9":  {"class_type": "VAEEncode", "inputs": {"pixels": ["2", 0], "vae": ["6", 0]}},
        # The reference latent is attached to BOTH sides -- that is what makes this an edit of the
        # source rather than a generation that merely resembles it.
        "10": {"class_type": "ReferenceLatent",
               "inputs": {"conditioning": ["7", 0], "latent": ["9", 0]}},
        "11": {"class_type": "ReferenceLatent",
               "inputs": {"conditioning": ["8", 0], "latent": ["9", 0]}},
        "12": {"class_type": "CFGGuider",
               "inputs": {"model": ["4", 0], "positive": ["10", 0], "negative": ["11", 0],
                          "cfg": CFG}},
        "13": {"class_type": "KSamplerSelect", "inputs": {"sampler_name": SAMPLER}},
        "14": {"class_type": "Flux2Scheduler",
               "inputs": {"steps": STEPS, "width": ["3", 0], "height": ["3", 1]}},
        # Keep only the tail of the sigma schedule, and start from the encoded source rather than
        # from noise -- that is what turns "generate something like this" into "edit this".
        "20": {"class_type": "SplitSigmasDenoise",
               "inputs": {"sigmas": ["14", 0], "denoise": denoise}},
        "16": {"class_type": "RandomNoise", "inputs": {"noise_seed": seed}},
        "17": {"class_type": "SamplerCustomAdvanced",
               "inputs": {"noise": ["16", 0], "guider": ["12", 0], "sampler": ["13", 0],
                          "sigmas": ["20", 1], "latent_image": ["9", 0]}},
        "18": {"class_type": "VAEDecode", "inputs": {"samples": ["17", 0], "vae": ["6", 0]}},
        "19": {"class_type": "SaveImage",
               "inputs": {"images": ["18", 0], "filename_prefix": out_prefix}},
    }


def run(image_path, states, out_dir, denoise=None):
    name = upload(image_path)
    print(f"[flux2] uploaded {name}")
    os.makedirs(out_dir, exist_ok=True)
    results = {}
    for state in states:
        tag = "" if denoise is None else f"_d{int(denoise * 100)}"
        prefix = f"quark_state_{state}{tag}"
        state_denoise = denoise if denoise is not None else STATE_DENOISE.get(state, DENOISE)
        pid = post("/prompt", {"prompt": graph(name, STATES[state], SEEDS[state], prefix,
                                               state_denoise),
                               "client_id": str(uuid.uuid4())})["prompt_id"]
        print(f"[flux2] {state}: queued {pid}")
        t0 = time.time()
        while time.time() - t0 < 900:
            h = json.loads(urllib.request.urlopen(f"{COMFY}/history/{pid}", timeout=30).read())
            if pid in h:
                status = h[pid]["status"].get("status_str")
                files = [im["filename"]
                         for out in h[pid].get("outputs", {}).values()
                         for im in out.get("images", [])]
                print(f"[flux2] {state}: {status} in {int(time.time() - t0)}s -> {files}")
                results[state] = files
                break
            time.sleep(5)
        else:
            raise TimeoutError(f"{state} did not finish within 900s")
    return results


if __name__ == "__main__":
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    wanted = [a.split("=", 1)[1] for a in sys.argv if a.startswith("--state=")] or list(STATES)
    den = next((float(a.split("=", 1)[1]) for a in sys.argv if a.startswith("--denoise=")), None)
    run(sys.argv[1], wanted, sys.argv[2], den)
