import { createApp, h } from 'vue';
import MapLocationPicker from '../components/MapLocationPicker.vue';

export function pickMapLocation(initial = null) {
  return new Promise(resolve => {
    const opener = document.activeElement;
    const host = document.createElement('div');
    host.style.cssText = 'position:fixed;inset:0;z-index:12000;background:#16364b88;display:grid;place-items:center;padding:16px;';
    document.body.appendChild(host);
    let app;
    const close = value => { app.unmount(); host.remove(); opener?.focus?.(); resolve(value); };
    app = createApp({ render: () => h('section', { role:'dialog', 'aria-modal':'true', 'aria-label':'选择地图位置', style:'width:min(100%,560px);max-height:90vh;overflow:auto;background:white;padding:20px;border-radius:16px;', onKeydown: event => {
      if(event.key === 'Escape') close(null);
      if(event.key === 'Tab') { const focusable=[...host.querySelectorAll('button:not(:disabled),input,a[href]')]; const first=focusable[0],last=focusable.at(-1); if(event.shiftKey && document.activeElement===first){event.preventDefault();last?.focus();}else if(!event.shiftKey && document.activeElement===last){event.preventDefault();first?.focus();} }
    } }, [h('button', { type:'button', onClick:()=>close(null), style:'float:right;border:0;background:none;padding:8px;color:#526f85;' }, '取消'), h('h3', '选择地图位置'), h(MapLocationPicker, { initial, onSelect: close })]) });
    app.mount(host); host.querySelector('input')?.focus();
  });
}
