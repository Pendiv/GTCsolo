// Re-Avaritia halo declarations exposed by gtcsolo for ATG.
// Startup script only: declarations must exist before client models are built.
const GtcHalo = Java.loadClass('DIV.gtcsolo.api.halo.HaloAPI')

// Fixed ARGB halo. Six-digit RGB is also accepted and receives FF alpha.
GtcHalo.fixed('gtcsolo:generic_101_001', '#FFC64CFF', 10, true)

// Real-time HSV rainbow halo; the final argument is one full cycle in milliseconds.
GtcHalo.rainbow('gtcsolo:generic_101_004', 10, true, 5000)

// ATG/KubeJS-owned Item proof. Its model is generated from the KubeJS ItemBuilder at startup.
GtcHalo.fixed('kubejs:coordinate_data_storage_orb', '#FF5DEBFF', 8, false)
