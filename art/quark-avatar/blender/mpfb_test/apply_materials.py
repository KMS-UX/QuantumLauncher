import bpy, math

bpy.ops.wm.open_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human2.blend")
human = bpy.data.objects.get("Human")

# ---------- measured landmarks (world Z meters, this tuned mesh, A-pose) ----------
Z_GROUND = 0.0
Z_ANKLE = 0.074
Z_KNEE = 0.449
Z_HIP = 0.891
Z_SPINE4 = 0.937   # low waist
Z_SPINE1 = 1.251   # upper chest
Z_CLAVICLE = 1.338
Z_SHOULDER = 1.343
Z_SCAPULA = 1.371
Z_NECK = 1.408
Z_MOUTH = 1.507
Z_HEAD_TOP = 1.668

# ---------- make_material() copied verbatim from 01_base_mesh_and_rig.py (mesh-agnostic) ----------
def make_material(
    name, base_color, metallic=0.0, roughness=0.5, emission=None, emission_strength=0.0,
    wear=True, wear_strength=0.35, noise_scale=22.0, noise_strength=0.12, bump_strength=0.04,
    panel_detail=False, panel_scale=9.0, rivet_scale=26.0, brushed=False,
):
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    nt = mat.node_tree
    nodes, links = nt.nodes, nt.links
    nodes.clear()

    output = nodes.new('ShaderNodeOutputMaterial')
    output.location = (600, 0)
    bsdf = nodes.new('ShaderNodeBsdfPrincipled')
    bsdf.location = (300, 0)
    links.new(bsdf.outputs['BSDF'], output.inputs['Surface'])
    bsdf.inputs['Metallic'].default_value = metallic
    bsdf.inputs['Roughness'].default_value = roughness

    base_rgba = (*base_color, 1.0)
    if emission is not None and "Emission Color" in bsdf.inputs:
        bsdf.inputs["Emission Color"].default_value = (*emission, 1.0)
        bsdf.inputs["Emission Strength"].default_value = emission_strength

    if not wear:
        bsdf.inputs['Base Color'].default_value = base_rgba
        return mat

    color_node = nodes.new('ShaderNodeRGB')
    color_node.location = (-600, 200)
    color_node.outputs[0].default_value = base_rgba

    worn_color = tuple(min(1.0, c * 1.35 + 0.05) for c in base_color) + (1.0,)
    wear_color_node = nodes.new('ShaderNodeRGB')
    wear_color_node.location = (-600, 0)
    wear_color_node.outputs[0].default_value = worn_color

    geo = nodes.new('ShaderNodeNewGeometry')
    geo.location = (-900, -200)
    pointiness_ramp = nodes.new('ShaderNodeValToRGB')
    pointiness_ramp.location = (-600, -200)
    pointiness_ramp.color_ramp.elements[0].position = 0.45
    pointiness_ramp.color_ramp.elements[1].position = 0.62
    links.new(geo.outputs['Pointiness'], pointiness_ramp.inputs['Fac'])

    mix_wear = nodes.new('ShaderNodeMixRGB')
    mix_wear.location = (-300, 150)
    mix_wear.inputs['Fac'].default_value = wear_strength
    links.new(pointiness_ramp.outputs['Color'], mix_wear.inputs['Fac'])
    links.new(color_node.outputs[0], mix_wear.inputs['Color1'])
    links.new(wear_color_node.outputs[0], mix_wear.inputs['Color2'])

    noise = nodes.new('ShaderNodeTexNoise')
    noise.location = (-900, -450)
    noise.inputs['Scale'].default_value = noise_scale
    noise.inputs['Detail'].default_value = 3.0
    mottle_ramp = nodes.new('ShaderNodeValToRGB')
    mottle_ramp.location = (-600, -450)
    mottle_ramp.color_ramp.elements[0].color = (1.0 - noise_strength,) * 3 + (1.0,)
    mottle_ramp.color_ramp.elements[1].color = (1.0, 1.0, 1.0, 1.0)
    links.new(noise.outputs['Fac'], mottle_ramp.inputs['Fac'])

    color_so_far = mix_wear.outputs['Color']
    last_bump_input = noise.outputs['Fac']

    coord = None
    if panel_detail or brushed:
        coord = nodes.new('ShaderNodeTexCoord')
        coord.location = (-1200, -750)

    if panel_detail:
        panel_voronoi = nodes.new('ShaderNodeTexVoronoi')
        panel_voronoi.location = (-900, -700)
        panel_voronoi.feature = 'DISTANCE_TO_EDGE'
        panel_voronoi.distance = 'CHEBYCHEV'
        panel_voronoi.inputs['Scale'].default_value = panel_scale
        links.new(coord.outputs['Object'], panel_voronoi.inputs['Vector'])

        seam_ramp = nodes.new('ShaderNodeValToRGB')
        seam_ramp.location = (-600, -700)
        seam_ramp.color_ramp.elements[0].color = (0.0, 0.0, 0.0, 1.0)
        seam_ramp.color_ramp.elements[0].position = 0.02
        seam_ramp.color_ramp.elements[1].color = (1.0, 1.0, 1.0, 1.0)
        seam_ramp.color_ramp.elements[1].position = 0.06
        links.new(panel_voronoi.outputs['Distance'], seam_ramp.inputs['Fac'])

        mix_seam = nodes.new('ShaderNodeMixRGB')
        mix_seam.location = (-300, 400)
        mix_seam.blend_type = 'MULTIPLY'
        mix_seam.inputs['Fac'].default_value = 0.85
        links.new(color_so_far, mix_seam.inputs['Color1'])
        links.new(seam_ramp.outputs['Color'], mix_seam.inputs['Color2'])
        color_so_far = mix_seam.outputs['Color']

        rivet_voronoi = nodes.new('ShaderNodeTexVoronoi')
        rivet_voronoi.location = (-900, -950)
        rivet_voronoi.feature = 'F1'
        rivet_voronoi.inputs['Scale'].default_value = rivet_scale
        links.new(coord.outputs['Object'], rivet_voronoi.inputs['Vector'])

        rivet_ramp = nodes.new('ShaderNodeValToRGB')
        rivet_ramp.location = (-600, -950)
        rivet_ramp.color_ramp.elements[0].color = (1.0, 1.0, 1.0, 1.0)
        rivet_ramp.color_ramp.elements[0].position = 0.05
        rivet_ramp.color_ramp.elements[1].color = (0.0, 0.0, 0.0, 1.0)
        rivet_ramp.color_ramp.elements[1].position = 0.09
        links.new(rivet_voronoi.outputs['Distance'], rivet_ramp.inputs['Fac'])

        rivet_color_node = nodes.new('ShaderNodeRGB')
        rivet_color_node.location = (-900, -1150)
        rivet_color_node.outputs[0].default_value = (0.85, 0.87, 0.9, 1.0)

        near_seam_ramp = nodes.new('ShaderNodeValToRGB')
        near_seam_ramp.location = (-600, -1150)
        near_seam_ramp.color_ramp.elements[0].color = (1.0, 1.0, 1.0, 1.0)
        near_seam_ramp.color_ramp.elements[0].position = 0.14
        near_seam_ramp.color_ramp.elements[1].color = (0.0, 0.0, 0.0, 1.0)
        near_seam_ramp.color_ramp.elements[1].position = 0.30
        links.new(panel_voronoi.outputs['Distance'], near_seam_ramp.inputs['Fac'])

        rivet_gated = nodes.new('ShaderNodeMixRGB')
        rivet_gated.location = (-300, -1150)
        rivet_gated.blend_type = 'MULTIPLY'
        rivet_gated.inputs['Fac'].default_value = 1.0
        links.new(rivet_ramp.outputs['Color'], rivet_gated.inputs['Color1'])
        links.new(near_seam_ramp.outputs['Color'], rivet_gated.inputs['Color2'])

        mix_rivet_color = nodes.new('ShaderNodeMixRGB')
        mix_rivet_color.location = (-300, 650)
        links.new(rivet_gated.outputs['Color'], mix_rivet_color.inputs['Fac'])
        links.new(color_so_far, mix_rivet_color.inputs['Color1'])
        links.new(rivet_color_node.outputs[0], mix_rivet_color.inputs['Color2'])
        color_so_far = mix_rivet_color.outputs['Color']

        seam_bump = nodes.new('ShaderNodeBump')
        seam_bump.location = (0, -700)
        seam_bump.invert = True
        seam_bump.inputs['Strength'].default_value = 0.12
        links.new(seam_ramp.outputs['Color'], seam_bump.inputs['Height'])
        last_bump_input = ('seam', seam_bump, rivet_gated)

    mix_mottle = nodes.new('ShaderNodeMixRGB')
    mix_mottle.location = (0, 100)
    mix_mottle.blend_type = 'MULTIPLY'
    mix_mottle.inputs['Fac'].default_value = 1.0
    links.new(color_so_far, mix_mottle.inputs['Color1'])
    links.new(mottle_ramp.outputs['Color'], mix_mottle.inputs['Color2'])
    links.new(mix_mottle.outputs['Color'], bsdf.inputs['Base Color'])

    rough_ramp = nodes.new('ShaderNodeValToRGB')
    rough_ramp.location = (-300, -300)
    rough_ramp.color_ramp.elements[0].color = (roughness,) * 3 + (1.0,)
    rough_ramp.color_ramp.elements[1].color = (max(0.05, roughness - 0.3),) * 3 + (1.0,)
    links.new(geo.outputs['Pointiness'], rough_ramp.inputs['Fac'])
    roughness_out = rough_ramp.outputs['Color']

    if brushed:
        brush_mapping = nodes.new('ShaderNodeMapping')
        brush_mapping.location = (-900, -50)
        brush_mapping.inputs['Rotation'].default_value = (0, math.radians(90), 0)
        links.new(coord.outputs['Object'], brush_mapping.inputs['Vector'])

        brush_wave = nodes.new('ShaderNodeTexWave')
        brush_wave.location = (-600, -50)
        brush_wave.wave_type = 'BANDS'
        brush_wave.bands_direction = 'Z'
        brush_wave.inputs['Scale'].default_value = 140.0
        brush_wave.inputs['Distortion'].default_value = 0.0
        links.new(brush_mapping.outputs['Vector'], brush_wave.inputs['Vector'])

        brush_ramp = nodes.new('ShaderNodeValToRGB')
        brush_ramp.location = (-300, -50)
        brush_ramp.color_ramp.elements[0].color = (0.85, 0.85, 0.85, 1.0)
        brush_ramp.color_ramp.elements[1].color = (1.08, 1.08, 1.08, 1.0)
        links.new(brush_wave.outputs['Color'], brush_ramp.inputs['Fac'])

        mix_brush = nodes.new('ShaderNodeMixRGB')
        mix_brush.location = (0, -50)
        mix_brush.blend_type = 'MULTIPLY'
        mix_brush.inputs['Fac'].default_value = 1.0
        links.new(roughness_out, mix_brush.inputs['Color1'])
        links.new(brush_ramp.outputs['Color'], mix_brush.inputs['Color2'])
        roughness_out = mix_brush.outputs['Color']

    links.new(roughness_out, bsdf.inputs['Roughness'])

    bump = nodes.new('ShaderNodeBump')
    bump.location = (0, -450)
    bump.inputs['Strength'].default_value = bump_strength
    links.new(noise.outputs['Fac'], bump.inputs['Height'])

    if panel_detail:
        _, seam_bump_node, rivet_ramp_node = last_bump_input
        links.new(bump.outputs['Normal'], seam_bump_node.inputs['Normal'])
        rivet_bump = nodes.new('ShaderNodeBump')
        rivet_bump.location = (150, -700)
        rivet_bump.inputs['Strength'].default_value = 0.25
        links.new(rivet_ramp_node.outputs['Color'], rivet_bump.inputs['Height'])
        links.new(seam_bump_node.outputs['Normal'], rivet_bump.inputs['Normal'])
        links.new(rivet_bump.outputs['Normal'], bsdf.inputs['Normal'])
    else:
        links.new(bump.outputs['Normal'], bsdf.inputs['Normal'])

    return mat


