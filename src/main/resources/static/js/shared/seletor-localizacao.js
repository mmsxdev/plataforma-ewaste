/* Contrato: #mapa-localizacao, #latitude, #longitude; opcional #localizacao-status.
   Opcional: #cep e #cep-buscar localizam a rua (ViaCEP + Nominatim/OpenStreetMap) e
   preenchem #endereco, #cidade e #uf quando existirem. O CEP não é enviado ao servidor.
   Opcional: #raioAtendimentoKm (km) desenha o perímetro de atendimento em volta do ponto.
   Teclado: com o mapa em foco, as setas e +/- movem o mapa (Leaflet) e Enter/Espaço marca o centro.
   Carrega Leaflet apenas nas telas que usam o seletor; coordenadas manuais sempre disponíveis. */
(() => {
  'use strict';
  const container = document.getElementById('mapa-localizacao');
  const latitude = document.getElementById('latitude');
  const longitude = document.getElementById('longitude');
  if (!container || !latitude || !longitude) return;
  const status = document.getElementById('localizacao-status');
  const informar = (texto) => { if (status) status.textContent = texto; };
  // Substituído por iniciar() quando o mapa carrega; sem mapa, só preenche os campos.
  let posicionar = () => {};
  const definirCoordenadas = (lat, lng) => {
    latitude.value = Number(lat).toFixed(6); longitude.value = Number(lng).toFixed(6);
  };

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
    // Opcional: #raioAtendimentoKm desenha o perímetro atendido em volta do ponto.
    const raio = document.getElementById('raioAtendimentoKm');
    let circulo;
    const atualizarRaio = (enquadrar) => {
      if (!raio) return;
      const centro = coordenadas(), km = Number(raio.value);
      if (!centro || !(km > 0)) { if (circulo) { circulo.remove(); circulo = null; } return; }
      if (circulo) circulo.setLatLng(centro).setRadius(km * 1000);
      else circulo = L.circle(centro, { radius: km * 1000, color: '#17634c', weight: 2, fillColor: '#17634c', fillOpacity: 0.12, interactive: false }).addTo(mapa);
      if (enquadrar) mapa.fitBounds(circulo.getBounds(), { padding: [16, 16] });
    };
    let marcador;
    const marcar = (ponto) => {
      if (marcador) marcador.setLatLng(ponto); else marcador = L.marker(ponto).addTo(mapa);
      atualizarRaio(false);
    };
    if (coordenadas()) marcar(coordenadas());
    if (raio) {
      raio.addEventListener('input', () => {
        atualizarRaio(true);
        if (!coordenadas() && Number(raio.value) > 0) informar('Marque a localização (CEP ou clique no mapa) para ver o raio de atendimento.');
      });
      if (circulo) atualizarRaio(true);
    }
    mapa.on('click', ({ latlng }) => {
      definirCoordenadas(latlng.lat, latlng.lng);
      marcar(latlng); informar('Localização selecionada. Confira o endereço informado.');
    });
    // Alternativa ao clique para quem usa só o teclado (WCAG 2.1.1); ignora os botões de zoom.
    container.addEventListener('keydown', (evento) => {
      if (evento.target !== container || (evento.key !== 'Enter' && evento.key !== ' ')) return;
      evento.preventDefault();
      const centro = mapa.getCenter();
      definirCoordenadas(centro.lat, centro.lng);
      marcar(centro); informar('Localização marcada no centro do mapa. Confira o endereço informado.');
    });
    [latitude, longitude].forEach((campo) => campo.addEventListener('change', () => {
      const ponto = coordenadas(); if (ponto) { marcar(ponto); mapa.setView(ponto, 13); }
    }));
    posicionar = (ponto, zoom) => { marcar(ponto); mapa.setView(ponto, zoom); };
  };

  // --- Localização pelo CEP ---
  const cep = document.getElementById('cep');
  const botaoCep = document.getElementById('cep-buscar');
  if (cep && botaoCep) {
    const endereco = document.getElementById('endereco');
    const cidade = document.getElementById('cidade');
    const uf = document.getElementById('uf');
    let ultimoCep = '';
    const digitos = () => cep.value.replace(/\D/g, '').slice(0, 8);

    const geocodificar = async (parametros) => {
      const url = new URL('https://nominatim.openstreetmap.org/search');
      Object.entries({ format: 'jsonv2', limit: '1', countrycodes: 'br', 'accept-language': 'pt-BR', ...parametros })
        .forEach(([chave, valor]) => url.searchParams.set(chave, valor));
      const resposta = await fetch(url, { headers: { Accept: 'application/json' } });
      if (!resposta.ok) return null;
      const [primeiro] = await resposta.json();
      return primeiro ? [Number(primeiro.lat), Number(primeiro.lon)] : null;
    };

    const buscar = async () => {
      const numero = digitos();
      if (numero.length !== 8) { informar('Informe um CEP com 8 dígitos.'); cep.focus(); return; }
      ultimoCep = numero; botaoCep.disabled = true;
      informar('Buscando o CEP…');
      try {
        const resposta = await fetch(`https://viacep.com.br/ws/${numero}/json/`);
        const dados = resposta.ok ? await resposta.json() : { erro: true };
        if (dados.erro) { informar('CEP não encontrado. Confira o número ou marque o ponto direto no mapa.'); return; }
        const rua = [dados.logradouro, dados.bairro].filter(Boolean).join(', ');
        // Só substitui o endereço se estiver vazio ou se foi preenchido por uma busca anterior.
        if (endereco && rua && (!endereco.value.trim() || endereco.dataset.viaCep === endereco.value)) {
          endereco.value = rua; endereco.dataset.viaCep = rua;
        }
        if (cidade && dados.localidade) cidade.value = dados.localidade;
        if (uf && dados.uf) uf.value = dados.uf;

        informar('Localizando a rua no mapa…');
        const local = { city: dados.localidade, state: dados.uf };
        let ponto = dados.logradouro ? await geocodificar({ ...local, street: dados.logradouro }) : null;
        let zoom = 17;
        if (!ponto) ponto = await geocodificar({ postalcode: `${numero.slice(0, 5)}-${numero.slice(5)}` });
        if (!ponto) { ponto = await geocodificar(local); zoom = 13; }
        if (!ponto) { informar('Endereço preenchido, mas a rua não foi encontrada no mapa. Marque o ponto manualmente.'); return; }
        definirCoordenadas(ponto[0], ponto[1]);
        posicionar(ponto, zoom);
        informar(zoom === 17
          ? 'Rua localizada. Complete o número do endereço e clique no mapa sobre o ponto exato, se necessário.'
          : 'Rua não encontrada no mapa; mostrando a cidade. Clique no mapa sobre o ponto exato.');
        if (endereco && !/\d/.test(endereco.value)) endereco.focus();
      } catch (erro) {
        informar('Não foi possível consultar o CEP agora. Preencha o endereço e marque o ponto no mapa.');
      } finally {
        botaoCep.disabled = false;
      }
    };

    botaoCep.addEventListener('click', buscar);
    cep.addEventListener('keydown', (evento) => {
      if (evento.key === 'Enter') { evento.preventDefault(); buscar(); }
    });
    cep.addEventListener('input', () => {
      const numero = digitos();
      cep.value = numero.length > 5 ? `${numero.slice(0, 5)}-${numero.slice(5)}` : numero;
      if (numero.length === 8 && numero !== ultimoCep) buscar();
    });
  }

  // Coordenadas podem ficar ocultas: o navegador não valida "required" em campos hidden.
  if (latitude.type === 'hidden' && latitude.form) {
    latitude.form.addEventListener('submit', (evento) => {
      if (latitude.value !== '' && longitude.value !== '') return;
      evento.preventDefault();
      informar('Marque a localização: informe o CEP ou clique no mapa sobre o endereço.');
      if (status) status.scrollIntoView({ behavior: 'smooth', block: 'center' });
      (cep || container).focus();
    });
  }

  if (window.L) { iniciar(); return; }
  const estilo = document.createElement('link');
  estilo.rel = 'stylesheet'; estilo.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
  document.head.appendChild(estilo);
  const script = document.createElement('script');
  script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
  script.onload = iniciar;
  script.onerror = () => informar(latitude.type === 'hidden'
    ? 'Mapa indisponível. Use o CEP para localizar o endereço.'
    : 'Mapa indisponível. Preencha as coordenadas manualmente.');
  document.head.appendChild(script);
})();
