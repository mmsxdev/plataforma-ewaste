"""Exporta os relatórios acadêmicos com capa, folha de rosto, resumos e sumário real.
Requer reportlab, Pillow e fontes Arial do Windows; preserva seleção --only.
"""
from pathlib import Path
import argparse, hashlib, html, json, re, textwrap
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import cm
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import BaseDocTemplate, PageTemplate, Frame, Paragraph, Spacer, PageBreak, LongTable, TableStyle, Preformatted, Image, KeepTogether
from reportlab.platypus.tableofcontents import TableOfContents

ROOT=Path(__file__).resolve().parents[1]
DOCS=ROOT/'docs'; OUT=DOCS/'pdf'; OUT.mkdir(exist_ok=True)
LEFT=TOP=3*cm; RIGHT=BOTTOM=2*cm; WIDTH=A4[0]-LEFT-RIGHT
FONT='ArialPI'
fontdir=Path('C:/Windows/Fonts')
if (fontdir/'arial.ttf').exists():
    pdfmetrics.registerFont(TTFont(FONT,str(fontdir/'arial.ttf')))
    pdfmetrics.registerFont(TTFont(FONT+'Bold',str(fontdir/'arialbd.ttf')))
    pdfmetrics.registerFontFamily(FONT,normal=FONT,bold=FONT+'Bold',italic=FONT,boldItalic=FONT+'Bold')
else: FONT='Helvetica'
if (fontdir/'seguisym.ttf').exists(): pdfmetrics.registerFont(TTFont('Simbolos',str(fontdir/'seguisym.ttf')))
styles=getSampleStyleSheet()
for key in ['BodyText','Heading1','Heading2','Heading3','Title']:
    styles[key].fontName=FONT
    styles[key].fontSize=12
    styles[key].leading=18
styles['BodyText'].alignment=TA_JUSTIFY
styles['BodyText'].spaceAfter=6
for key in ['Heading1','Heading2','Heading3']:
    styles[key].fontName=FONT+'Bold' if FONT=='ArialPI' else 'Helvetica-Bold'
    styles[key].spaceBefore=18;styles[key].spaceAfter=18;styles[key].keepWithNext=True
styles['Title'].alignment=TA_CENTER
styles.add(ParagraphStyle(name='Centro',fontName=FONT,fontSize=12,leading=18,alignment=TA_CENTER,spaceAfter=6))
styles.add(ParagraphStyle(name='Natureza',fontName=FONT,fontSize=12,leading=12,leftIndent=WIDTH/2,spaceAfter=12,alignment=TA_JUSTIFY))
styles.add(ParagraphStyle(name='Celula',fontName=FONT,fontSize=8,leading=10,spaceAfter=2))
styles.add(ParagraphStyle(name='Legenda',fontName=FONT,fontSize=10,leading=12,spaceAfter=8))
styles.add(ParagraphStyle(name='Referencia',fontName=FONT,fontSize=12,leading=12,alignment=TA_LEFT,spaceAfter=12))
AUTHORS=['Davi Fraga','Guilherme Gonçalves','Miguel Melo','Felipe Rohnelt','Rafael Camargo']
META={
 'estrutura-dados':('Estrutura de Dados II','Apresenta a árvore AVL integrada ao fluxo de consulta de pontos de coleta, com origem no PostgreSQL, operações de inserção, busca e percurso, complexidade, limitações e evidências de testes.','Describes the AVL tree integrated into collection-point queries, including PostgreSQL input, insertion, search, traversal, complexity, limitations and test evidence.','Árvore AVL; Banco de dados; Resíduos eletrônicos.'),
 'teoria-computacao':('Teoria da Computação','Formaliza regras de negócio por lógica e tabelas-verdade e demonstra operações de um exemplo RSA didático. Distingue os cálculos acadêmicos dos mecanismos efetivos de autenticação e integridade do aplicativo.','Formalizes business rules using logic and truth tables and demonstrates a didactic RSA example. Academic calculations are distinguished from the application authentication and integrity mechanisms.','Lógica; Aritmética modular; Criptografia.'),
 'banco-dados':('Banco de Dados II','Documenta o modelo de negócio, os modelos conceitual e lógico, o dicionário de dados e recursos SQL do PostgreSQL 16. Descreve transações, privilégios, recuperação e os limites das evidências locais de execução.','Documents the business model, conceptual and logical models, data dictionary and PostgreSQL 16 resources. It describes transactions, privileges, recovery and the limits of local execution evidence.','PostgreSQL; Modelagem; Recuperação.'),
 'design-software':('Design de Software','Relaciona requisitos, critérios de aceitação, arquitetura, UML, testes e implantação do protótipo E-Waste. Distingue o fluxo funcional validado, as decisões da equipe e as etapas de rede e submissão ainda não executadas.','Relates requirements, acceptance criteria, architecture, UML, tests and deployment of the E-Waste prototype. It distinguishes validated functionality, team decisions and network and submission tasks not yet executed.','Requisitos; Arquitetura; Testes.'),
 'administracao':('Fundamentos da Administração','Apresenta identidade, Missão, Visão e Valores aprovados pela equipe, organograma com responsabilidades e estimativas de custos do protótipo. Os valores são premissas didáticas, sem comprovação de despesas ou horas realizadas.','Presents the approved identity, mission, vision and values, assigned project responsibilities and prototype cost estimates. Values are didactic assumptions rather than evidence of expenses or hours worked.','Organograma; Planejamento; Custos.')}

