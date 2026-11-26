import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import vm from 'node:vm';

const source = await fs.readFile(new URL('../../src/main/resources/static/js/shared/seletor-localizacao.js', import.meta.url), 'utf8');
const tick = () => new Promise(resolve => setImmediate(resolve));
class Element {
  value = ''; textContent = ''; dataset = {}; disabled = false; open = true; type = 'number';
  handlers = new Map();
  addEventListener(type, listener) { const listeners = this.handlers.get(type) || []; listeners.push(listener); this.handlers.set(type, listeners); }
  emit(type, event = {}) { for (const listener of this.handlers.get(type) || []) listener(event); }
  querySelectorAll() { return []; }
  focus() { this.focused = true; }
  scrollIntoView() {}
}
function setup() {
  const ids = ['mapa-localizacao','latitude','longitude','localizacao-status','localizacao-manual','cep','cep-buscar','endereco','cidade','uf'];
  const nodes = Object.fromEntries(ids.map(id => [id,new Element()]));
  const form = new Element(); nodes.latitude.form = form;
  const inserted = [], requests = [];
  vm.runInNewContext(source, {
    document: {getElementById: id => nodes[id], createElement: () => ({}), head: {appendChild: element => inserted.push(element)}},
    window: {}, URL, AbortController,
    fetch: (url, options = {}) => new Promise((resolve,reject) => requests.push({url:String(url),signal:options.signal,resolve,reject})),
  });
  return {
    nodes,form,requests,inserted,
    changeCep(value) { nodes.cep.value = value; nodes.cep.emit('input'); },
    async answer(index, body) { requests[index].resolve({ok:true,json:async () => body}); await tick(); },
  };
}
const address = (street, city) => ({logradouro:street,bairro:'Centro',localidade:city,uf:'GO'});

test('resposta antiga do ViaCEP não sobrescreve o CEP mais recente, mesmo se fetch ignorar abort', async () => {
  const h = setup(); h.changeCep('11111111'); h.changeCep('22222222');
  assert.equal(h.requests[0].signal.aborted,true);
  await h.answer(1,address('Rua B','Cidade B'));
  await h.answer(2,[{lat:'-16',lon:'-49'}]);
  await h.answer(0,address('Rua A','Cidade A'));
  assert.equal(h.nodes.cidade.value,'Cidade B');
  assert.equal(h.nodes.endereco.value,'Rua B, Centro');
  assert.equal(h.nodes.latitude.value,'-16.000000');
  assert.equal(h.requests.length,3);
});

test('geocodificação atrasada não substitui coordenadas da segunda busca', async () => {
  const h = setup(); h.changeCep('11111111'); await h.answer(0,address('Rua A','Cidade A'));
  h.changeCep('22222222'); await h.answer(2,address('Rua B','Cidade B'));
  await h.answer(3,[{lat:'-17',lon:'-48'}]);
  await h.answer(1,[{lat:'-20',lon:'-50'}]);
  assert.equal(h.requests[1].signal.aborted,true);
  assert.equal(h.nodes.latitude.value,'-17.000000');
  assert.equal(h.nodes.longitude.value,'-48.000000');
  assert.equal(h.nodes.cidade.value,'Cidade B');
});

test('fim de busca obsoleta não habilita botão enquanto a atual está em andamento', async () => {
  const h = setup(); h.changeCep('11111111'); h.changeCep('22222222');
  await h.answer(0,address('Rua A','Cidade A'));
  assert.equal(h.nodes['cep-buscar'].disabled,true);
  await h.answer(1,address('Rua B','Cidade B')); await h.answer(2,[{lat:'-17',lon:'-48'}]);
  assert.equal(h.nodes['cep-buscar'].disabled,false);
});

test('edição parcial do CEP cancela a resposta pendente e invalida coordenadas anteriores', async () => {
  const h = setup(); h.nodes.latitude.value='-16';h.nodes.longitude.value='-49';
  h.changeCep('11111111');h.changeCep('111');
  await h.answer(0,address('Rua A','Cidade A'));
  assert.equal(h.nodes.latitude.value,'');assert.equal(h.nodes.longitude.value,'');
  assert.equal(h.nodes.cidade.value,'');assert.equal(h.requests.length,1);
  assert.equal(h.nodes['cep-buscar'].disabled,false);
});

test('entrada manual cancela geocodificação pendente e preserva o ponto escolhido', async () => {
  const h = setup();h.changeCep('11111111');await h.answer(0,address('Rua A','Cidade A'));
  h.nodes.latitude.value='-12.34';h.nodes.latitude.emit('input');
  h.nodes.longitude.value='-45.67';h.nodes.longitude.emit('input');
  await h.answer(1,[{lat:'-16',lon:'-49'}]);
  assert.equal(h.nodes.latitude.value,'-12.34');assert.equal(h.nodes.longitude.value,'-45.67');
});

test('falha externa abre alternativa manual e permite submeter coordenadas válidas sem mapa', async () => {
  const h = setup();h.changeCep('11111111');h.requests[0].reject(new TypeError('offline'));await tick();
  assert.equal(h.nodes['localizacao-manual'].open,true);
  assert.match(h.nodes['localizacao-status'].textContent,/manualmente/);
  assert.equal(h.nodes['cep-buscar'].disabled,false);
  h.nodes.latitude.value='0';h.nodes.longitude.value='0';
  let blocked=false;h.form.emit('submit',{preventDefault:()=>{blocked=true;}});
  assert.equal(blocked,false);
});

test('falha no CDN do mapa abre campos manuais e validação abre os detalhes antes de focar', () => {
  const h = setup();assert.equal(h.nodes['localizacao-manual'].open,false);
  h.inserted.at(-1).onerror();assert.equal(h.nodes['localizacao-manual'].open,true);
  h.nodes['localizacao-manual'].open=false;h.nodes.latitude.emit('invalid');
  assert.equal(h.nodes['localizacao-manual'].open,true);
  let blocked=false;h.form.emit('submit',{preventDefault:()=>{blocked=true;}});
  assert.equal(blocked,true);assert.equal(h.nodes.latitude.focused,true);
});
