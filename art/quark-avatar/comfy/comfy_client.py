"""
Minimal ComfyUI HTTP client shared by the QUARK art pipelines.

Extracted from `triposplat_reference_to_splat.py` when the reconstruction pipelines were retired
(PRODUCTION_LOG Phase 14): the DA3 relief and Flux 2 state-plate scripts were importing their
transport from a script that is no longer part of the live route, which would have made
the retired code load-bearing. That script is gone now; this transport is the survivor.

ComfyUI runs locally on 127.0.0.1:8188 with its HTTP API open.
"""
import json
import os
import urllib.request
import uuid

COMFY = "http://127.0.0.1:8188"


def post(path, payload):
    req = urllib.request.Request(
        COMFY + path, data=json.dumps(payload).encode(),
        headers={"Content-Type": "application/json"},
    )
    return json.loads(urllib.request.urlopen(req, timeout=60).read())


def upload(image_path):
    """POST an image to ComfyUI's input folder and return the name it stored it under."""
    name = os.path.basename(image_path)
    boundary = uuid.uuid4().hex
    body = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"overwrite\"\r\n\r\n"
            f"true\r\n").encode()
    body += (f"--{boundary}\r\nContent-Disposition: form-data; name=\"image\"; "
             f"filename=\"{name}\"\r\nContent-Type: image/png\r\n\r\n").encode()
    body += open(image_path, "rb").read() + f"\r\n--{boundary}--\r\n".encode()
    req = urllib.request.Request(
        COMFY + "/upload/image", data=body,
        headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
    )
    return json.loads(urllib.request.urlopen(req, timeout=120).read())["name"]
