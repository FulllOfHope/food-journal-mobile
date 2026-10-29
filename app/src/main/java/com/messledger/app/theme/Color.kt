package com.messledger.app.theme

import com.messledger.app.ui.theme.CardLight as UiCardLight
import com.messledger.app.ui.theme.CardDark as UiCardDark
import com.messledger.app.ui.theme.Ember as UiEmber
import com.messledger.app.ui.theme.EtchedRuleStrong as UiEtchedRuleStrong
import com.messledger.app.ui.theme.Ink as UiInk
import com.messledger.app.ui.theme.NightSurfaceVariant as UiNightSurfaceVariant
import com.messledger.app.ui.theme.Paper as UiPaper
import com.messledger.app.ui.theme.Pencil as UiPencil
import com.messledger.app.ui.theme.Rule as UiRule
import com.messledger.app.ui.theme.StampRed as UiStampRed
import com.messledger.app.ui.theme.TextLight as UiTextLight
import com.messledger.app.ui.theme.VoidColor as UiVoidColor

// Compatibility names for the older `theme` package (widget, shared modifiers).
// The palette itself lives in `ui.theme.Color`; these aliases only exist so
// there is one set of values instead of two palettes drifting apart.

// Sunlit Scrapbook
val StampRed = UiStampRed
val PaperCream = UiPaper
val InkBlack = UiInk
val PencilFaint = UiPencil
val LedgerRule = UiRule
val EatenCardBg = UiCardLight
val CardBgLight = UiCardLight

// The Night Ledger
val VoidCharcoal = UiVoidColor
val NeonEmber = UiEmber
val EtchedGlass = UiEtchedRuleStrong
val StarlightWhite = UiTextLight
val DeepSpaceCard = UiCardDark
val NebulaGlow = UiNightSurfaceVariant