# ---------- new classifier, calibrated to THIS mesh's measured landmarks ----------
def classify(co, idx):
    x, y, z = co.x, co.y, co.z
    ax = abs(x)
    r_xy = math.hypot(x, y)

    # Face/head: real exposed skin above the neck (this is the whole point of the MPFB swap).
    if z > Z_NECK:
        # thin headband LED ring at brow height
        if (Z_MOUTH + 0.06) < z < (Z_MOUTH + 0.075) and r_xy > 0.06:
            return idx['emissive']
        return idx['skin']

    # Neck collar seam.
    if (Z_NECK - 0.03) < z <= Z_NECK:
        return idx['graphite']

    # Arm territory: this mesh is in a relaxed A-pose (arms angled down), not a T-pose, so both x
    # and z vary together along the arm -- ax alone (torso half-width is well under 0.18m here) is
    # still a reasonable "out on the arm" gate. Shoulder pauldron first (a cap over the joint),
    # then elbow/wrist graphite bands, with the hand ARMORED (not bare skin) -- the reference's
    # "Hand (Palm)" close-up shows segmented plate armor over the hand, unlike the old blockout.
    if ax > 0.14 and Z_HIP < z < (Z_SCAPULA + 0.02):
        if Z_SHOULDER - 0.04 < z <= Z_SCAPULA + 0.02 and ax < 0.23:
            return idx['graphite']  # shoulder pauldron
        if z < 0.85:  # roughly forearm/hand height in this A-pose
            if z < 0.90:
                return idx['graphite']  # wrist band
            return idx['ceramic']
        return idx['ceramic']

    # Leg territory.
    if z < (Z_HIP + 0.02):
        if (Z_KNEE - 0.035) < z < (Z_KNEE + 0.035):
            return idx['graphite']  # kneecap
        if Z_ANKLE - 0.01 < z < Z_ANKLE + 0.04:
            return idx['graphite']  # ankle
        if z < Z_ANKLE - 0.01:
            return idx['graphite']  # foot
        return idx['ceramic']

    # Hip wrap.
    if Z_HIP - 0.02 < z < Z_HIP + 0.08 and r_xy > 0.075:
        if Z_HIP + 0.01 < z < Z_HIP + 0.05:
            return idx['metal']
        return idx['graphite']

    # Spine conduit (back).
    if y < -0.06 and ax < 0.05 and Z_SPINE4 < z < Z_NECK - 0.03:
        return idx['emissive']

    # Scapula plates.
    if y < -0.05 and 0.05 < ax < 0.13 and Z_SPINE1 < z < Z_SCAPULA + 0.02:
        return idx['graphite']

    # Chest seam.
    if Z_SPINE1 - 0.03 < z < Z_SPINE1 and y > 0.02:
        return idx['graphite']

    return idx['ceramic']


