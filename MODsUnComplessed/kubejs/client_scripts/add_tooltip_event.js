// priority: 1000

const TOOLTIP_REGISTRY = {}

function tooltipKeyBase(itemId) {
  return `tooltip.${String(itemId).replace(/:/g, '.')}`
}

function ensureLang(lang) {
  if (!TOOLTIP_REGISTRY[lang]) TOOLTIP_REGISTRY[lang] = {}
  return TOOLTIP_REGISTRY[lang]
}

function normalizeTooltipData(data) {
  const target = {
    mode: 'only', // デフォルト: 元ツールチップを消して自前のみ表示
    normal: [],
    shift: []
  }
  if (Array.isArray(data) && (data.length === 0 || typeof data[0] === 'string')) {
    target.normal = data.slice()
    return target
  }
  if (!data || typeof data !== 'object') {
    return target
  }
  if (typeof data.mode === 'string') {
    const mode = data.mode.toLowerCase()
    if (mode === 'add' || mode === 'only') {
      target.mode = mode
    }
  }
  if (Array.isArray(data.normal)) {
    target.normal = data.normal.slice()
  }
  if (Array.isArray(data.shift)) {
    target.shift = data.shift.slice()
  }
  return target
}
function registerTooltipEntry(langMap, itemId, data) {
  if (typeof itemId !== 'string' || itemId.trim() === '') return
  langMap[itemId] = normalizeTooltipData(data)
}
function AddTooltipEvent(lang, entries) {
  const langMap = ensureLang(lang)
  if (!Array.isArray(entries)) {
    Object.entries(entries).forEach(([itemId, data]) => {
      registerTooltipEntry(langMap, itemId, data)
    })
    return
  }
  entries.forEach(entry => {
    if (!entry || typeof entry !== 'object') return

    const items = Array.isArray(entry.items) ? entry.items : []
    const data = {
      mode: entry.mode,
      normal: entry.normal,
      shift: entry.shift
    }

    items.forEach(itemId => {
      registerTooltipEntry(langMap, itemId, data)
    })
  })
}

function getBaseLangMap() {
  return (
    global.__TOOLTIP_REGISTRY['ja_jp'] ||
    global.__TOOLTIP_REGISTRY['en_us'] ||
    {}
  )
}

function buildTranslatedLines(keyBase, prefix, count) {
  const lines = []
  for (let i = 0; i < count; i++) {
    lines.push(Text.translate(`${keyBase}.${prefix}${i + 1}`))
  }
  return lines
}

global.AddTooltipEvent = AddTooltipEvent
global.__TOOLTIP_REGISTRY = TOOLTIP_REGISTRY

ClientEvents.lang('ja_jp', event => {
  const langMap = global.__TOOLTIP_REGISTRY['ja_jp'] || {}

  Object.entries(langMap).forEach(([itemId, data]) => {
    const keyBase = tooltipKeyBase(itemId)

    for (let i = 0; i < data.normal.length; i++) {
      event.add(`${keyBase}.${i + 1}`, data.normal[i])
    }

    for (let i = 0; i < data.shift.length; i++) {
      event.add(`${keyBase}.s${i + 1}`, data.shift[i])
    }
  })
})

ClientEvents.lang('en_us', event => {
  const langMap = global.__TOOLTIP_REGISTRY['en_us'] || {}
  Object.entries(langMap).forEach(([itemId, data]) => {
    const keyBase = tooltipKeyBase(itemId)
    for (let i = 0; i < data.normal.length; i++) {
      event.add(`${keyBase}.${i + 1}`, data.normal[i])
    }
    for (let i = 0; i < data.shift.length; i++) {
      event.add(`${keyBase}.s${i + 1}`, data.shift[i])
    }
  })
})
ItemEvents.tooltip(event => {
  const baseLangMap = getBaseLangMap()
  Object.entries(baseLangMap).forEach(([itemId, data]) => {
    const keyBase = tooltipKeyBase(itemId)
    const normalTexts = buildTranslatedLines(keyBase, '', data.normal.length)
    const shiftTexts = buildTranslatedLines(keyBase, 's', data.shift.length)
    event.addAdvanced(itemId, (item, advanced, text) => {
      const hasShiftLines = shiftTexts.length > 0
      const showShift = hasShiftLines && event.isShift()
      const linesToShow = showShift ? shiftTexts : normalTexts
      if (data.mode === 'only') {
        const firstLine = text.isEmpty() ? null : text.get(0)
        text.clear()
        if (firstLine != null) {
          text.add(firstLine)
        }
      }
      for (let i = 0; i < linesToShow.length; i++) {
        text.add(linesToShow[i])
      }
    })
  })
})