def markup(value):
    parts=[]
    for part in re.split(r'(`[^`]*`)',value):
        if part.startswith('`') and part.endswith('`'): parts.append('<font name="Courier">'+html.escape(part[1:-1])+'</font>')
        else:
            part=html.escape(part)
            part=re.sub(r'\[([^\]]+)\]\(([^)]+)\)',lambda m:m[1]+' ('+m[2]+')',part)
            parts.append(re.sub(r'\*\*([^*]+)\*\*',r'<b>\1</b>',part))
    result=''.join(parts)
    if 'Simbolos' in pdfmetrics.getRegisteredFontNames():result=re.sub(r'[∧∨¬⇒⇔∈∀∃≤≥≡φ]',lambda m:'<font name="Simbolos">'+m[0]+'</font>',result)
    return result

class AcademicDoc(BaseDocTemplate):
    def __init__(self,*args,**kwargs):
        super().__init__(*args,leftMargin=LEFT,rightMargin=RIGHT,topMargin=TOP,bottomMargin=BOTTOM,**kwargs)
        self.addPageTemplates(PageTemplate(id='A4',frames=Frame(LEFT,BOTTOM,WIDTH,A4[1]-TOP-BOTTOM,id='corpo',leftPadding=0,rightPadding=0,topPadding=0,bottomPadding=0),onPageEnd=self.page_number))
    def beforeDocument(self):self.text_started=False
    def afterFlowable(self,flowable):
        if hasattr(flowable,'toc_level'):
            self.text_started=True
            self.canv.bookmarkPage(flowable.toc_key)
            self.notify('TOCEntry',(flowable.toc_level,flowable.toc_text,self.page-1,flowable.toc_key))
    def page_number(self,canvas,doc):
        if self.text_started:
            canvas.saveState();canvas.setFont(FONT,10)
            canvas.drawRightString(A4[0]-RIGHT,A4[1]-2*cm,str(doc.page-1));canvas.restoreState()

class Sections:
    def __init__(self):self.n=[0,0,0];self.count=0
    def heading(self,text,level=0,number=True):
        self.count+=1
        if number:
            self.n[level]+=1
            for j in range(level+1,3):self.n[j]=0
            text=re.sub(r'^\d+(?:\.\d+)*\s+','',text)
            text='.'.join(map(str,self.n[:level+1]))+' '+text
        p=Paragraph(markup(text.upper() if level==0 else text),styles['Heading'+str(level+1)])
        p.toc_level=level;p.toc_text=html.escape(text.upper() if level==0 else text);p.toc_key='sec-'+str(self.count)
        return p

