'use strict';
const storageKey='softglass-setup-preview-v1';
const defaults={step:0,lang:'en',theme:'dark',reduced:false,apps:['Stremio','YouTube','Netflix','Spotify','Prime Video','Disney+'],city:'',connections:{}};
let saved; try { saved=JSON.parse(localStorage.getItem(storageKey)||'null'); } catch { saved=null; }
const state={...defaults,...saved,apps:Array.isArray(saved?.apps)?saved.apps.slice():defaults.apps.slice(),connections:{...(saved?.connections||{})}};
state.step=Math.max(0,Math.min(5,Number(state.step)||0));
const $=selector=>document.querySelector(selector);
const en=['Welcome','Appearance','Your apps','Weather','Connections','Ready'];
const es=['Bienvenida','Apariencia','Tus apps','Clima','Conexiones','Listo'];
const text=(english,spanish)=>state.lang==='es'?spanish:english;
const apps=[['Stremio','#7362d9'],['YouTube','#b42427'],['Netflix','#a3232b'],['Spotify','#217b46'],['Prime Video','#326bab'],['Disney+','#2e387d'],['Plex','#80652c'],['VLC','#8d5e36'],['Kodi','#31667b']];
const cities=[['Buenos Aires','Argentina'],['Madrid','España'],['London','United Kingdom'],['New York','United States'],['Berlin','Deutschland'],['São Paulo','Brasil']];
const icons={stremio:'<path d="m6 3 15 9-15 9z"/>',spotify:'<circle cx="12" cy="12" r="9"/><path d="M7 9c4-2 7-1 11 1M8 12c3-1 6-1 9 1M9 15c2-1 4 0 6 1"/>',bulb:'<path d="M9 18h6M9 21h6M9 15c-5-4-4-12 3-12s8 8 3 12v1H9z"/>',wifi:'<path d="M3 8a15 15 0 0 1 18 0M6 12a10 10 0 0 1 12 0M9 16a5 5 0 0 1 6 0"/><circle cx="12" cy="20" r=".6"/>',key:'<circle cx="8" cy="9" r="5"/><path d="m12 13 8 8m-3-3 3-3"/>'};
const escape=value=>String(value).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
function persist(){try{localStorage.setItem(storageKey,JSON.stringify(state));}catch{/* Preview works without persistence. */}}
function node(tag,className,content){const element=document.createElement(tag);if(className)element.className=className;if(content!==undefined)element.textContent=content;return element;}
function button(label,className,action){const element=node('button',className,label);element.type='button';element.addEventListener('click',action);return element;}
function preview(){const element=node('div','welcome-preview glass');element.innerHTML='<div class="mini-header"><span class="mini-time">21:14</span><span>Soft Glass Launcher</span></div><div class="mini-label">'+text('Apps','Apps')+'</div><div class="mini-title">'+text('Your selected apps','Tus apps seleccionadas')+'</div><div class="mini-apps">'+state.apps.slice(0,6).map(name=>'<div class="mini-app">'+escape(name)+'</div>').join('')+'</div>';return element;}
function choice(label,selected,action){const item=button(label,'choice',action);item.setAttribute('aria-pressed',String(selected));return item;}
function scene(){const element=node('div','scene');$('#content').append(element);return element;}
function welcome(){const body=scene();body.append(preview(),node('p','understated',text('Choose your apps and optional connections. Everything can be changed later in Settings.','Elegí tus apps y las conexiones que quieras usar. Todo se puede cambiar después en Settings.')));}
function appearance(){const body=scene(),row=node('div','theme-options');[['dark','Dark','Oscuro'],['light','Light','Claro'],['system','System','Sistema']].forEach(([value,a,b])=>row.append(choice(text(a,b),state.theme===value,()=>{state.theme=value;render(false);})));body.append(row,preview());const reduce=button('','setting-line',()=>{state.reduced=!state.reduced;render(false);});reduce.innerHTML='<span><strong>'+text('Reduced motion','Movimiento reducido')+'</strong><small>'+text('Gentler transitions and immediate focus.','Transiciones suaves y foco inmediato.')+'</small></span><span class="setting-value">'+text(state.reduced?'On':'Off',state.reduced?'Activado':'Desactivado')+'</span>';reduce.setAttribute('aria-pressed',String(state.reduced));body.append(reduce);}
function yourApps(){const body=scene(),grid=node('div','app-grid');apps.forEach(([name,color])=>{const selected=state.apps.includes(name),item=button('','app-option',()=>{if(selected)state.apps=state.apps.filter(a=>a!==name);else state.apps.push(name);render(false);});item.innerHTML='<span class="app-avatar" style="background:'+color+';color:white">'+escape(name[0])+'</span><span>'+escape(name)+'</span><span class="app-check">'+(selected?'✓':'')+'</span>';item.setAttribute('aria-pressed',String(selected));item.setAttribute('aria-label',name+'. '+text(selected?'Selected':'Not selected',selected?'Seleccionada':'Sin seleccionar'));grid.append(item);});body.append(grid,node('p','understated',text('Selected apps appear in your slider. All apps remains available. Hold an app on Home to reorder it.','Las seleccionadas aparecen en tu slider. All apps conserva el catálogo completo. Mantené una app en Home para ordenarla.')));$('#footer-note').textContent=text(state.apps.length+' selected',state.apps.length+' seleccionadas');}
function weather(){const body=scene(),row=node('form','input-row'),input=node('input','search-field');input.placeholder=text('City, country','Ciudad, país');input.setAttribute('aria-label',text('Search city','Buscar ciudad'));input.autocomplete='off';const results=node('div','cities');function fill(){results.replaceChildren();const query=input.value.trim();const list=query?cities.filter(c=>c.join(' ').toLowerCase().includes(query.toLowerCase())):cities.slice(0,4);if(query&&!list.length)list.push([query,text('Preview city','Ciudad de ejemplo')]);list.forEach(([name,country])=>{const item=choice('',state.city===name,()=>{state.city=name;render(false);});item.classList.add('city');item.innerHTML='<strong>'+escape(name)+'</strong><small>'+escape(country)+'</small>';results.append(item);});}row.append(input,button(text('Search','Buscar'),'secondary',fill));row.addEventListener('submit',event=>{event.preventDefault();fill();});body.append(row,results,node('p','understated',state.city?text('Forecast location: ','Ciudad del pronóstico: ')+state.city:text('No location permission or exact address needed. Skip to keep weather off.','No necesita permiso de ubicación ni tu dirección exacta. Omití este paso para dejar el clima desactivado.')));fill();}
function connections(){const body=scene(),list=node('div','connections');const items=[['stremio','Stremio','Continue Watching on Home','Seguir viendo en Home'],['spotify','Spotify','Current track while music is playing','Canción actual mientras reproduce música'],['bulb','WiZ lights','Discover lights on this Wi-Fi · on/off','Buscar luces en esta red · encendido/apagado'],['wifi','Wi-Fi name','Android asks for location access to show the network name','Android requiere ubicación para mostrar el nombre de la red'],['key','TMDB','Optional artwork key · your own credential','Clave opcional para imágenes · tu propia credencial']];items.forEach(([id,name,a,b])=>{const item=button('','connection',()=>{state.connections[id]=!state.connections[id];persist();render(false);$('#preview-note').textContent=text('Preview only: connection state simulated. The APK opens the corresponding setup or Android permission screen.','Solo prototipo: estado simulado. El APK abre la configuración o el permiso de Android correspondiente.');});item.innerHTML='<span class="connection-icon"><svg viewBox="0 0 24 24" aria-hidden="true">'+icons[id]+'</svg></span><span class="connection-copy"><strong>'+name+'</strong><small>'+text(a,b)+'</small></span><span class="connection-status">'+(state.connections[id]?text('Ready (demo)','Listo (demo)'):text(id==='bulb'?'Scan':'Set up',id==='bulb'?'Buscar':'Configurar'))+' →</span>';list.append(item);});body.append(list);}
function ready(){const body=scene(),summary=node('div','summary');[[text('Apps','Apps'),text(state.apps.length+' in your slider',state.apps.length+' en tu slider')],[text('Weather','Clima'),state.city||text('Off · choose later','Desactivado · elegir después')],[text('Appearance','Apariencia'),text({dark:'Dark',light:'Light',system:'System'}[state.theme],{dark:'Oscuro',light:'Claro',system:'Sistema'}[state.theme])],[text('Connections','Conexiones'),text(Object.values(state.connections).filter(Boolean).length+' configured in preview',Object.values(state.connections).filter(Boolean).length+' simuladas')]].forEach(([label,value])=>{const entry=node('div');entry.append(node('small','',label),node('strong','',value));summary.append(entry);});body.append(summary);const actions=node('div','home-actions');actions.append(button(text('Choose default Home','Elegir Home predeterminado'),'secondary',()=>{$('#preview-note').textContent=text('APK: opens Android Home settings. Some TVs require a separate remote-button mapping.','APK: abre los ajustes Home de Android. Algunas TVs necesitan mapear el botón del control.');}),button(text('Start on power on','Iniciar al encender'),'secondary',()=>{$('#preview-note').textContent=text('APK: optional startup guide with Android accessibility settings. No permission is granted automatically.','APK: guía opcional de inicio con ajustes de accesibilidad de Android. Ningún permiso se concede automáticamente.');}));body.append(actions,node('p','understated',text('Setup can be reopened in Settings → Customize → Setup guide.','Podés volver a abrir la guía en Settings → Customize → Setup guide.')));}
const titles=[['Set up your Home','Configurá tu Home'],['Choose the appearance','Elegí la apariencia'],['Choose your apps','Elegí tus apps'],['Choose your forecast city','Elegí la ciudad del pronóstico'],['Connect what you use','Conectá lo que usás'],['Your Home is ready','Tu Home está listo']];
const descriptions=[['A few choices to get started. Your accounts and preferences stay on this TV.','Unas elecciones para empezar. Tus cuentas y preferencias quedan en esta TV.'],['Light, dark or follow your TV. Changes appear immediately.','Claro, oscuro o el tema de la TV. Los cambios se ven al instante.'],['Only apps installed on the TV appear in the APK. These are examples.','El APK muestra solo las apps instaladas en la TV. Estos son ejemplos.'],['Search by city and country. You can leave weather off.','Buscá por ciudad y país. Podés dejar el clima desactivado.'],['Every connection is optional. You can configure them later.','Todas las conexiones son opcionales. Podés configurarlas después.'],['Choose how to open Home. System support varies between TVs.','Elegí cómo abrir Home. Las opciones dependen de cada TV.']];
function renderPalette(){
  document.documentElement.lang=state.lang;
  const dark=state.theme==='dark'||(state.theme==='system'&&matchMedia('(prefers-color-scheme: dark)').matches);
  $('.setup').dataset.theme=dark?'dark':'light';
}
function renderSteps(){
  $('#steps').replaceChildren();
  (state.lang==='es'?es:en).forEach((label,index)=>{
    const item=button('','step',()=>go(index));
    item.innerHTML='<span class="step-number">'+(index+1)+'</span><span class="step-label">'+label+'</span>';
    if(index===state.step)item.setAttribute('aria-current','step');
    $('#steps').append(item);
  });
}
function renderCopy(){
  const languageIndex=state.lang==='es'?1:0;
  $('#eyebrow').textContent=text('SETUP','CONFIGURACIÓN')+' · '+(state.step+1)+' / 6';
  $('#title').textContent=titles[state.step][languageIndex];
  $('#description').textContent=descriptions[state.step][languageIndex];
  $('#remote-hint').innerHTML=text('Use the arrows to move.<br>Press OK to choose.','Movete con las flechas.<br>Presioná OK para elegir.');
  $('#language').textContent=text('Español','English');
}
function renderFooter(){
  const nextLabels=[['Get started →','Comenzar →'],['Continue →','Continuar →'],['Continue →','Continuar →'],['Continue →','Continuar →'],['Continue →','Continuar →'],['Open Home →','Abrir Home →']];
  $('#back').hidden=state.step===0;$('#later').hidden=state.step===5;
  $('#later').textContent=state.step===0?text('Set up later','Configurar después'):text('Skip','Omitir');
  $('#back').textContent=text('Back','Atrás');$('#next').textContent=text(...nextLabels[state.step]);
}
function renderMotion(animate){
  $('#content').classList.remove('reveal');
  if(!animate||state.reduced)return;
  void $('#content').offsetWidth;$('#content').classList.add('reveal');
}
function render(animate=true){
  persist();
  const focusIndex=Array.from($('#content').querySelectorAll('button,input')).indexOf(document.activeElement);
  renderPalette();renderSteps();renderCopy();
  $('#content').replaceChildren();$('#footer-note').textContent=text('Optional choices can be skipped.','Podés omitir lo opcional.');
  [welcome,appearance,yourApps,weather,connections,ready][state.step]();
  renderFooter();renderMotion(animate);
  if(!animate&&focusIndex>=0)$('#content').querySelectorAll('button,input')[focusIndex]?.focus();
}
function go(step){state.step=Math.max(0,Math.min(5,step));render();$('#content').scrollTop=0;($('#content').querySelector('button,input')||$('#next')).focus();}
$('#next').addEventListener('click',()=>{if(state.step===5){location.href='../launcher/index.html';return;}go(state.step+1);});
$('#back').addEventListener('click',()=>go(state.step-1));$('#later').addEventListener('click',()=>go(state.step===0?5:state.step+1));$('#language').addEventListener('click',()=>{state.lang=state.lang==='en'?'es':'en';render(false);});$('#reset').addEventListener('click',()=>{Object.assign(state,JSON.parse(JSON.stringify(defaults)));go(0);});
document.addEventListener('keydown',event=>{if(event.key==='Escape'){event.preventDefault();go(state.step-1);return;}if(!event.key.startsWith('Arrow')||event.target.tagName==='INPUT')return;const current=document.activeElement;if(!$('.setup').contains(current))return;const rect=current.getBoundingClientRect(),horizontal=/Left|Right/.test(event.key),direction=/Right|Down/.test(event.key)?1:-1;const x=rect.left+rect.width/2,y=rect.top+rect.height/2;let candidate=null,score=Infinity;$('.setup').querySelectorAll('button,input,a').forEach(element=>{if(element===current||element.disabled||!element.getClientRects().length)return;const bounds=element.getBoundingClientRect(),dx=bounds.left+bounds.width/2-x,dy=bounds.top+bounds.height/2-y,axis=horizontal?dx:dy,cross=horizontal?dy:dx;if(axis*direction<=2)return;const distance=Math.abs(axis)+Math.abs(cross)*2;if(distance<score){score=distance;candidate=element;}});event.preventDefault();candidate?.focus();});
render();
