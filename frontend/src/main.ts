import './style.css';
import { iniciarApp } from './app';

iniciarApp().catch((error: unknown) => {
  const mensaje = error instanceof Error ? error.message : 'Error desconocido al iniciar.';
  const fatal = document.getElementById('fatal');
  if (fatal) {
    fatal.hidden = false;
    fatal.textContent = `No se pudo iniciar la app: ${mensaje}`;
  }
});
