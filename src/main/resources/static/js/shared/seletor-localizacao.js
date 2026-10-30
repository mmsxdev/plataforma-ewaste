/* Contrato: #mapa-localizacao, #latitude, #longitude; opcional #localizacao-status.
   Carrega Leaflet apenas nas telas que usam o seletor; coordenadas manuais sempre disponíveis. */
(() => {
  'use strict';
  const container = document.getElementById('mapa-localizacao');
  const latitude = document.getElementById('latitude');
  const longitude = document.getElementById('longitude');
  if (!container || !latitude || !longitude) return;
  const status = document.getElementById('localizacao-status');
  const informar = (texto) => { if (status) status.textContent = texto; };
  const iniciar = () => {
    const coordenadas = () => {
      const lat = Number(latitude.value), lng = Number(longitude.value);
      return latitude.value !== '' && longitude.value !== '' && Number.isFinite(lat) && Number.isFinite(lng)
        && Math.abs(lat) <= 90 && Math.abs(lng) <= 180 ? [lat, lng] : null;
    };
    const mapa = L.map(container).setView(coordenadas() || [-14.2, -51.9], coordenadas() ? 13 : 4);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors', maxZoom: 19
    }).addTo(mapa);
    let marcador;
    const marcar = (ponto) => {
      if (marcador) marcador.setLatLng(ponto); else marcador = L.marker(ponto).addTo(mapa);
    };
    if (coordenadas()) marcar(coordenadas());
    mapa.on('click', ({ latlng }) => {
      latitude.value = latlng.lat.toFixed(6); longitude.value = latlng.lng.toFixed(6);
      marcar(latlng); informar('Localização selecionada. Confira o endereço informado.');
    });
    [latitude, longitude].forEach((campo) => campo.addEventListener('change', () => {
      const ponto = coordenadas(); if (ponto) { marcar(ponto); mapa.setView(ponto, 13); }
    }));
  };
  if (window.L) { iniciar(); return; }
  const estilo = document.createElement('link');
  estilo.rel = 'stylesheet'; estilo.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
  document.head.appendChild(estilo);
  const script = document.createElement('script');
  script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
  script.onload = iniciar;
  script.onerror = () => informar('Mapa indisponível. Preencha as coordenadas manualmente.');
  document.head.appendChild(script);
})();
