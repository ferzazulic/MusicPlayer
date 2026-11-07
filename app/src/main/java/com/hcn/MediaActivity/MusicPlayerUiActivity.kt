package com.hcn.MediaActivity

import com.ferzazulic.carmusic.MainActivity

/**
 * CarMusic's launcher screen. It carries the same class name as the HCN stock player screen because the
 * HCN firmware only forwards music info (track time) to the car's CID for a screen with this name
 * (HcnMediaCLASS in android.sourceservice.SourceService). Behaviour is identical to [MainActivity].
 */
class MusicPlayerUiActivity : MainActivity()
