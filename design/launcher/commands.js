'use strict';

/* Deliberately local: this prototype previews routes; it never controls a TV. */
(() => {
  const form=document.querySelector('#command-form');
  const input=document.querySelector('#tv-command');
  const voiceButton=document.querySelector('#voice-command');
  const result=document.querySelector('#command-result');
  const previewNote=document.querySelector('#preview-note');
  const commandParams=new URLSearchParams(location.search);
  const intentRules=[
    {app:'Play Store',pattern:/^(?:download|install|get|descargar|descarg[aá](?:me)?|instalar|instal[aá](?:me)?|bajar|baj[aá])\s+(?:(?:(?:la|una|the|an?)\s+)?app\s+(?:(?:llamada|called)\s+)?)?(.+)$/iu},
    {app:'Stremio',pattern:/^(?:play|pon[eé](?:me)?|reproducir|reproduce|reproduc[ií])\s+(?:(?:la|una|the|a)\s+)?(?:pel[ií]cula|peli|serie|movie|film|show)\s+(.+)$/iu},
    {app:'Stremio',pattern:/^(?:watch|see|ver|mirar|mir[aá])\s+(?:(?:(?:la|una|the|a)\s+)?(?:pel[ií]cula|peli|serie|movie|film|show)\s+)?(.+)$/iu},
    {app:'Spotify',pattern:/^(?:play|reproducir|reproduce|reproduc[ií]|escuchar|escuch[aá](?:me)?|listen(?:\s+to)?|pon[eé](?:me)?|ponme)\s+(?:(?:(?:la|una|the|a)\s+)?(?:canci[oó]n|tema|song|track|m[uú]sica)\s+)?(.+)$/iu}
  ];
  const appNames={'stremio':'Stremio','spotify':'Spotify','play store':'Play Store','google play':'Play Store'};
  let recognition=null;

  function showResult(message) {
    result.textContent=message;
    result.hidden=false;
  }
  function helpMessage(text) {
    const greeting=/^[¡¿\s]*(?:hola|buenas|buenos d[ií]as|buenas tardes|buenas noches|hello|hi|hey|help|ayuda)[!?.\s]*$/iu.test(text);
    const question=/^[¿\s]*(?:what can you do|qu[eé] (?:puedes|pod[eé]s) hacer)[?\s]*$/iu.test(text);
    if (!greeting && !question) return '';
    const spanish=/hola|buen|ayuda|qu[eé]/iu.test(text);
    return spanish?'Hola. ¿Qué querés ver, escuchar o instalar?':'Hello. Search for a movie, song or app.';
  }
  function cleanRequest(text) {
    const polite=/^(?:por favor[,\s]+|please[,\s]+)/iu;
    return text.trim().replace(/^(?:hola|hello|hey)[,!]\s*/iu,'')
      .replace(polite,'')
      .replace(/^(?:quiero|quisiera|necesito|me gustar[ií]a|puedes|pod[eé]s|i want to|i need to|i(?:['’]d| would) like to|can you|could you)\s+/iu,'')
      .replace(polite,'').replace(/[,\s]+(?:por favor|please)[.!?]*$/iu,'');
  }
  function commandIntent(text) {
    const message=helpMessage(text.trim());
    if (message) return {message};
    const request=cleanRequest(text);
    const destination=request.match(/^(.+?)\s+(?:en|on|in)\s+(stremio|spotify|play store|google play)[.!?]*$/iu);
    const body=destination?destination[1]:request;
    const app=destination?appNames[destination[2].toLowerCase()]:'';
    for (const rule of intentRules) {
      const match=body.match(rule.pattern);
      if (match) return {app:app||rule.app,query:match[1].replace(/[.!?]+$/u,'').trim()};
    }
    const query=body.replace(/^(?:buscar|busc[aá](?:me)?|b[uú]scame|search(?:\s+for)?|look for|find)\s+/iu,'').trim();
    return {app:app||'Stremio',query};
  }
  function submitCommand(event) {
    event?.preventDefault();
    if (!input.value.trim()) { input.focus(); return; }
    const intent=commandIntent(input.value);
    if (intent.message) {
      showResult(intent.message);
      previewNote.textContent='Bilingual search preview. Spanish and English requests share the same movie, music and app destinations.';
      return;
    }
    showResult(`${intent.app} · ${intent.query}`);
    previewNote.textContent=`Route preview: ${intent.app} search for “${intent.query}”. No TV app is launched in this HTML prototype.`;
  }
  function setListening(listening) {
    voiceButton.setAttribute('aria-pressed',String(listening));
    voiceButton.setAttribute('aria-label',listening?'Stop listening':'Speak a command');
    if (listening) showResult('Listening…');
  }
  function voiceError(event) {
    const messages={
      'not-allowed':'Microphone access was not allowed. You can type instead.',
      'audio-capture':'No microphone is available. You can type instead.',
      'no-speech':'No speech detected. Try again or type.',
      network:'Voice service is unavailable. You can type instead.'
    };
    showResult(messages[event.error]||'Voice is unavailable. You can type instead.');
  }
  function finishVoice() {
    recognition=null;
    setListening(false);
    if (result.textContent==='Listening…') showResult('Voice stopped. Try again or type.');
  }
  function startVoice() {
    if (recognition) { recognition.stop(); return; }
    const Recognition=window.SpeechRecognition||window.webkitSpeechRecognition;
    if (!Recognition) { showResult('Voice is unavailable in this browser. You can type.'); input.focus(); return; }
    recognition=new Recognition();
    recognition.lang=document.querySelector('#screen').dataset.voiceLanguage||'es-AR';
    recognition.continuous=false;
    recognition.interimResults=false;
    recognition.onstart=()=>setListening(true);
    recognition.onend=finishVoice;
    recognition.onerror=voiceError;
    recognition.onresult=event=>{ input.value=event.results[0][0].transcript.slice(0,240); submitCommand(); };
    try { recognition.start(); }
    catch { recognition=null; setListening(false); showResult('Voice could not start. You can type instead.'); }
  }

  form.addEventListener('submit',submitCommand);
  voiceButton.addEventListener('click',startVoice);
  window.addEventListener('pagehide',()=>recognition?.abort());
  if (commandParams.has('command')) {
    input.value=commandParams.get('command').slice(0,240);
    submitCommand();
  }
})();