def assign_materials(obj, glow_color=(0.902, 0.945, 1.0), glow_strength=4.0):
    ceramic = make_material(
        "QUARK_Ceramic", (0.902, 0.882, 0.816), roughness=0.45,
        wear_strength=0.3, noise_scale=16.0, noise_strength=0.08,
        panel_detail=True, panel_scale=3.2, rivet_scale=22.0,
    )
    synth_skin = make_material(
        "QUARK_SynthSkin", (0.867, 0.757, 0.686), roughness=0.5,
        wear_strength=0.05, noise_scale=40.0, noise_strength=0.03, bump_strength=0.01,
    )  # a real skin tone this time -- this material now covers the actual exposed face
    graphite = make_material(
        "QUARK_Graphite", (0.169, 0.176, 0.192), roughness=0.7,
        wear_strength=0.4, noise_scale=25.0, noise_strength=0.15,
    )
    metal_alloy = make_material(
        "QUARK_MetalAlloy", (0.784, 0.8, 0.82), metallic=1.0, roughness=0.3,
        wear_strength=0.55, noise_scale=12.0, noise_strength=0.1, bump_strength=0.02,
        panel_detail=True, panel_scale=6.0, rivet_scale=30.0, brushed=True,
    )
    emissive = make_material(
        "QUARK_Emissive", glow_color, emission=glow_color, emission_strength=glow_strength, wear=False,
    )

    idx = {}
    for key, mat in (
        ('ceramic', ceramic), ('skin', synth_skin), ('graphite', graphite),
        ('metal', metal_alloy), ('emissive', emissive),
    ):
        idx[key] = len(obj.data.materials)
        obj.data.materials.append(mat)

    for poly in obj.data.polygons:
        poly.material_index = classify(poly.center, idx)

    return emissive


assign_materials(human, glow_color=(0.902, 0.945, 1.0), glow_strength=0.3)
bpy.ops.wm.save_as_mainfile(filepath="C:/GitHub/QuantumLauncher/art/quark-avatar/blender/mpfb_test/test_human_materials.blend")
print("MATERIALS_ASSIGNED_AND_SAVED")
