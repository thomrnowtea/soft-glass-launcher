'use strict';

/* D-pad movement follows the layout, never the order of controls in the DOM. */
const directionalNavigation=(() => {
  const memory=new WeakMap();
  const directions={
    ArrowLeft:{axis:'x',cross:'y',start:'left',end:'right',crossStart:'top',crossEnd:'bottom',sign:-1},
    ArrowRight:{axis:'x',cross:'y',start:'left',end:'right',crossStart:'top',crossEnd:'bottom',sign:1},
    ArrowUp:{axis:'y',cross:'x',start:'top',end:'bottom',crossStart:'left',crossEnd:'right',sign:-1},
    ArrowDown:{axis:'y',cross:'x',start:'top',end:'bottom',crossStart:'left',crossEnd:'right',sign:1}
  };

  function controls(root) {
    return [...root.querySelectorAll('button,input,select,a[href]')].filter(control=>
      !control.disabled && !control.closest('[hidden],[inert]') && control.getClientRects().length>0);
  }
  function bounds(control,root) {
    const box=control.getBoundingClientRect(),container=root.getBoundingClientRect();
    const scale=container.width/root.offsetWidth||1;
    const left=(box.left-container.left)/scale+root.scrollLeft;
    const top=(box.top-container.top)/scale+root.scrollTop;
    const right=left+box.width/scale,bottom=top+box.height/scale;
    return {left,top,right,bottom,x:(left+right)/2,y:(top+bottom)/2};
  }
  function axisGap(source,target,direction) {
    if (direction.sign>0) return target[direction.start]-source[direction.end];
    return source[direction.start]-target[direction.end];
  }
  function inBeam(source,target,direction) {
    return target[direction.crossStart]<source[direction.crossEnd]-.5 &&
      target[direction.crossEnd]>source[direction.crossStart]+.5;
  }
  function crossDistance(target,direction,anchor) {
    return Math.max(target[direction.crossStart]-anchor,0,anchor-target[direction.crossEnd]);
  }
  function rankCandidate(control,root,source,direction,anchor) {
    const target=bounds(control,root);
    const gap=axisGap(source,target,direction);
    if (gap<-.5) return null;
    const cross=crossDistance(target,direction,anchor);
    return {control,aligned:inBeam(source,target,direction),gap:Math.max(0,gap),cross,
      distance:Math.hypot(Math.max(0,gap),cross),center:Math.abs(target[direction.cross]-anchor)};
  }
  function next(root,origin,key,candidates=controls(root)) {
    const direction=directions[key];
    if (!direction) return null;
    const source=bounds(origin,root),previous=memory.get(root);
    const continuing=previous?.control===origin && previous.axis===direction.axis;
    const anchor=continuing?previous.anchor:source[direction.cross];
    const ranked=candidates.filter(control=>control!==origin)
      .map(control=>rankCandidate(control,root,source,direction,anchor)).filter(Boolean);
    ranked.sort((a,b)=>a.distance-b.distance || Number(b.aligned)-Number(a.aligned) ||
      a.gap-b.gap || a.center-b.center);
    const target=ranked[0]?.control;
    if (!target) return null;
    memory.set(root,{control:target,axis:direction.axis,anchor});
    return target;
  }
  return {next,controls,reset:root=>memory.delete(root)};
})();