def parse(source,sections,references):
    lines=source.read_text(encoding='utf8').splitlines();story=[];i=0;in_refs=False
    while i<len(lines):
        line=lines[i].strip()
        if line.startswith('```'):
            code=[];i+=1
            while i<len(lines) and not lines[i].startswith('```'):
                code.extend(textwrap.wrap(lines[i],90,replace_whitespace=False,drop_whitespace=False) or ['']);i+=1
            story.append(Preformatted('\n'.join(code),ParagraphStyle('Code',fontName='Courier',fontSize=8,leading=10)));i+=1;continue
        if line.startswith('#'):
            depth=len(line)-len(line.lstrip('#'));title=line.lstrip('#').strip()
            in_refs=bool(re.fullmatch(r'Referências?',title,re.I))
            if not in_refs:
                level=min(depth-1,2)
                if level==0 and story:story.append(PageBreak())
                story.append(sections.heading(title,level))
            i+=1;continue
        if not line:i+=1;continue
        if in_refs:
            buf=[line];i+=1
            while i<len(lines) and lines[i].strip() and not lines[i].startswith('#'):buf.append(lines[i].strip());i+=1
            references.append(' '.join(buf));continue
        if line.startswith('|'):
            rows=[]
            while i<len(lines) and lines[i].strip().startswith('|'):
                cells=lines[i].strip().strip('|').split('|')
                if not all(re.fullmatch(r'\s*:?-+:?\s*',c) for c in cells):rows.append([c.strip() for c in cells])
                i+=1
            if rows:
                n=max(map(len,rows));data=[[Paragraph(markup(c),styles['Celula']) for c in row+['']*(n-len(row))] for row in rows]
                t=LongTable(data,colWidths=[WIDTH/n]*n,repeatRows=1,hAlign='LEFT')
                t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),colors.HexColor('#DDEDE8')),('VALIGN',(0,0),(-1,-1),'TOP'),('LINEBELOW',(0,0),(-1,0),.5,colors.black),('LINEBELOW',(0,-1),(-1,-1),.5,colors.black),('LEFTPADDING',(0,0),(-1,-1),4),('RIGHTPADDING',(0,0),(-1,-1),4)]));story.append(t)
            continue
        buf=[line];i+=1
        while i<len(lines) and lines[i].strip() and not re.match(r'^(#|\||```|[-*] |\d+\. )',lines[i].strip()):buf.append(lines[i].strip());i+=1
        story.append(Paragraph(markup(' '.join(buf)),styles['BodyText']))
    return story

def preliminaries(name):
    title,pt,en,keywords=META[name]
    pt += ' O relatório faz parte do Projeto Integrador do quarto período de Engenharia de Software da Faculdade SENAI FATESG, turma 1, semestre 2026-2. O estudo utiliza o protótipo existente, suas fontes versionadas e os resultados de execução registrados no repositório. A análise relaciona as exigências acadêmicas às decisões de modelagem, implementação e validação, sem tratar a existência de arquivos como comprovação de comportamento. As demonstrações usam dados fictícios e não representam uma operação em produção. A organização da equipe distingue responsabilidades futuras de contribuições efetivamente realizadas. As evidências anteriores mantêm seus comandos, resultados e limites originais. A transferência do banco para outro computador e a validação da rede dependem de execução específica, não sendo inferidas de testes locais. O material permite que cada integrante associe conceitos da disciplina ao fluxo funcional e explique suas decisões. A conclusão deve ser lida em conjunto com as referências, os modelos e as evidências indicadas nas seções.'
    en += ' This report is part of the fourth-semester Software Engineering integrative project at Faculdade SENAI FATESG, class 1, in the second semester of 2026. It examines the existing prototype, versioned sources and execution results recorded in the repository. Academic requirements are related to modeling, implementation and validation decisions. The presence of a file is not treated as evidence that a behavior works. Demonstrations use fictitious data and do not constitute a production operation. Team organization distinguishes future responsibilities from work actually completed. Previous evidence retains its original commands, results and limitations. Database transfer and network validation require specific execution and cannot be inferred from local tests. The material enables each team member to relate course concepts to the functional workflow, explain design decisions and identify the associated tests. Conclusions should be considered together with the references, models and evidence presented in the report.'
    center=lambda t:Paragraph(markup(t),styles['Centro'])
    authors=[center(a.upper()) for a in AUTHORS]
    story=[center('FACULDADE SENAI FATESG'),center('ENGENHARIA DE SOFTWARE — 4º PERÍODO — TURMA 1'),Spacer(1,24)]+authors+[Spacer(1,75),center('<b>E-WASTE: RESÍDUOS COM DESTINO</b>'.replace('<b>','').replace('</b>','')),center(title.upper()),Spacer(1,125),center('GOIÂNIA'),center('2026'),PageBreak()]
    story += [center(a.upper()) for a in AUTHORS]
    story += [Spacer(1,65),center('E-WASTE: RESÍDUOS COM DESTINO'),center(title.upper()),Spacer(1,40),Paragraph(markup('Relatório acadêmico apresentado ao Projeto Integrador 2026-2 do curso de Engenharia de Software, 4º período, turma 1, da Faculdade SENAI FATESG. Disciplina: '+title+'. Grupo 01.'),styles['Natureza']),Paragraph('Professora líder: Carmen Cecília Centeno da Silva.',styles['Natureza']),Spacer(1,90),center('GOIÂNIA'),center('2026'),PageBreak()]
    story += [center('RESUMO'),Spacer(1,18),Paragraph(markup(pt),styles['BodyText']),Paragraph('Palavras-chave: '+markup(keywords.lower()),styles['BodyText']),PageBreak(),center('ABSTRACT'),Spacer(1,18),Paragraph(markup(en),styles['BodyText']),Paragraph('Keywords: E-Waste; software engineering; academic project.',styles['BodyText']),PageBreak(),center('SUMÁRIO'),Spacer(1,18)]
    toc=TableOfContents();toc.levelStyles=[ParagraphStyle('TOC'+str(i),fontName=FONT,fontSize=12,leading=18,leftIndent=i*12,firstLineIndent=0,spaceBefore=4) for i in range(3)]
    toc.dotsMinLevel=0
    return story+[toc,PageBreak()]

