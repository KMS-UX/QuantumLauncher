import bpy
bpy.ops.wm.open_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human2.blend")
human = bpy.data.objects.get("Human")
mesh = human.data

# find boundary (non-manifold, used by exactly 1 face) edges -- eye sockets are open holes
boundary_verts = set()
for e in mesh.edges:
    if len(e.link_faces) <= 1:
        boundary_verts.update(e.vertices)

# among boundary verts, cluster ones near the expected face region (positive z upper area, small |x|)
pts = []
for vi in boundary_verts:
    v = mesh.vertices[vi]
    w = human.matrix_world @ v.co
    if w.z > 1.4 and abs(w.x) < 0.15 and w.y < -0.05:
        pts.append((w.x, w.y, w.z))

print("BOUNDARY_PTS_NEAR_FACE:", len(pts))
# cluster into left/right by x sign
left = [p for p in pts if p[0] > 0.01]
right = [p for p in pts if p[0] < -0.01]
def centroid(pts):
    n = len(pts)
    if n == 0: return None
    return (sum(p[0] for p in pts)/n, sum(p[1] for p in pts)/n, sum(p[2] for p in pts)/n)
print("LEFT_SOCKET_CENTROID:", centroid(left), "n=", len(left))
print("RIGHT_SOCKET_CENTROID:", centroid(right), "n=", len(right))
