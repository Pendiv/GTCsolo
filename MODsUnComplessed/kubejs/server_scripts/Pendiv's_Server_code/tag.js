ServerEvents.tags('item', event => {
    // 1. 既存のタグにアイテムを追加する
    // 例：独自素材のエクスポロニウム製レンチをレンチタグに追加
    event.add('gtceu:circuits','kubejs:singularity_processor')
      event.add('gtceu:circuits', 'kubejs:singularity_processor_assembly')
     event.add('gtceu:circuits', 'kubejs:singularity_processor_computer')
   event.add('gtceu:circuits', 'kubejs:singularity_processor_mainframe')
 event.add('gtceu:circuits/zpm','kubejs:singularity_processor')
event.add('gtceu:circuits/uv', 'kubejs:singularity_processor_assembly')
event.add('gtceu:circuits/uhv', 'kubejs:singularity_processor_computer')
event.add('gtceu:circuits/uev', 'kubejs:singularity_processor_mainframe')
   
})