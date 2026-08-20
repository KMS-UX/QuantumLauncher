import bpy, math

bpy.ops.wm.open_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human_materials.blend")
human = bpy.data.objects.get("Human")

# measured eye socket centers (world coords, this tuned mesh)
EYE_L = (0.035, -0.10, 1.492)
EYE_R = (-0.035, -0.10, 1.492)
EYE_RADIUS = 0.012

def make_eye(name, loc, iris_color=(0.30, 0.45, 0.55)):
    bpy.ops.mesh.primitive_uv_sphere_add(radius=EYE_RADIUS, location=loc, segments=24, ring_count=16)
    eye = bpy.context.active_object
    eye.name = name

    sclera = bpy.data.materials.new(f"{name}_Sclera")
    sclera.use_nodes = True
    bsdf = sclera.node_tree.nodes["Principled BSDF"]
    bsdf.inputs['Base Color'].default_value = (0.92, 0.90, 0.87, 1.0)
    bsdf.inputs['Roughness'].default_value = 0.15

    iris = bpy.data.materials.new(f"{name}_Iris")
    iris.use_nodes = True
    bsdf2 = iris.node_tree.nodes["Principled BSDF"]
    bsdf2.inputs['Base Color'].default_value = (*iris_color, 1.0)
    bsdf2.inputs['Roughness'].default_value = 0.05

    pupil = bpy.data.materials.new(f"{name}_Pupil")
    pupil.use_nodes = True
    bsdf3 = pupil.node_tree.nodes["Principled BSDF"]
    bsdf3.inputs['Base Color'].default_value = (0.01, 0.01, 0.01, 1.0)
    bsdf3.inputs['Roughness'].default_value = 0.05

    eye.data.materials.append(sclera)
    eye.data.materials.append(iris)
    eye.data.materials.append(pupil)

    # front-facing direction is -Y (confirmed via the face-closeup render); classify polygons by
    # angle from sphere center toward -Y to paint iris (small cone) and pupil (smaller cone).
    center = eye.location
    for poly in eye.data.polygons:
        world_center = eye.matrix_world @ poly.center
        d = (world_center - center).normalized()
        # -Y is "forward" for this mesh's front-facing convention
        forward_dot = -d.y
        if forward_dot > 0.97:
            poly.material_index = 2  # pupil
        elif forward_dot > 0.85:
            poly.material_index = 1  # iris
        else:
            poly.material_index = 0  # sclera

    eye.select_set(False)
    return eye

eye_l = make_eye("Eye_L", EYE_L)
eye_r = make_eye("Eye_R", EYE_R)

for eye in (eye_l, eye_r):
    eye.parent = human

bpy.ops.wm.save_as_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human_eyes.blend")
print("EYES_ADDED")
