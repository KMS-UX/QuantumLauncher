import bpy, math

bpy.ops.wm.open_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human_eyes.blend")
human = bpy.data.objects.get("Human")

hair_mat = bpy.data.materials.new("QUARK_Hair")
hair_mat.use_nodes = True
bsdf = hair_mat.node_tree.nodes["Principled BSDF"]
bsdf.inputs['Base Color'].default_value = (0.404, 0.310, 0.220, 1.0)
bsdf.inputs['Roughness'].default_value = 0.42
nt = hair_mat.node_tree
noise = nt.nodes.new('ShaderNodeTexNoise')
noise.inputs['Scale'].default_value = 60.0
noise.inputs['Detail'].default_value = 4.0
ramp = nt.nodes.new('ShaderNodeValToRGB')
ramp.color_ramp.elements[0].color = (0.30, 0.22, 0.15, 1.0)
ramp.color_ramp.elements[1].color = (0.55, 0.42, 0.30, 1.0)
nt.links.new(noise.outputs['Fac'], ramp.inputs['Fac'])
bump = nt.nodes.new('ShaderNodeBump')
bump.inputs['Strength'].default_value = 0.15
nt.links.new(noise.outputs['Fac'], bump.inputs['Height'])
nt.links.new(bump.outputs['Normal'], bsdf.inputs['Normal'])
nt.links.new(ramp.outputs['Color'], bsdf.inputs['Base Color'])


def add_ellipsoid(name, loc, radii, rot=(0, 0, 0)):
    bpy.ops.mesh.primitive_uv_sphere_add(radius=1.0, location=loc, segments=28, ring_count=18)
    obj = bpy.context.active_object
    obj.name = name
    obj.scale = radii
    obj.rotation_euler = rot
    obj.data.materials.append(hair_mat)
    obj.parent = human
    return obj


# Scalp cap: small dome sitting fully ABOVE the measured hairline (z>1.545) -- no bisect, just
# sized/placed so it never dips below it. Head dome measured x in (-0.091,0.091), z in (1.52,1.668).
add_ellipsoid("Hair_Cap", (0.0, -0.01, 1.595), (0.092, 0.105, 0.062))

# Bun: gathered mass at the back/top (back = +Y, matching this pipeline's established
# front-shows-back camera convention).
add_ellipsoid("Hair_Bun", (0.0, 0.07, 1.60), (0.050, 0.050, 0.042))

# Two slim front locks framing the face, small and tapered.
add_ellipsoid("Hair_LockL", (0.082, -0.05, 1.53), (0.016, 0.028, 0.055))
add_ellipsoid("Hair_LockR", (-0.082, -0.05, 1.53), (0.016, 0.028, 0.055))

bpy.ops.wm.save_as_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human_hair.blend")
print("HAIR_ADDED")
