// Executar com Node do runtime de artefatos; resolução via target/academic/node_modules.
import fs from 'node:fs/promises';
import path from 'node:path';
import { createRequire } from 'node:module';
import { pathToFileURL } from 'node:url';
const root = process.cwd();
const req = createRequire(path.join(root, 'target/academic/loader.cjs'));
const { Workbook, SpreadsheetFile, FileBlob } = await import(pathToFileURL(req.resolve('@oai/artifact-tool')).href);
// Edição pontual: preserva a planilha existente e atualiza somente a taxa DEV01.
if (process.argv.includes('--atualizar')) {
 const file=path.join(root,'docs/custos.xlsx');
 const wb=await SpreadsheetFile.importXlsx(await FileBlob.load(file));
 const s=wb.worksheets.getItem('Custos');
 const csv=await fs.readFile(path.join(root,'docs/custos-premissas.csv'),'utf8');
 const input=csv.trim().split(/\r?\n/).find(line=>line.startsWith('DEV01;')).split(';');
 if(s.getRange('A7').values[0][0]!=='DEV01') throw new Error('Posição DEV01 divergente; edição interrompida.');
 s.getRange('G7').values=[[Number(input[6])]];
 wb.recalculate();
 console.log(JSON.stringify(await wb.inspect({kind:'region',sheetId:'Custos',range:'D18:H27',maxChars:2200,tableMaxRows:10,tableMaxCols:5})));
 const base=Number(input[4])*Number(input[6]);
 const reserve=base*s.getRange('H4').values[0][0];
 const expected={H20:base,H21:reserve,H22:base+reserve,H25:120,H26:360,H27:base+reserve+360};
 for(const [cell,value] of Object.entries(expected)) if(s.getRange(cell).values[0][0]!==value) throw new Error('Total divergente: '+cell);
 const errors=await wb.inspect({kind:'match',searchTerm:'#REF!|#DIV/0!|#VALUE!|#NAME\\?|#N/A|#NUM!|#NULL!',options:{useRegex:true,maxResults:10},summary:'Erros de fórmula'});
 console.log(errors.ndjson);
 const preview=await wb.render({sheetName:'Custos',range:'A1:I30',scale:1.3,format:'png'});
 await fs.writeFile(path.join(root,'target/academic/custos-preview.png'),new Uint8Array(await preview.arrayBuffer()));
 await (await SpreadsheetFile.exportXlsx(wb)).save(file);
 await fs.rename(file+'.inspect.ndjson',path.join(root,'target/academic/custos-inspecao-'+Date.now()+'.ndjson')).catch(error=>{if(error.code!=='ENOENT')throw error;});
 process.exit(0);
}
const wb = Workbook.create();
const s = wb.worksheets.add('Custos');
s.showGridLines = false;
s.getRange('A1:I1').merge();
s.getRange('A1').values = [['E-Waste | estimativas de desenvolvimento e operação']];
s.getRange('A1:I1').format.fill = '#164E43';
s.getRange('A1:I1').format.font = {name:'Arial',size:16,bold:true,color:'#FFFFFF'};
s.getRange('A2:I2').merge();
s.getRange('A2').values = [['ESTIMATIVAS DIDÁTICAS • não representam despesas pagas, salários ou dedicação confirmada']];
s.getRange('D3:G3').merge();s.getRange('D4:G4').merge();
s.getRange('D3').values=[['Meses de operação']];s.getRange('H3').values=[[3]];
s.getRange('D4').values=[['Contingência sobre desenvolvimento']];s.getRange('H4').values=[[0.1]];
s.getRange('H4').setNumberFormat('0%');
s.getRange('A6:I6').values = [['ID','Fase','Natureza','Recurso','Quantidade','Unidade','R$/unidade','Total R$','Premissa / fonte']];
const csv = await fs.readFile(path.join(root,'docs/custos-premissas.csv'),'utf8');
const lines = csv.trim().split(/\r?\n/).slice(1).filter(Boolean).map(l=>l.split(';'));
const rows = lines.map(r=>[r[0],r[1],r[2],r[3],Number(r[4]),r[5],Number(r[6]),null,r[8]]);
s.getRange(`A7:I${6+rows.length}`).values = rows;
for(let i=7;i<7+rows.length;i++) s.getRange(`H${i}`).formulas = [[`=E${i}*G${i}`]];
const start=18;
const summary=[
 ['Desenvolvimento fixo','=SUMIFS(H7:H13,B7:B13,"desenvolvimento",C7:C13,"fixo")'],
 ['Desenvolvimento variável','=SUMIFS(H7:H13,B7:B13,"desenvolvimento",C7:C13,"variavel")'],
 ['Desenvolvimento base','=SUM(H18:H19)'],
 ['Contingência de desenvolvimento','=H20*$H$4'],
 ['Desenvolvimento com reserva','=SUM(H20:H21)'],
 ['Operação fixa / mês','=SUMIFS(H7:H13,B7:B13,"operacao",C7:C13,"fixo")'],
 ['Operação variável / mês','=SUMIFS(H7:H13,B7:B13,"operacao",C7:C13,"variavel")'],
 ['Operação total / mês','=SUM(H23:H24)'],
 ['Operação no horizonte','=H25*$H$3'],
 ['Total no horizonte','=SUM(H22,H26)']
];
for(let i=0;i<summary.length;i++){
 s.getRange(`D${start+i}:G${start+i}`).merge();
 s.getRange(`D${start+i}`).values=[[summary[i][0]]];
 s.getRange(`H${start+i}`).formulas=[[summary[i][1]]];
}
s.getRange('A29:I29').merge();
s.getRange('A29').values=[['Fonte: docs/custos-premissas.csv. Quantidades e valores são premissas editáveis; fórmulas em preto.']];
s.getRange('A30:I30').merge();
s.getRange('A30').values=[['Equipamentos existentes: custo incremental considerado zero; depreciação e custo econômico não estimados.']];
s.getRange('A1:I30').format.font.name='Arial';
s.getRange('A6:I6').format.fill='#DDEDE8';
s.getRange('A6:I6').format.font.bold=true;
s.getRange('H3:H4').format.font.color='#1D4ED8';
s.getRange('E7:G13').format.font.color='#1D4ED8';
s.getRange('G7:H27').setNumberFormat('"R$" #,##0.00');
s.getRange('A1:I30').format.rowHeight=24;
s.getRange('A7:I13').format.rowHeight=48;
s.getRange('A14:I17').format.rowHeight=6;
s.getRange('A1:I30').format.wrapText=true;
for(const [col,width] of [['A',10],['B',19],['C',14],['D',31],['E',13],['F',12],['G',16],['H',18],['I',57]]) s.getRange(`${col}:${col}`).format.columnWidth=width;
s.getRange('D27:H27').format.fill='#DDEDE8';
s.getRange('D27:H27').format.font.bold=true;
s.freezePanes.freezeRows(6);
await fs.mkdir(path.join(root,'docs'),{recursive:true});
await (await SpreadsheetFile.exportXlsx(wb)).save(path.join(root,'docs/custos.xlsx'));
console.log(JSON.stringify(await wb.inspect({kind:'region',sheetId:'Custos',range:'D18:H27',maxChars:2500,tableMaxRows:10,tableMaxCols:5})));
const preview=await wb.render({sheetName:'Custos',range:'A1:I30',scale:1.3,format:'png'});
await fs.writeFile(path.join(root,'target/academic/custos-preview.png'),new Uint8Array(await preview.arrayBuffer()));
