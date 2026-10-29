"""Deterministic, synthetic XLSX import fixture; no employee data."""
import sys
from zipfile import ZipFile, ZIP_DEFLATED
from xml.sax.saxutils import escape
from pathlib import Path

def workbook(path,rows):
    def cell(col,row,value):
        return f'<c r="{col}{row}" t="inlineStr"><is><t>{escape(str(value))}</t></is></c>'
    all_rows=[['Full Name','Gender','Skill Level','Base Price','Auction Order','Player Reference']]+rows
    sheet='<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>'+''.join('<row r="'+str(i)+'">'+''.join(cell(chr(65+j),i,v) for j,v in enumerate(r))+'</row>' for i,r in enumerate(all_rows,1))+'</sheetData></worksheet>'
    with ZipFile(path,'w',ZIP_DEFLATED) as z:
        z.writestr('[Content_Types].xml','<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>')
        z.writestr('_rels/.rels','<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>')
        z.writestr('xl/workbook.xml','<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Players" sheetId="1" r:id="rId1"/></sheets></workbook>')
        z.writestr('xl/_rels/workbook.xml.rels','<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>')
        z.writestr('xl/worksheets/sheet1.xml',sheet)

target=Path(sys.argv[1]);target.mkdir(parents=True,exist_ok=True)
rows=[]
for team in range(4):
    for n in range(10):
        female=n >= (8 if team==3 else 7)
        rows.append([f'Test Player {team}-{n}','FEMALE' if female else 'MALE','Corporate',5000,team*10+n+1,f'EMP-{team}-{n}'])
workbook(target/'forty-players.xlsx',rows)
workbook(target/'captain-duplicate.xlsx',[['Test Captain 0','MALE','',5000,'','WRONG-CAPTAIN-REFERENCE']])
print('Created 40-player and captain-duplicate synthetic XLSX fixtures')
