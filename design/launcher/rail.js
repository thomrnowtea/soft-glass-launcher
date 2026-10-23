'use strict';

const screen = document.querySelector('#screen');
const viewport = document.querySelector('#viewport');
const track = document.querySelector('#app-track');
const panel = document.querySelector('#quick-panel');
const scrim = document.querySelector('#scrim');
const panelBody = document.querySelector('#panel-body');
const note = document.querySelector('#preview-note');
const spotifyNotch = document.querySelector('#spotify-notch');
const spotifyPreview = document.querySelector('#spotify-preview');
const params = new URLSearchParams(location.search);
const appCatalog = [
  {id:'stremio', name:'Stremio', mark:'brand-stremio', text:'stremio'},
  {id:'smarttube', name:'SmartTube', mark:'brand-tube', text:'SmartTube'},
  {id:'netflix', name:'Netflix', text:'NETFLIX'},
  {id:'spotify', name:'Spotify', mark:'brand-spotify', text:'Spotify'},
  {id:'prime', name:'Prime Video', text:'prime video'},
  {id:'disney', name:'Disney+', text:'Disney+'},
  {id:'twitch', name:'Twitch', text:'twitch'},
  {id:'kodi', name:'Kodi', text:'KODI'},
  {id:'vlc', name:'VLC', text:'VLC'},
  {id:'play', name:'Play Store', mark:'play-store-mono', text:'Google Play'},
  {id:'remapper', name:'Buttons Remapper', text:'Buttons Remapper'},
  {id:'youtube', name:'YouTube', mark:'brand-tube', text:'YouTube'}
];
const storedHome=loadHomePreferences();
const defaultHomeApps=['stremio','smarttube','netflix','spotify','prime','disney'];
let homeAppIds=Array.isArray(storedHome.homeApps)?[...new Set(storedHome.homeApps)].filter(id=>appCatalog.some(app=>app.id===id)):defaultHomeApps;
let weatherCity=typeof storedHome.weatherCity==='string'&&storedHome.weatherCity.trim()?storedHome.weatherCity.trim().slice(0,80):'Palermo';
let appList=[];
let movingAppId='';
const widgetLabels={inputs:'Inputs',search:'Search',weather:'Weather',lights:'Lights',connections:'Wi-Fi, audio & All apps'};
const widgetVisibility=Object.fromEntries(Object.keys(widgetLabels).map(key=>[key,storedHome.widgets?.[key]!==false]));
const appContext=document.querySelector('#app-context');
let appContextOrigin=null;
const quickSettings = [
  {name:'Wi-Fi', icon:'wifi', action:'system'},
  {name:'Sound', icon:'sound', action:'system'},
  {name:'Picture', icon:'picture', action:'system'},
  {name:'Inputs', icon:'inputs', action:'inputs'},
  {name:'Customize', icon:'customize', action:'customize'},
  {name:'Accessibility', icon:'accessibility', action:'system'}
];
const lightStates = new Map([
  ['1',{name:'Light 1',on:true}],
  ['2',{name:'Light 2',on:false}]
]);
let selectedApp = 0;
let windowStart = 0;
let panelOrigin = null;
let activePanel = '';
let lastArrow = 0;
let currentTitle = 0;
let renameId = '';
let themeChoice = 'dark';
let spotifyPlaying = false;
const positionMemory = new Map();