bundles={'estrutura-dados':['estrutura-dados.md'],'teoria-computacao':['teoria-computacao.md'],'banco-dados':['banco-dados.md','sql-recursos.md','operacao-banco.md'],'design-software':['requisitos.md','arquitetura.md','rastreabilidade.md','testes.md','implantacao.md','equipe.md'],'administracao':['administracao.md','equipe.md']}
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--only',nargs='+');args=parser.parse_args()
selected=set(bundles) if not args.only else {n for v in args.only for n in v.split(',')}
if selected-set(bundles):parser.error('Relatórios desconhecidos: '+', '.join(sorted(selected-set(bundles))))
manifest_path=OUT/'fontes-sha256.json';manifest=json.loads(manifest_path.read_text(encoding='utf8')) if manifest_path.exists() else {}
for name,files in bundles.items():
    if name not in selected:continue
    sections=Sections();references=[];story=preliminaries(name)
    for filename in files:
        source=DOCS/filename
        story+=parse(source,sections,references)+[PageBreak()]
        manifest[filename]=hashlib.sha256(source.read_text(encoding='utf8').encode('utf8')).hexdigest()
    if references:
        story.append(sections.heading('Referências',number=False))
        for text in sorted(dict.fromkeys(references)):story.append(Paragraph(markup(text),styles['Referencia']))
        story.append(PageBreak())
    if name in ['design-software','banco-dados','administracao']:
        pattern='*.png' if name=='design-software' else 'der-*.png' if name=='banco-dados' else 'organograma.png'
        for appendix_index,image in enumerate(sorted((DOCS/'uml').glob(pattern))):
            manifest['uml/'+image.name]=hashlib.sha256(image.read_bytes()).hexdigest()
            diagram=image.with_suffix('.puml')
            if diagram.exists():manifest['uml/'+diagram.name]=hashlib.sha256(diagram.read_text(encoding='utf8').encode('utf8')).hexdigest()
            from PIL import Image as PILImage
            w,h=PILImage.open(image).size;factor=min(WIDTH/w,(A4[1]-TOP-BOTTOM-100)/h)
            story += [sections.heading('Apêndice '+chr(65+appendix_index)+' — '+image.stem.replace('-',' '),number=False),Paragraph('Fonte: elaboração da equipe; docs/uml/'+image.name+'. Amplie o PDF ou consulte o PNG original para os detalhes. DER também possui dicionário textual no relatório de Banco de Dados.',styles['Legenda']),Image(str(image),width=w*factor,height=h*factor),PageBreak()]
    if isinstance(story[-1],PageBreak):story.pop()
    pdf=OUT/(name+'.pdf')
    AcademicDoc(str(pdf),pagesize=A4,title='E-Waste — '+META[name][0],author='; '.join(AUTHORS)).multiBuild(story)
    print(pdf.name,pdf.stat().st_size)
manifest.pop('gerador-relatorios',None)
manifest['../tools/gerar-relatorios.py']=hashlib.sha256(Path(__file__).read_text(encoding='utf8').encode('utf8')).hexdigest()
manifest_path.write_text(json.dumps(manifest,indent=2,ensure_ascii=False),encoding='utf8')
