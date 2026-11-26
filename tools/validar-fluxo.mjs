// Ensaio com dados inteiramente fictícios em DUAS aplicações locais, não em duas máquinas.
// Requer banco de demonstração isolado com seeds e duas apps profile team.
// Bootstrap: npm ci --prefix tools; npm run browser:install --prefix tools.
import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
import {chromium} from 'playwright';
import {scheduleForDemo, validateDemoUrls} from './demo-config.mjs';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
if (process.env.PI_DEMO_CONFIRM !== 'FICTICIO') throw new Error('Defina PI_DEMO_CONFIRM=FICTICIO somente para instâncias ligadas ao banco isolado de demonstração.');
const runId=process.env.PI_RUN_ID || new Date().toISOString().replace(/[:.]/g,'-')+'-'+crypto.randomBytes(3).toString('hex');
if (!/^[a-zA-Z0-9][a-zA-Z0-9_-]{0,99}$/.test(runId)) throw new Error('PI_RUN_ID inválido.');
const out=path.join(root,'target/evidencias',runId);
await fs.mkdir(path.dirname(out),{recursive:true});
await fs.mkdir(out); // Falha se já existe: nunca sobrescrever evidências anteriores.
const schedule=scheduleForDemo(process.env.PI_AGENDAMENTO);
const A=process.env.PI_APP_A || 'http://127.0.0.1:18080';
const B=process.env.PI_APP_B || 'http://127.0.0.1:18081';
validateDemoUrls(A,B);
const tag='PI-'+Date.now();
const email=tag.toLowerCase()+'@teste.invalid';
const password='Ficticia-PI-2026';
const cnpj=()=>{let n=String(Date.now()).slice(-8)+'0001';for(const w of [[5,4,3,2,9,8,7,6,5,4,3,2],[6,5,4,3,2,9,8,7,6,5,4,3,2]]){let r=[...n].reduce((a,c,i)=>a+Number(c)*w[i],0)%11;n+=r<2?0:11-r;}return n;};
const browserOptions={headless:true};
if (process.env.PI_BROWSER_EXECUTABLE && process.env.PI_BROWSER_CHANNEL) throw new Error('Use somente PI_BROWSER_EXECUTABLE ou PI_BROWSER_CHANNEL.');
if (process.env.PI_BROWSER_EXECUTABLE) { await fs.access(process.env.PI_BROWSER_EXECUTABLE); browserOptions.executablePath=process.env.PI_BROWSER_EXECUTABLE; }
if (process.env.PI_BROWSER_CHANNEL) {
  if (!['chrome','msedge'].includes(process.env.PI_BROWSER_CHANNEL)) throw new Error('PI_BROWSER_CHANNEL deve ser chrome ou msedge.');
  browserOptions.channel=process.env.PI_BROWSER_CHANNEL;
}
let browser;
const results={started:new Date().toISOString(),scope:'Duas JVMs locais no mesmo computador; teste entre duas máquinas permanece pendente',run_id:runId,tag,apps:{A,B},schedule,node:process.version,checks:[]};
const contexts=[];
async function context(){let c=await browser.newContext({viewport:{width:1440,height:1000}});contexts.push(c);return c;}
async function login(p,base,user,pass){await p.goto(base+'/login');await p.locator('#username').fill(user);await p.locator('#password').fill(pass);await p.locator('form[action="/login"] button').click();await p.waitForURL('**/painel');}
async function check(name,action){await action();results.checks.push({name,passed:true});console.log('OK',name);}
try{
 browser=await chromium.launch(browserOptions);
 results.browser=browser.version();
 console.log('Evidências:',out);
 const ca=await context(),cb=await context(),cc=await context();
 const a=await ca.newPage(),b=await cb.newPage(),c=await cc.newPage();
 await check('home e identidade visíveis',async()=>{await a.goto(A);assert.match(await a.locator('body').innerText(),/Missão/);await a.screenshot({path:path.join(out,'01-home.png'),fullPage:true});});
 await check('cadastro fictício pela interface',async()=>{
   await a.goto(A+'/cadastro/geradora');
   for(const [id,value] of Object.entries({razaoSocial:'Empresa Fictícia '+tag,cnpj:cnpj(),telefone:'62999990000',endereco:'Rua Fictícia de Ensaio, 100',cidade:'São Paulo',latitude:'-23.5629',longitude:'-46.6544',nome:'Responsável Fictício',email,senha:password})) { const campo=a.locator('#'+id); if(await campo.getAttribute('type')==='hidden') await campo.evaluate((e,v)=>{e.value=v;},value); else await campo.fill(value); }
   await a.locator('#uf').selectOption('SP');await a.getByRole('button',{name:'Concluir cadastro'}).click();await a.waitForURL('**/login?cadastro');await login(a,A,email,password);
 });
 await check('consulta banco → AVL → lista e categoria',async()=>{await a.goto(A+'/mapa?tipo=INFORMATICA');assert.match(await a.locator('body').innerText(),/Escola Circular/);await a.screenshot({path:path.join(out,'02-pontos.png'),fullPage:true});});
 const formHref=await a.getByRole('link',{name:'Registrar solicitação para este ponto'}).first().getAttribute('href');
 async function fillForm(type){await a.goto(A+formHref);for(const [name,value] of Object.entries({descricao:tag,pesoEstimadoKg:'10',endereco:'Rua Fictícia, 100',cidade:'São Paulo',uf:'SP',latitude:'-23.5629',longitude:'-46.6544','itens[0].identificacao':'Notebook fictício '+tag,'itens[0].quantidade':'1','itens[0].pesoKg':'10'})){const campo=a.locator(`[name="${name}"]`);if(await campo.getAttribute('type')==='hidden')await campo.evaluate((e,v)=>{e.value=v;},value);else await campo.fill(value);}await a.locator('[name="itens[0].tipoResiduo"]').selectOption(type);}
 await check('rejeição de material incompatível pelo servidor',async()=>{await fillForm('BATERIA');await a.getByRole('button',{name:'Criar solicitação'}).click();assert.match(await a.locator('body').innerText(),/não recebe todos os tipos/);await a.screenshot({path:path.join(out,'03-rejeicao.png'),fullPage:true});});
 let id;
 await check('solicitação válida e persistência',async()=>{await fillForm('INFORMATICA');await a.getByRole('button',{name:'Criar solicitação'}).click();await a.waitForURL(/\/geradora\/solicitacoes\/\d+$/);id=Number(a.url().split('/').at(-1));results.solicitacao_id=id;assert.match(await a.locator('body').innerText(),new RegExp(tag));});
 await check('registro criado em A aparece em B',async()=>{await login(b,B,email,password);await b.goto(B+'/geradora/solicitacoes/'+id);assert.match(await b.locator('body').innerText(),new RegExp(tag));await b.screenshot({path:path.join(out,'04-outra-instancia.png'),fullPage:true});});
 await check('edição autorizada enquanto aberta',async()=>{await a.goto(A+`/geradora/solicitacoes/${id}/editar`);await a.locator('#descricao').fill(tag+' atualizado');await a.getByRole('button',{name:'Salvar alterações'}).click();await b.reload();assert.match(await b.locator('body').innerText(),/atualizado/);});
 await check('coletora aceita e agenda pelo navegador',async()=>{
   await login(c,A,'coletora@teste.com','senha123');await c.goto(A+`/coletora/oportunidades/${id}`);await c.getByRole('button',{name:'Aceitar esta coleta'}).click();
   await c.locator('#agendadaPara').fill(schedule.value);await c.getByRole('button',{name:'Salvar agendamento'}).click();assert.match(await c.locator('body').innerText(),/AGENDADA/);
 });
 await check('edição rejeitada após aceite',async()=>{const r=await a.goto(A+`/geradora/solicitacoes/${id}/editar`);assert.equal(r.status(),409);});
 const photo=path.join(root,'src/main/resources/static/img/logo-ewaste.png');
 await check('upload em A recuperado em B',async()=>{
   await c.goto(A+`/coletora/coletas/${id}`);await c.locator('#pesoRealKg').fill('9.50');await c.locator('#fotoColeta').setInputFiles(photo);await c.getByRole('button',{name:'Registrar coleta',exact:true}).click();
   const photoHref=await c.getByRole('link',{name:'Foto',exact:true}).first().getAttribute('href');
   const cd=await context(),d=await cd.newPage();await login(d,B,'coletora@teste.com','senha123');const response=await cd.request.get(B+photoHref);assert.equal(response.status(),200);assert.equal(crypto.createHash('sha256').update(await response.body()).digest('hex'),crypto.createHash('sha256').update(await fs.readFile(photo)).digest('hex'));
 });
 await check('destinação via procedure e PDFs recuperados em B',async()=>{
   await c.locator('#destinacao').fill('Reciclagem fictícia em ensaio acadêmico');await c.getByRole('button',{name:'Registrar destinação'}).click();await c.getByRole('button',{name:'Concluir e emitir documentos'}).click();
   await b.goto(B+'/geradora/documentos');const row=b.locator('tr').filter({has:b.locator('td').filter({hasText:'CDF-'+id})}).first();const pdfHref=await row.getByRole('link',{name:'Baixar PDF'}).getAttribute('href');const response=await cb.request.get(B+pdfHref);assert.equal(response.status(),200);const bytes=await response.body();assert.equal(bytes.subarray(0,4).toString(),'%PDF');results.pdf_sha256=crypto.createHash('sha256').update(bytes).digest('hex');await fs.writeFile(path.join(out,'cdf-ficticio.pdf'),bytes);await b.screenshot({path:path.join(out,'05-documentos.png'),fullPage:true});
 });
 await check('interface móvel e navegação por teclado',async()=>{await a.setViewportSize({width:390,height:844});await a.goto(A+'/mapa');await a.keyboard.press('Tab');assert.ok(await a.locator(':focus').count());await a.screenshot({path:path.join(out,'06-mobile.png'),fullPage:true});});
 results.passed=true;
}catch(e){results.passed=false;results.error=String(e);for(const c of contexts){const p=c.pages()[0];if(p)await p.screenshot({path:path.join(out,'falha-'+contexts.indexOf(c)+'.png'),fullPage:true}).catch(()=>{});}throw e;
}finally{results.finished=new Date().toISOString();await fs.writeFile(path.join(out,'resultado.json'),JSON.stringify(results,null,2));await browser?.close();}