function icon(name, extra = '') {
  return `<svg class="icon ${extra}" aria-hidden="true" focusable="false"><use href="#${name}"/></svg>`;
}
function brand(app) {
  const mark = app.mark ? `<svg aria-hidden="true" focusable="false"><use href="#${app.mark}"/></svg>` : '';
  const arrow = app.id === 'prime' ? '<svg viewBox="0 0 140 35" aria-hidden="true"><path d="M5 5c40 24 83 23 125 0M113 3l20 1-4 19"/></svg>' : '';
  return `<span class="app-banner brand-${app.id}">${mark}<span>${app.text}</span>${arrow}</span>`;
}
function loadHomePreferences() {
  try { return JSON.parse(localStorage.getItem('inicio-tv-home-preview-v1')||'{}')||{}; }
  catch { return {}; }
}
function saveHomePreferences() {
  try { localStorage.setItem('inicio-tv-home-preview-v1',JSON.stringify({homeApps:homeAppIds,weatherCity,widgets:widgetVisibility})); return true; }
  catch { note.textContent='Browser storage is unavailable. Changes are kept for this preview session.'; return false; }
}
function renderApps(focus = false) {
  appList=homeAppIds.map(id=>appCatalog.find(app=>app.id===id));
  if (!appList.length) {
    selectedApp=0; windowStart=0; track.style.setProperty('--rail-x','0px');
    track.innerHTML='<button class="empty-rail" data-panel="apps">Choose apps</button>';
    track.querySelector('button').addEventListener('click',event=>openPanel('apps',event.currentTarget));
    document.querySelector('#rail-position').textContent='No apps selected';
    if (focus) track.querySelector('button').focus({preventScroll:true});
    return;
  }
  track.innerHTML = appList.map((app, i) => `<button class="app-tile" data-app="${i}" aria-label="${app.name}, ${i+1} of ${appList.length}">${brand(app)}<span class="app-label">${app.name}</span></button>`).join('');
  track.querySelectorAll('[data-app]').forEach(button => {
    const index = Number(button.dataset.app);
    button.addEventListener('focus', () => selectApp(index));
    button.addEventListener('click', () => { if (movingAppId) finishAppMove(); else systemAction(appList[index].name); });
    bindHold(button,()=>beginAppMove(appList[index].id),()=>!movingAppId);
  });
  selectApp(selectedApp);
  updateMoveCaption();
  if (focus) focusApp(selectedApp);
}
function selectApp(index) {
  selectedApp = Math.max(0, Math.min(appList.length - 1, index));
  if (selectedApp < windowStart) windowStart = selectedApp;
  if (selectedApp >= windowStart + 6) windowStart = selectedApp - 5;
  windowStart = Math.max(0, Math.min(Math.max(0,appList.length - 6), windowStart));
  track.style.setProperty('--rail-x', `${-windowStart * (265.333333 + 24)}px`);
  document.querySelector('#rail-position').textContent = `${windowStart+1}–${Math.min(windowStart+6,appList.length)} / ${appList.length}`;
  updateMoveCaption();
}
function focusApp(index) {
  const next = Math.max(0, Math.min(appList.length - 1, index));
  const target=track.querySelector(`[data-app="${next}"]`)||track.querySelector('button');
  target?.focus({preventScroll:true});
}
function setPage(page, focus = false) {
  screen.dataset.page = page==='lower' && !Object.values(widgetVisibility).some(Boolean) ? 'home' : page;
  if (focus && page === 'home') focusApp(selectedApp);
  if (focus && page === 'lower') {
    const visible=directionalNavigation.controls(screen.querySelector('.home-controls'))[0];
    (visible||track.querySelector('button'))?.focus({preventScroll:true});
  }
}
function setTheme(mode) {
  themeChoice = mode;
  const theme = mode === 'system' ? (matchMedia('(prefers-color-scheme:light)').matches?'light':'dark') : mode;
  screen.dataset.theme = theme;
  const toggle = document.querySelector('#theme-preview');
  toggle.textContent = 'Light theme';
  toggle.setAttribute('aria-pressed', String(theme === 'light'));
}
function systemAction(name) {
  note.textContent = `Design preview: ${name}. No app is launched and no device setting changes.`;
}
function setSpotifyPreview(mode) {
  spotifyPlaying = mode === 'playing';
  spotifyPreview.value = spotifyPlaying ? 'playing' : 'off';
  syncSpotifyVisibility();
}
function syncSpotifyVisibility() {
  const previous = rowButtons('header')[positionMemory.get('header')||0];
  const focused = spotifyNotch.contains(document.activeElement);
  spotifyNotch.hidden = !spotifyPlaying;
  const column = rowButtons('header').indexOf(previous);
  positionMemory.set('header',Math.max(0,column));
  if (spotifyNotch.hidden && focused) rowButtons('header')[0]?.focus({preventScroll:true});
}
function settingsMarkup() {
  return `<div class="settings-grid" data-focus-row="panel-grid">${quickSettings.map(item => `<button class="setting-tile" data-quick="${item.action}" data-name="${item.name}">${icon(item.icon)}<span>${item.name}</span></button>`).join('')}</div><button class="all-settings" data-system="All TV settings"><span>All settings</span>${icon('right')}</button>`;
}
function customizeMarkup() {
  const language=screen.dataset.voiceLanguage||'es-AR';
  const themes=['dark','light','system'].map(theme=>`<button data-theme-choice="${theme}" aria-pressed="${themeChoice===theme}">${theme[0].toUpperCase()+theme.slice(1)}</button>`).join('');
  return `<div class="theme-choices" role="group" aria-label="Theme">${themes}</div><p class="panel-hint">Hold an app on the Home slider to move it. Press OK to finish.</p><button class="panel-row" data-panel="weather-location"><span>Weather location<small class="customize-city">${escapeHtml(weatherCity)}</small></span>${icon('right')}</button><h3 class="panel-section">Widgets</h3>${widgetOptionsMarkup()}<h3 class="panel-section">Motion & voice</h3><button class="panel-row" id="motion-setting" aria-pressed="${screen.classList.contains('reduced-motion')}"><span>Reduced motion</span><small>${screen.classList.contains('reduced-motion')?'On':'Off'}</small></button><label class="voice-language-label" for="voice-language">Voice language</label><select class="rename-input" id="voice-language"><option value="es-AR" ${language==='es-AR'?'selected':''}>Español</option><option value="en-US" ${language==='en-US'?'selected':''}>English</option></select>`;
}
function widgetOptionsMarkup() {
  return Object.entries(widgetLabels).map(([key,label])=>`<button class="panel-row widget-option" data-widget-choice="${key}" role="switch" aria-checked="${widgetVisibility[key]}"><span>${label}</span><small>${widgetVisibility[key]?'On':'Off'}</small></button>`).join('');
}
function connectionsMarkup() {
  const rows=[['Stremio','Linked'],['Spotify mini player','Enabled'],['Wi-Fi network name','Home Wi-Fi'],['TMDB','Artwork']];
  return '<h3 class="panel-section">Connections</h3>'+rows.map(([name,state])=>`<button class="panel-row" data-system="${name} connection"><span>${name}</span><small>${state}</small></button>`).join('');
}
function applyWidgetVisibility() {
  for (const [key,shown] of Object.entries(widgetVisibility)) screen.querySelector(`[data-widget="${key}"]`).hidden=!shown;
  collapseWidgetGroup('.control-groups'); collapseWidgetGroup('.right-widgets'); collapseWidgetGroup('.widgets');
  const any=Object.values(widgetVisibility).some(Boolean);
  screen.querySelector('.home-controls').hidden=!any;
  screen.querySelector('.page-cue').hidden=!any;
  if (!any) setPage('home');
}
function collapseWidgetGroup(selector) {
  const group=screen.querySelector(selector);
  const count=[...group.children].filter(child=>!child.hidden).length;
  group.hidden=count===0; group.classList.toggle('single-widget',count===1);
}
function toggleWidget(button) {
  const key=button.dataset.widgetChoice; widgetVisibility[key]=!widgetVisibility[key];
  button.setAttribute('aria-checked',String(widgetVisibility[key]));
  button.querySelector('small').textContent=widgetVisibility[key]?'On':'Off';
  saveHomePreferences(); applyWidgetVisibility();
}
function appsMarkup() {
  const sorted = [...appCatalog].sort((a,b) => a.name.localeCompare(b.name));
  return `<div class="app-directory">${sorted.map(app => `<button class="directory-item" data-directory="${app.id}"><span class="directory-mark">${app.mark?icon(app.mark):app.name.slice(0,1)}</span><span>${app.name}</span></button>`).join('')}</div>`;
}
function lightRowsMarkup(editable = false) {
  return [...lightStates].map(([id,light]) => {
    const name=escapeHtml(light.name);
    const rename=editable?`<button class="rename" data-rename="${id}" aria-label="Rename ${name}">${icon('pencil')}</button>`:'';
    return `<div class="light-row" data-light="${id}"><button class="power" aria-pressed="${light.on}" aria-label="${name}, power"><span class="light-name">${name}</span><span class="light-state">${icon(light.on?'power':'power-off')}</span></button>${rename}</div>`;
  }).join('');
}
function lightsMarkup() {
  return `<button class="lights-master" id="all-lights">${allLightsContent()}</button><div class="panel-lights">${lightRowsMarkup(true)}</div><div class="lights-tools"><button data-light-tool="sync">Sync</button><button data-light-tool="scan">Scan</button></div>`;
}
function allLightsContent() {
  const nextOn=![...lightStates.values()].every(light=>light.on);
  return `${icon('bulb')}<span>${allLightsAction()}</span>${icon(nextOn?'power':'power-off')}`;
}
function allLightsAction() {
  return [...lightStates.values()].every(light=>light.on)?'All lights OFF':'All lights ON';
}
function powerAllLights() {
  const on=![...lightStates.values()].every(light=>light.on);
  lightStates.forEach((light,id)=>{ light.on=on; syncLight(id); });
  note.textContent='Group power preview updated locally. No WiZ command is sent.';
}
function lightTool(action) {
  const messages={sync:'Sync preview: query the known lights for their current state. No network request runs in this HTML.',scan:'Scan preview: discover additional WiZ lights while preserving known lights and aliases. No network scan runs in this HTML.'};
  note.textContent=messages[action];
}
function weatherMarkup() {
  return `<p class="panel-weather-current">18°</p><p class="weather-condition">Cloudy · High 19° · Low 14°</p><div class="panel-row"><span>Thursday</span>${icon('cloud')}<span>14° / 19°</span></div><div class="panel-row"><span>Friday</span>${icon('weather')}<span>15° / 22°</span></div><div class="panel-row"><span>Saturday</span>${icon('weather')}<span>16° / 24°</span></div><button class="panel-row" data-panel="weather-location"><span>Change location</span>${icon('right')}</button>`;
}
function weatherLocationMarkup() {
  return `<form class="weather-city-form" id="weather-city-form"><label for="weather-city">City, country</label><input class="rename-input" id="weather-city" maxlength="80" required autocomplete="off" value="${escapeHtml(weatherCity)}"><button class="save-name" type="submit">Save</button></form>`;
}
function saveWeatherCity(event) {
  event.preventDefault();
  const city=document.querySelector('#weather-city').value.trim();
  if (!city) return;
  weatherCity=city;
  const saved=saveHomePreferences();
  renderWeatherCity(); openPanel('customize');
  note.textContent=saved?'Weather city saved in this preview. Forecast values remain illustrative; native geocoding and forecast integration are pending.':'Weather city changed for this session. Browser storage is unavailable; forecast values remain illustrative.';
}
function renderWeatherCity() {
  document.querySelector('#weather-city-label').textContent=weatherCity;
  screen.querySelector('.weather-peek small').textContent=weatherCity;
  screen.querySelector('.forecast').setAttribute('aria-label',`${weatherCity} forecast, example weather`);
  screen.querySelector('.weather-peek').setAttribute('aria-label',`Weather for ${weatherCity}`);
}
function renameMarkup() {
  const name = lightStates.get(renameId).name;
  return `<label class="panel-text" for="light-name">Name</label><input class="rename-input" id="light-name" maxlength="40" autocomplete="off" value="${escapeHtml(name)}"><button class="save-name" id="save-name">Save</button>`;
}
function escapeHtml(text) {
  return text.replace(/[&<>"']/g, character => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[character]));
}
const panelSpecs = {
  settings:{title:'Settings',subtitle:'Wednesday, 21 October',render:settingsMarkup},
  customize:{title:'Customize',subtitle:'Home preferences',render:()=>customizeMarkup()+connectionsMarkup()},
  apps:{title:'All apps',subtitle:'Hold an app for options',render:appsMarkup},
  lights:{title:'Lights',subtitle:'WiZ',render:lightsMarkup},
  weather:{title:()=>weatherCity,subtitle:'Weather',render:weatherMarkup},
  'weather-location':{title:'Weather location',subtitle:'',render:weatherLocationMarkup},
  rename:{title:'Rename light',subtitle:'',render:renameMarkup}
};
function openPanel(name, origin = document.activeElement) {
  if (!panelSpecs[name]) return;
  closeAppContext(false);
  if (movingAppId) finishAppMove();
  if (panel.hidden) panelOrigin = origin;
  activePanel = name;
  const spec = panelSpecs[name];
  document.querySelector('#panel-title').textContent = typeof spec.title==='function'?spec.title():spec.title;
  document.querySelector('#panel-subtitle').textContent = spec.subtitle;
  panelBody.innerHTML = spec.render();
  directionalNavigation.reset(panel);
  panel.scrollTop=0;
  panel.hidden = false; scrim.hidden = false;
  syncSpotifyVisibility();
  document.querySelector('#page-content').inert = true;
  bindPanel();
  const first = panelBody.querySelector('input,button');
  first?.focus({preventScroll:true});
  if (first instanceof HTMLInputElement) first.select();
}
function closePanel() {
  closeAppContext(false);
  panel.hidden = true; scrim.hidden = true; activePanel = '';
  syncSpotifyVisibility();
  document.querySelector('#page-content').inert = false;
  if (panelOrigin?.isConnected) panelOrigin.focus({preventScroll:true});
  else focusApp(selectedApp);
}
function bindPanel() {
  bindActions(panelBody);
  panelBody.querySelectorAll('[data-quick]').forEach(button => button.addEventListener('click', () => quickAction(button)));
  panelBody.querySelectorAll('[data-theme-choice]').forEach(button => button.addEventListener('click', () => chooseTheme(button.dataset.themeChoice)));
  panelBody.querySelectorAll('[data-directory]').forEach(button => {
    button.addEventListener('click',()=>directoryAction(button.dataset.directory));
    bindHold(button,()=>openAppContext(button.dataset.directory,button));
  });
  panelBody.querySelectorAll('[data-widget-choice]').forEach(button=>button.addEventListener('click',()=>toggleWidget(button)));
  panelBody.querySelector('#all-lights')?.addEventListener('click',powerAllLights);
  panelBody.querySelectorAll('[data-light-tool]').forEach(button=>button.addEventListener('click',()=>lightTool(button.dataset.lightTool)));
  panelBody.querySelector('#weather-city-form')?.addEventListener('submit',saveWeatherCity);
  panelBody.querySelector('#save-name')?.addEventListener('click', saveName);
  panelBody.querySelector('#motion-setting')?.addEventListener('click', toggleMotion);
  panelBody.querySelector('#voice-language')?.addEventListener('change',event=>{ screen.dataset.voiceLanguage=event.target.value; });
}
function quickAction(button) {
  const action = button.dataset.quick;
  if (action === 'customize') { openPanel('customize'); return; }
  if (action === 'inputs') { closePanel(); setPage('lower',true); return; }
  systemAction(button.dataset.name);
}
function chooseTheme(theme) {
  setTheme(theme);
  panelBody.querySelectorAll('[data-theme-choice]').forEach(button=>button.setAttribute('aria-pressed',String(button.dataset.themeChoice===theme)));
}
function toggleMotion() {
  screen.classList.toggle('reduced-motion');
  const button = document.querySelector('#motion-setting');
  const reduced = screen.classList.contains('reduced-motion');
  button.setAttribute('aria-pressed',String(reduced));
  button.querySelector('small').textContent = reduced ? 'On' : 'Off';
}
function directoryAction(id) {
  const app=appCatalog.find(item=>item.id===id);
  closePanel(); systemAction(app.name);
}
function toggleHomeApp(id) {
  const index=homeAppIds.indexOf(id);
  if (index<0) { homeAppIds.push(id); selectedApp=homeAppIds.length-1; }
  else { homeAppIds.splice(index,1); selectedApp=Math.max(0,Math.min(index,homeAppIds.length-1)); }
  saveHomePreferences(); renderApps();
  note.textContent=index<0?'Added to the Home slider.':'Removed from the Home slider; still available in All apps.';
}
function openAppContext(id,origin) {
  const app=appCatalog.find(item=>item.id===id);
  appContextOrigin=origin;
  appContext.innerHTML=`<h3 id="app-context-title">${app.name}</h3><button data-app-action="pin">${homeAppIds.includes(id)?'Remove from Home':'Add to Home'}</button><button data-app-action="details">Details</button>`;
  appContext.hidden=false; panel.inert=true;
  positionAppContext(origin);
  appContext.querySelectorAll('button').forEach(button=>button.addEventListener('click',()=>{
    const action=button.dataset.appAction; closeAppContext();
    if (action==='pin') toggleHomeApp(id);
    else systemAction(`${app.name} → Android App info`);
  }));
  appContext.querySelector('button').focus({preventScroll:true});
}
function positionAppContext(origin) {
  const frame=screen.getBoundingClientRect(),anchor=origin.getBoundingClientRect(),scale=frame.width/1920;
  const x=(anchor.left-frame.left)/scale,y=(anchor.top-frame.top)/scale;
  appContext.style.left=`${Math.max(24,Math.min(x,1920-appContext.offsetWidth-24))}px`;
  appContext.style.top=`${Math.max(24,Math.min(y,1080-appContext.offsetHeight-24))}px`;
}
function closeAppContext(restore=true) {
  if (appContext.hidden) return;
  appContext.hidden=true; panel.inert=false;
  if (restore && appContextOrigin?.isConnected) appContextOrigin.focus({preventScroll:true});
  appContextOrigin=null;
}
function handleAppContextKeys(event) {
  if (appContext.hidden) return false;
  if (event.key==='Escape') { event.preventDefault(); closeAppContext(); return true; }
  if (['ArrowLeft','ArrowRight'].includes(event.key)) { event.preventDefault(); return true; }
  const buttons=[...appContext.querySelectorAll('button')];
  const index=Math.max(0,buttons.indexOf(document.activeElement));
  if (event.key==='Tab') {
    event.preventDefault(); buttons[(index+(event.shiftKey?-1:1)+buttons.length)%buttons.length].focus();
  } else if (['ArrowUp','ArrowDown'].includes(event.key)) {
    event.preventDefault(); buttons[Math.max(0,Math.min(buttons.length-1,index+(event.key==='ArrowUp'?-1:1)))].focus();
  }
  return true;
}
function beginAppMove(id) {
  movingAppId=id;
  selectedApp=homeAppIds.indexOf(movingAppId);
  screen.dataset.arranging='true'; focusApp(selectedApp); updateMoveCaption();
}
function updateMoveCaption() {
  if (movingAppId) document.querySelector('#rail-position').textContent='Move with ← → · Enter to finish';
}
function moveHomeApp(delta) {
  const index=homeAppIds.indexOf(movingAppId),target=index+delta;
  if (target<0 || target>=homeAppIds.length) return;
  [homeAppIds[index],homeAppIds[target]]=[homeAppIds[target],homeAppIds[index]];
  selectedApp=target; renderApps(true);
}
function finishAppMove() {
  if (movingAppId) saveHomePreferences();
  movingAppId=''; screen.dataset.arranging='false'; selectApp(selectedApp);
}
function handleAppMoveKeys(event) {
  if (!movingAppId) return false;
  if (['ArrowUp','ArrowDown'].includes(event.key)) { finishAppMove(); return false; }
  if (!['ArrowLeft','ArrowRight','ArrowUp','ArrowDown','Enter',' ','Escape'].includes(event.key)) return false;
  event.preventDefault();
  const now=performance.now();
  if (event.repeat && now-lastArrow<85) return true;
  lastArrow=now;
  const deltas={ArrowLeft:-1,ArrowRight:1};
  if (deltas[event.key]) moveHomeApp(deltas[event.key]);
  if (!event.repeat && ['Enter',' ','Escape'].includes(event.key)) finishAppMove();
  return true;
}
function saveName() {
  const name = document.querySelector('#light-name').value.trim();
  if (!name) { document.querySelector('#light-name').focus(); return; }
  lightStates.get(renameId).name=name;
  syncLight(renameId);
  openPanel('lights');
}
function togglePower(button) {
  const id=button.closest('[data-light]').dataset.light;
  const light=lightStates.get(id);
  light.on=!light.on;
  syncLight(id);
  note.textContent='Lights preview: example state updated locally. No command is sent to WiZ.';
}
function syncLight(id) {
  const light=lightStates.get(id);
  screen.querySelectorAll(`.light-row[data-light="${id}"]`).forEach(row => {
    row.querySelector('.light-name').textContent=light.name;
    const power=row.querySelector('.power');
    power.setAttribute('aria-pressed',String(light.on));
    power.setAttribute('aria-label',`${light.name}, ${light.on?'on, turn off':'off, turn on'}`);
    row.querySelector('.light-state').innerHTML=icon(light.on?'power':'power-off');
    row.querySelector('.rename')?.setAttribute('aria-label',`Rename ${light.name}`);
  });
  const master=panelBody.querySelector('#all-lights');
  if (master) master.innerHTML=allLightsContent();
}
function bindActions(root) {
  root.querySelectorAll('[data-panel]').forEach(button => button.addEventListener('click', () => openPanel(button.dataset.panel,button)));
  root.querySelectorAll('[data-system]').forEach(button => button.addEventListener('click', () => systemAction(button.dataset.system)));
  root.querySelectorAll('[data-rename]').forEach(button => button.addEventListener('click', () => { renameId=button.dataset.rename; openPanel('rename',button); }));
  root.querySelectorAll('.power').forEach(button => button.addEventListener('click', () => togglePower(button)));
}
function stepTitle(delta) {
  currentTitle=(currentTitle+delta+3)%3;
  screen.querySelectorAll('.dots i').forEach((dot,index) => dot.classList.toggle('active',index===currentTitle));
  document.querySelector('#title-position').textContent=`${currentTitle+1} / 3`;
  note.textContent='Pagination preview. The example artwork stays fixed; the real launcher will use your Stremio queue.';
}
function rowButtons(name) {
  const row = screen.querySelector(`[data-focus-row="${name}"]`);
  if (!row || row.hidden) return [];
  return [...row.querySelectorAll('button,input,a[href]')].filter(control=>!control.disabled && !control.closest('[hidden]'));
}
function rowOf(element) {
  return element.closest('[data-focus-row]')?.dataset.focusRow;
}
function visibleRailControl(control) {
  if (!control.matches('[data-app]')) return true;
  const index=Number(control.dataset.app);
  return index>=windowStart && index<windowStart+6;
}
function moveRailHorizontally(key) {
  const delta=key==='ArrowLeft'?-1:1;
  const target=Math.max(0,Math.min(appList.length-1,selectedApp+delta));
  if (target===selectedApp) return;
  directionalNavigation.reset(document.querySelector('#page-content'));
  focusApp(target);
}
function revealMainControl(control) {
  const row=rowOf(control);
  if (row==='controls'||row==='widgets') setPage('lower');
  if (['header','resume','pagination'].includes(row)) setPage('home');
}
function moveMain(key) {
  const current=document.activeElement;
  const row=rowOf(current);
  if (!row) { focusApp(selectedApp); return; }
  const horizontal=key==='ArrowLeft'||key==='ArrowRight';
  if (row==='apps' && horizontal) { moveRailHorizontally(key); return; }
  const root=document.querySelector('#page-content');
  const candidates=directionalNavigation.controls(root).filter(visibleRailControl);
  const target=directionalNavigation.next(root,current,key,candidates);
  if (!target) return;
  revealMainControl(target);
  target.focus({preventScroll:true});
}
function movePanel(key) {
  const target=directionalNavigation.next(panel,document.activeElement,key);
  if (!target) return;
  target.focus({preventScroll:true});
  ensurePanelVisible(target);
}
function ensurePanelVisible(target) {
  if (!target) return;
  const bounds=panel.getBoundingClientRect(),rect=target.getBoundingClientRect();
  if (rect.bottom>bounds.bottom-24) panel.scrollTop+=rect.bottom-bounds.bottom+24;
  else if (rect.top<bounds.top+24) panel.scrollTop-=bounds.top+24-rect.top;
}
function back() {
  if (!appContext.hidden) { closeAppContext(); return; }
  if (!panel.hidden) { closePanel(); return; }
  setPage('home',true);
}
function trapTab(event) {
  const controls=[...panel.querySelectorAll('button,input,select')];
  const first=controls[0],last=controls[controls.length-1];
  if (event.shiftKey && document.activeElement===first) { event.preventDefault(); last.focus(); }
  if (!event.shiftKey && document.activeElement===last) { event.preventDefault(); first.focus(); }
}
function nativeEditor(element) {
  return element instanceof HTMLInputElement || element instanceof HTMLSelectElement;
}
function handleKeys(event) {
  if (handleAppContextKeys(event)) return;
  if (handleAppMoveKeys(event)) return;
  if (event.key==='Escape') { event.preventDefault(); back(); return; }
  if (!panel.hidden && event.key==='Tab') { trapTab(event); return; }
  if (nativeEditor(event.target)) return;
  if (!['ArrowLeft','ArrowRight','ArrowUp','ArrowDown'].includes(event.key)) return;
  event.preventDefault();
  const now=performance.now();
  if (event.repeat && now-lastArrow<85) return;
  lastArrow=now;
  if (panel.hidden) moveMain(event.key); else movePanel(event.key);
}
function previewView(view) {
  closePanel();
  setPage(view==='lower'?'lower':'home');
  if (view==='settings') openPanel('settings',document.querySelector('#settings-trigger'));
  else focusApp(selectedApp);
  document.querySelectorAll('[data-preview]').forEach(button=>button.setAttribute('aria-pressed',String(button.dataset.preview===view)));
}
function resize() {
  screen.style.setProperty('--scale',String(viewport.clientWidth/1920));
}

renderApps();
renderWeatherCity();
document.querySelector('#home-light-list').innerHTML=lightRowsMarkup();
bindActions(document.querySelector('#page-content'));
applyWidgetVisibility();
document.querySelector('#page-content').addEventListener('focusin',event=>{
  const row=rowOf(event.target);
  if (row) positionMemory.set(row,rowButtons(row).indexOf(event.target));
});
document.querySelectorAll('[data-title-step]').forEach(button=>button.addEventListener('click',()=>stepTitle(Number(button.dataset.titleStep))));
document.querySelector('#panel-close').addEventListener('click',closePanel);
scrim.addEventListener('click',closePanel);
document.querySelector('#theme-preview').addEventListener('click',()=>setTheme(screen.dataset.theme==='dark'?'light':'dark'));
spotifyPreview.addEventListener('change',()=>setSpotifyPreview(spotifyPreview.value));
spotifyNotch.querySelector('a').addEventListener('click',()=>{ note.textContent='Opening Spotify. The track and artwork in this preview are examples.'; });
document.querySelectorAll('[data-preview]').forEach(button=>button.addEventListener('click',()=>previewView(button.dataset.preview)));
document.addEventListener('keydown',handleKeys);
screen.addEventListener('pointerdown',event=>{
  if (!appContext.hidden && !appContext.contains(event.target)) { event.preventDefault(); event.stopPropagation(); closeAppContext(); }
},true);
new ResizeObserver(resize).observe(viewport);
setTheme(params.get('theme')==='light'?'light':'dark');
if (params.has('capture')) { document.body.classList.add('capture'); screen.classList.add('reduced-motion'); }
setSpotifyPreview(params.get('music')==='playing'?'playing':'off');
previewView(params.get('view')||'home');
if (params.has('panel')) openPanel(params.get('panel'),document.querySelector('#settings-trigger'));
if (params.get('focus')==='audio') {
  setPage('lower');
  screen.querySelector('.status-row[data-system="Sound"]').focus({preventScroll:true});
}
resize();
