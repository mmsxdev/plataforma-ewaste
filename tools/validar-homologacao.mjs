// Ensaio real da homologação; executar somente contra a aplicação de demonstração isolada.
// Não usa SQL nem o banco compartilhado. Requer seeds e PI_DEMO_CONFIRM=FICTICIO.
import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
import {chromium} from 'playwright';

if (process.env.PI_DEMO_CONFIRM !== 'FICTICIO') throw new Error('Defina PI_DEMO_CONFIRM=FICTICIO apenas para o banco isolado de demonstração.');
const base = new URL(process.env.PI_APP_A || 'http://127.0.0.1:18080');
if (base.protocol !== 'http:' || !['127.0.0.1','localhost','[::1]'].includes(base.hostname)
    || !base.port || Number(base.port) === 0 || base.username || base.password || base.pathname !== '/' || base.search || base.hash)
  throw new Error('PI_APP_A deve ser uma origem HTTP local com porta explícita.');
const app = base.origin;
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const runId = process.env.PI_RUN_ID || 'homologacao-'+new Date().toISOString().replace(/[:.]/g,'-')+'-'+crypto.randomBytes(3).toString('hex');
if (!/^[a-zA-Z0-9][a-zA-Z0-9_-]{0,99}$/.test(runId)) throw new Error('PI_RUN_ID inválido.');
const out = path.join(root,'target/evidencias',runId);
await fs.mkdir(path.dirname(out),{recursive:true});
await fs.mkdir(out); // Não substituir qualquer evidência existente.
const tag = 'HOMO-'+Date.now()+'-'+crypto.randomBytes(2).toString('hex');
const company = 'Coletora Fictícia '+tag;
const email = tag.toLowerCase()+'@teste.invalid';
const password = 'Ficticia-PI-2026';
const reason = 'Revisão fictícia: reenviar a licença legível '+tag;
const expiry = new Date(Date.now()+365*86400000).toISOString().slice(0,10);
const pdf = await fs.readFile(path.join(root,'docs/evidencias/fechamento-20261124/cdf-ficticio.pdf'));
const sha = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
function cnpj() {
  let n=crypto.randomInt(10000000,99999999).toString()+'0001';
  for(const w of [[5,4,3,2,9,8,7,6,5,4,3,2],[6,5,4,3,2,9,8,7,6,5,4,3,2]]) {
    const r=[...n].reduce((a,c,i)=>a+Number(c)*w[i],0)%11;n+=r<2?0:11-r;
  }
  return n;
}
const options={headless:true};
if(process.env.PI_BROWSER_EXECUTABLE && process.env.PI_BROWSER_CHANNEL) throw new Error('Use PI_BROWSER_EXECUTABLE ou PI_BROWSER_CHANNEL, não ambos.');
if(process.env.PI_BROWSER_EXECUTABLE){await fs.access(process.env.PI_BROWSER_EXECUTABLE);options.executablePath=process.env.PI_BROWSER_EXECUTABLE;}
if(process.env.PI_BROWSER_CHANNEL){
  if(!['chrome','msedge'].includes(process.env.PI_BROWSER_CHANNEL))throw new Error('PI_BROWSER_CHANNEL inválido.');
  options.channel=process.env.PI_BROWSER_CHANNEL;
}
const results={started:new Date().toISOString(),run_id:runId,scope:'Navegador real, uma aplicação local de demonstração; não comprova duas máquinas ou VPS',app,node:process.version,tag,servicos_externos:'HTTPS externo bloqueado para exercitar coordenadas manuais sem CDN/CEP',validade_documento:expiry,pdf_fixture_sha256:sha(pdf),checks:[]};
let browser;
const contexts=[];
let step;
async function check(name,action){step=name;await action();results.checks.push({name,passed:true});console.log('OK',name);}
async function context(){const c=await browser.newContext({viewport:{width:1440,height:1000}});await c.route('https://**/*',route=>route.abort());contexts.push(c);return c;}
async function login(p,user,pass){
  await p.goto(app+'/login');await p.locator('#username').fill(user);await p.locator('#password').fill(pass);
  await p.locator('form[action="/login"] button').click();await p.waitForURL('**/painel');
}
async function manual(p){
  const details=p.locator('#localizacao-manual');
  if(!await details.evaluate(e=>e.open))await details.locator('summary').click();
}
async function shot(p,name){await p.screenshot({path:path.join(out,name),fullPage:true});}
async function upload(p,type,name){
  await p.goto(app+'/coletora/homologacao');await p.locator('#tipo').selectOption(type);
  await p.locator('#validade').fill(type==='LICENCA_AMBIENTAL'?expiry:'');
  await p.locator('#arquivo').setInputFiles({name,mimeType:'application/pdf',buffer:pdf});
  await p.getByRole('button',{name:'Enviar documento',exact:true}).click();
  await p.getByText('Documento enviado.',{exact:true}).waitFor();
}
async function submitForReview(p){
  await p.getByRole('button',{name:'Enviar para análise',exact:true}).click();
  await p.getByText('Documentos enviados para análise.',{exact:false}).waitFor();
  assert.match(await p.locator('body').innerText(),/Situação: em análise/);
}
try {
  browser=await chromium.launch(options);results.browser=browser.version();console.log('Evidências:',out);
  const cg=await context(),cc=await context(),ca=await context();
  const g=await cg.newPage(),c=await cc.newPage(),a=await ca.newPage();
  await check('cadastro de coletora fictícia por coordenadas manuais',async()=>{
    await c.goto(app+'/cadastro/coletora');await manual(c);
    const values={razaoSocial:company,cnpj:cnpj(),telefone:'11999990000',endereco:'Rua Fictícia, 100',cidade:'São Paulo',latitude:'-23.5629',longitude:'-46.6544',raioAtendimentoKm:'30',nome:'Responsável Fictício',email,senha:password};
    for(const [id,value] of Object.entries(values))await c.locator('#'+id).fill(value);
    await c.locator('#uf').selectOption('SP');await c.getByRole('button',{name:'Concluir cadastro',exact:true}).click();
    await c.waitForURL('**/login?cadastro');await login(c,email,password);await c.goto(app+'/coletora/homologacao');
    assert.match(await c.locator('body').innerText(),/Situação: pendente/);await shot(c,'01-coletora-pendente.png');
  });
  let id;
  await check('solicitação ABERTA criada antes da aprovação',async()=>{
    await login(g,'geradora@teste.com','senha123');await g.goto(app+'/geradora/solicitacoes/nova');await manual(g);
    const values={descricao:tag,pesoEstimadoKg:'10',endereco:'Rua Fictícia, 100',cidade:'São Paulo',uf:'SP',latitude:'-23.5629',longitude:'-46.6544','itens[0].identificacao':'Notebook fictício '+tag,'itens[0].quantidade':'1','itens[0].pesoKg':'10'};
    for(const [name,value] of Object.entries(values))await g.locator(`[name="${name}"]`).fill(value);
    await g.locator('[name="itens[0].tipoResiduo"]').selectOption('INFORMATICA');
    await g.getByRole('button',{name:'Criar solicitação',exact:true}).click();await g.waitForURL(/\/geradora\/solicitacoes\/\d+$/);
    id=Number(g.url().split('/').at(-1));results.solicitacao_id=id;assert.match(await g.locator('body').innerText(),/ABERTA/);
    await c.goto(app+'/coletora/oportunidades');assert.equal(await c.locator(`a[href="/coletora/oportunidades/${id}"]`).count(),0);
  });
  await check('UI e servidor recusam análise sem documentos obrigatórios',async()=>{
    await c.goto(app+'/coletora/homologacao');assert.equal(await c.getByRole('button',{name:'Enviar para análise',exact:true}).isDisabled(),true);
    const csrf=await c.locator('form[action="/coletora/homologacao/enviar"] input[name="_csrf"]').inputValue();
    const response=await cc.request.post(app+'/coletora/homologacao/enviar',{form:{_csrf:csrf},maxRedirects:0});
    assert.equal(response.status(),302);await c.goto(app+'/coletora/homologacao');
    assert.match(await c.locator('body').innerText(),/Envie antes:.*Licença ambiental.*Cadastro IBAMA/s);
    assert.match(await c.locator('body').innerText(),/Situação: pendente/);await shot(c,'02-rejeicao-documentos.png');
  });
  await check('upload dos dois PDFs obrigatórios e envio para análise',async()=>{
    await upload(c,'LICENCA_AMBIENTAL','licenca-ficticia-'+tag+'.pdf');await upload(c,'CADASTRO_IBAMA','ibama-ficticio-'+tag+'.pdf');
    await submitForReview(c);await shot(c,'03-em-analise.png');
  });
  let homologacaoId;
  await check('ADMIN reprova com motivo e coletora visualiza a decisão',async()=>{
    await login(a,'admin@teste.com','senha123');await a.goto(app+'/admin/homologacoes');
    const card=a.locator('section.card').filter({has:a.getByRole('heading',{name:company,exact:true})});
    const action=await card.locator('form[action$="/reprovar"]').getAttribute('action');
    homologacaoId=Number(action.match(/\/homologacoes\/(\d+)\/reprovar$/)[1]);results.homologacao_id=homologacaoId;
    await card.locator('input[name="motivo"]').fill(reason);await card.getByRole('button',{name:'Reprovar',exact:true}).click();
    await a.getByText('Homologação reprovada.',{exact:false}).waitFor();
    await c.goto(app+'/coletora/homologacao');assert.match(await c.locator('body').innerText(),/Situação: reprovada/);
    assert.ok((await c.locator('body').innerText()).includes(reason));await shot(c,'04-reprovacao.png');
    await c.goto(app+'/coletora/oportunidades');assert.equal(await c.locator(`a[href="/coletora/oportunidades/${id}"]`).count(),0);
  });
  await check('coletora corrige documento e reenvia para análise',async()=>{
    await upload(c,'LICENCA_AMBIENTAL','licenca-corrigida-'+tag+'.pdf');await submitForReview(c);
  });
  await check('ADMIN aprova a coletora com documentos em dia',async()=>{
    await a.goto(app+'/admin/homologacoes');const card=a.locator('section.card').filter({has:a.getByRole('heading',{name:company,exact:true})});
    await shot(a,'05-admin-aprovacao.png');await card.getByRole('button',{name:'Aprovar',exact:true}).click();
    await a.getByText('Coletora aprovada.',{exact:false}).waitFor();
    await c.goto(app+'/coletora/homologacao');assert.match(await c.locator('body').innerText(),/Situação: aprovada/);
    assert.match(await c.locator('body').innerText(),/Sua coletora está apta/);
  });
  await check('solicitação antiga chega retroativamente às oportunidades da coletora aprovada',async()=>{
    await c.goto(app+'/coletora/oportunidades');const link=c.locator(`a[href="/coletora/oportunidades/${id}"]`);
    await link.waitFor();assert.equal(await link.count(),1);await shot(c,'06-alerta-retroativo.png');
    await link.click();assert.ok((await c.locator('body').innerText()).includes(tag));
  });
  let ownHref;
  await check('coletora baixa o próprio PDF com bytes íntegros',async()=>{
    await c.goto(app+'/coletora/homologacao');ownHref=await c.getByRole('link',{name:'licenca-corrigida-'+tag+'.pdf',exact:true}).getAttribute('href');
    const response=await cc.request.get(app+ownHref);assert.equal(response.status(),200);const bytes=await response.body();
    assert.equal(sha(bytes),sha(pdf));assert.equal(bytes.subarray(0,4).toString(),'%PDF');results.pdf_recuperado_sha256=sha(bytes);
    await fs.writeFile(path.join(out,'documento-ficticio-recuperado.pdf'),bytes);await shot(c,'07-documentos-proprios.png');
  });
  await check('outra coletora não consegue baixar o documento',async()=>{
    await g.goto(app+'/painel');await g.getByRole('button',{name:'Sair',exact:true}).click();await login(g,'coletora@teste.com','senha123');
    const response=await cg.request.get(app+ownHref);results.download_outra_coletora_status=response.status();
    assert.ok([400,403,404].includes(response.status()),'Esperado 400/403/404 ao negar documento, recebido '+response.status());
    assert.notEqual(sha(await response.body()),sha(pdf));
  });
  results.passed=true;
} catch(error) {
  results.passed=false;results.failed_check=step;results.error=String(error);
  for(let i=0;i<contexts.length;i++)for(const [j,p] of contexts[i].pages().entries())await shot(p,`falha-${i}-${j}.png`).catch(()=>{});
  throw error;
} finally {
  results.finished=new Date().toISOString();await fs.writeFile(path.join(out,'resultado.json'),JSON.stringify(results,null,2));await browser?.close();
}
