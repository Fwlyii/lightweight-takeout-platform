import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { ref } from 'vue';
import { parse, compileTemplate } from '@vue/compiler-sfc';
import { validCoordinates } from '../src/utils/businessDistance.js';
const source=readFileSync(new URL('../src/components/MapLocationPicker.vue',import.meta.url),'utf8');
function fixture(overrides={}) {
  let dispose;
  const bindings={ref,validCoordinates,onMounted(){},onBeforeUnmount(fn){dispose=fn;},defineProps:()=>({initial:null}),defineEmits(){},
    loadMapSdk:async()=>{throw Error('offline');},mapSearch:async()=>({pois:[]}),mapReverse:async()=>({regeocode:{formatted_address:'真实地点',addressComponent:{adcode:'120112'}}}),
    locateDevice:async()=>{throw Error('定位被拒绝');},poiLocation:x=>x,...overrides};
  const script=parse(source).descriptor.scriptSetup.content.replace(/^import .*;\r?\n/gm,'');
  const view=new Function(...Object.keys(bindings),script+'\nreturn {keyword,results,error,selected,resolving,locating,search,choose,resolvePoint,locate,init,mapError};')(...Object.values(bindings));
  return {view,dispose:()=>dispose()};
}
test('map picker template compiles with explicit confirmation',()=>{
  const descriptor=parse(source).descriptor;
  assert.deepEqual(compileTemplate({source:descriptor.template.content,filename:'MapLocationPicker',id:'map-test'}).errors,[]);
  assert.match(source,/:disabled="!selected \|\| resolving \|\| locating"/);
});
test('search handles empty and unavailable services without fake points',async()=>{
  const {view}=fixture();view.keyword.value='天津';await view.search();assert.match(view.error.value,/没有找到/);assert.equal(view.selected.value,null);
  const failed=fixture({mapSearch:async()=>{throw Error('offline');}}).view;failed.keyword.value='天津';await failed.search();assert.match(failed.error.value,/搜索失败/);
});
test('late reverse-geocoding cannot overwrite a newer POI choice',async()=>{
  let finish;const {view}=fixture({mapReverse:()=>new Promise(resolve=>finish=resolve)});
  const pending=view.resolvePoint({longitude:117,latitude:39});assert.equal(view.resolving.value,true);
  view.choose({longitude:118,latitude:40,formattedAddress:'用户最后选择'});
  finish({regeocode:{formatted_address:'过期结果'}});await pending;
  assert.equal(view.selected.value.formattedAddress,'用户最后选择');assert.equal(view.resolving.value,false);
});
test('failed reverse lookup and denied geolocation do not confirm an invented position',async()=>{
  const {view}=fixture({mapReverse:async()=>{throw Error('服务不可用');}});
  await view.resolvePoint({longitude:117,latitude:39});assert.equal(view.selected.value,null);assert.match(view.error.value,/服务不可用/);
  await view.locate();assert.equal(view.locating.value,false);assert.match(view.error.value,/定位被拒绝/);
});
test('unmounted picker ignores pending location responses',async()=>{
  let finish;const {view,dispose}=fixture({mapReverse:()=>new Promise(resolve=>finish=resolve)});
  const pending=view.resolvePoint({longitude:117,latitude:39});dispose();finish({regeocode:{formatted_address:'迟到结果'}});await pending;assert.equal(view.selected.value,null);
});
