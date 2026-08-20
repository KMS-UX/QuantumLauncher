import bpy
bpy.ops.wm.open_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human2.blend")
human = bpy.data.objects.get("Human")
# sample vertices in the head region to get scalp dome shape
pts = []
for v in human.data.vertices:
    w = human.matrix_world @ v.co
    if w.z > 1.52:
        pts.append(w)
xs = [p.x for p in pts]; ys = [p.y for p in pts]; zs = [p.z for p in pts]
print(f"HEAD_DOME n={len(pts)} x=({min(xs):.4f},{max(xs):.4f}) y=({min(ys):.4f},{max(ys):.4f}) z=({min(zs):.4f},{max(zs):.4f})")
# center estimate
cx = (min(xs)+max(xs))/2; cy=(min(ys)+max(ys))/2
print(f"CENTER_XY=({cx:.4f},{cy:.4f})")
