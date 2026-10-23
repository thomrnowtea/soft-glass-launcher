'use strict';

/** Native buttons keep short press activation; a hold opens the contextual action once. */
function bindHold(button, action, enabled = () => true) {
  let timer = 0, held = false, key = '', suppressClick = false;
  const cancel = () => { clearTimeout(timer); timer = 0; };
  const begin = () => {
    cancel(); held = false; suppressClick = false;
    timer = setTimeout(() => {
      if (!button.isConnected || !enabled()) return;
      held = true; suppressClick = true; action();
    }, 500);
  };
  button.addEventListener('pointerdown', event => {
    if (event.button === 0 && enabled()) begin();
  });
  button.addEventListener('pointerup', cancel);
  button.addEventListener('pointercancel', cancel);
  button.addEventListener('pointerleave', cancel);
  button.addEventListener('blur', cancel);
  button.addEventListener('keydown', event => {
    if (!['Enter', ' '].includes(event.key) || !enabled()) return;
    event.preventDefault(); event.stopPropagation();
    if (!event.repeat) { key = event.key; begin(); }
  });
  button.addEventListener('keyup', event => {
    if (event.key !== key) return;
    event.preventDefault(); event.stopPropagation(); cancel(); key = '';
    if (!held) button.click();
    held = false; suppressClick = false;
  });
  button.addEventListener('click', event => {
    if (!suppressClick) return;
    event.preventDefault(); event.stopImmediatePropagation(); suppressClick = false;
  }, true);
  button.addEventListener('contextmenu', event => {
    event.preventDefault(); cancel();
    if (enabled() && !held) action();
  });
  button.addEventListener('keydown', event => {
    if (enabled() && (event.key === 'ContextMenu' || (event.shiftKey && event.key === 'F10'))) {
      event.preventDefault(); action();
    }
  });
}
