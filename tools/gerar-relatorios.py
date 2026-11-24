"""Gera PDFs acadêmicos das fontes Markdown e diagramas locais; requer reportlab e Pillow."""
from pathlib import Path
import hashlib
import html
import json
import re
import textwrap
from datetime import datetime
from reportlab.lib import colors
from reportlab.lib.enums import TA_JUSTIFY
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, LongTable, TableStyle, Preformatted, Image

ROOT=Path(__file__).resolve().parents[1]
DOCS=ROOT/'docs'
OUT=DOCS/'pdf'
OUT.mkdir(exist_ok=True)
fontdir=Path('C:/Windows/Fonts')
if (fontdir/'arial.ttf').exists():
    pdfmetrics.registerFont(TTFont('Corpo',str(fontdir/'arial.ttf')))
    pdfmetrics.registerFont(TTFont('CorpoBold',str(fontdir/'arialbd.ttf')))
    pdfmetrics.registerFontFamily('Corpo',normal='Corpo',bold='CorpoBold',italic='Corpo',boldItalic='CorpoBold')
    FONT='Corpo'
else:
    FONT='Helvetica'
if (fontdir/'seguisym.ttf').exists():
    pdfmetrics.registerFont(TTFont('Simbolos',str(fontdir/'seguisym.ttf')))
styles=getSampleStyleSheet()
for name in ['BodyText','Heading1','Heading2','Heading3','Title']:
    styles[name].fontName=FONT
styles['BodyText'].fontSize=12
styles['BodyText'].leading=18
styles['BodyText'].alignment=TA_JUSTIFY
styles.add(ParagraphStyle(name='Celula',fontName=FONT,fontSize=7,leading=10,spaceAfter=2))
styles.add(ParagraphStyle(name='Rodape',fontName=FONT,fontSize=8,leading=11,textColor=colors.HexColor('#52616B')))

def markup(s):
    result=[]
    for part in re.split(r'(`[^`]*`)',s):
        if part.startswith('`') and part.endswith('`'):
            result.append('<font name="Courier">'+html.escape(part[1:-1])+'</font>')
        else:
            part=html.escape(part)
            part=re.sub(r'\[([^\]]+)\]\(([^)]+)\)',lambda m: m[1]+' ('+m[2]+')',part)
            result.append(re.sub(r'\*\*([^*]+)\*\*',r'<b>\1</b>',part))
    joined=''.join(result)
    if 'Simbolos' in pdfmetrics.getRegisteredFontNames():
        joined=re.sub(r'[∧∨¬⇒⇔∈∀∃≤≥≡φ]',lambda m:'<font name="Simbolos">'+m[0]+'</font>',joined)
    return joined

def parse(path):
    story=[]; lines=path.read_text(encoding='utf8').splitlines(); i=0
    while i<len(lines):
        line=lines[i].strip()
        if not line: story.append(Spacer(1,6)); i+=1; continue
        if line.startswith('```'):
            code=[];i+=1
            while i<len(lines) and not lines[i].startswith('```'):
                code.extend(textwrap.wrap(lines[i],92,replace_whitespace=False,drop_whitespace=False) or ['']);i+=1
            story.append(Preformatted('\n'.join(code),ParagraphStyle('Code',fontName='Courier',fontSize=7,leading=10)))
            i+=1;continue
        if line.startswith('|'):
            rows=[]
            while i<len(lines) and lines[i].strip().startswith('|'):
                cells=lines[i].strip().strip('|').split('|')
                if not all(re.fullmatch(r'\s*:?-+:?\s*',c) for c in cells):rows.append([c.strip() for c in cells])
                i+=1
            if rows:
                n=max(map(len,rows));width=(A4[0]-142)/n
                data=[[Paragraph(markup(c),styles['Celula']) for c in row+['']*(n-len(row))] for row in rows]
                t=LongTable(data,colWidths=[width]*n,repeatRows=1,hAlign='LEFT')
                t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),colors.HexColor('#DDEDE8')),('VALIGN',(0,0),(-1,-1),'TOP'),('GRID',(0,0),(-1,-1),.3,colors.lightgrey),('LEFTPADDING',(0,0),(-1,-1),4),('RIGHTPADDING',(0,0),(-1,-1),4)]))
                story.append(t)
            continue
        if line.startswith('#'):
            depth=len(line)-len(line.lstrip('#'))
            story.append(Paragraph(markup(line.lstrip('#').strip()),styles['Heading'+str(min(depth,3))]));i+=1;continue
        buf=[line];i+=1
        while i<len(lines) and lines[i].strip() and not re.match(r'^(#|\||```|[-*] |\d+\. )',lines[i].strip()):
            buf.append(lines[i].strip());i+=1
        story.append(Paragraph(markup(' '.join(buf)),styles['BodyText']))
    return story

def footer(canvas,doc):
    canvas.saveState();canvas.setFont(FONT,8)
    canvas.setFillColor(colors.HexColor('#52616B'))
    canvas.drawString(50,30,'E-Waste | Projeto Integrador 2026-2 | fontes e pendências no repositório')
    canvas.drawRightString(A4[0]-50,30,str(doc.page));canvas.restoreState()

bundles={
 'estrutura-dados':['estrutura-dados.md'],
 'teoria-computacao':['teoria-computacao.md'],
 'banco-dados':['banco-dados.md','sql-recursos.md','operacao-banco.md'],
 'design-software':['requisitos.md','arquitetura.md','rastreabilidade.md','testes.md','implantacao.md'],
 'administracao':['administracao.md']
}
manifest={}
for name,files in bundles.items():
    story=[Paragraph('FACULDADE SENAI FATESG',styles['Title']),Spacer(1,15),Paragraph('Projeto Integrador — Engenharia de Software / 4º período / 2026-2',styles['BodyText']),Spacer(1,20),Paragraph(name.replace('-',' ').title(),styles['Title']),Spacer(1,20),Paragraph('E-Waste — gestão de descarte e reciclagem de resíduos eletrônicos',styles['BodyText']),Spacer(1,15),Paragraph('Equipe: integrantes e atribuições a confirmar. Artefato técnico com pendências explicitadas; não declara aprovação docente ou conformidade integral.',styles['BodyText']),PageBreak()]
    for filename in files:
        source=DOCS/filename
        if not source.exists():raise FileNotFoundError(source)
        story+=parse(source)+[PageBreak()]
        manifest[filename]=hashlib.sha256(source.read_bytes()).hexdigest()
    if name in ['design-software','banco-dados','administracao']:
        patterns=['*.png'] if name=='design-software' else ['der-*.png'] if name=='banco-dados' else ['organograma.png']
        for pattern in patterns:
            for image in sorted((DOCS/'uml').glob(pattern)):
                from PIL import Image as PILImage
                w,h=PILImage.open(image).size
                factor=min((A4[0]-142)/w,(A4[1]-190)/h)
                story += [Paragraph(image.stem.replace('-',' ').title(),styles['Heading1']),Image(str(image),width=w*factor,height=h*factor),PageBreak()]
    if isinstance(story[-1],PageBreak):story.pop()
    pdf=OUT/(name+'.pdf')
    SimpleDocTemplate(str(pdf),pagesize=A4,rightMargin=57,leftMargin=85,topMargin=85,bottomMargin=57,title='E-Waste — '+name,author='Equipe E-Waste — atribuições pendentes').build(story,onFirstPage=footer,onLaterPages=footer)
    print(pdf.name,pdf.stat().st_size)
(OUT/'fontes-sha256.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False),encoding='utf8')
