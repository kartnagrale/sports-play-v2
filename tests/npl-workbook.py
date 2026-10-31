"""Deterministic, synthetic XLSX import fixture; no employee data."""
import sys
from zipfile import ZipFile, ZIP_DEFLATED
from xml.sax.saxutils import escape
from pathlib import Path
import xml.etree.ElementTree as ET
import struct
import zlib

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

def from_template(template, path, rows):
    """Populate the real downloaded template; preserve styles, validation and instructions."""
    ns='http://schemas.openxmlformats.org/spreadsheetml/2006/main'
    ET.register_namespace('',ns)
    with ZipFile(template) as source:
        sheet=ET.fromstring(source.read('xl/worksheets/sheet1.xml'))
        data=sheet.find(f'{{{ns}}}sheetData')
        for row in list(data)[1:]:
            data.remove(row)
        for index, values in enumerate(rows,2):
            row=ET.SubElement(data,f'{{{ns}}}row',{'r':str(index)})
            for col,value in enumerate(values):
                cell=ET.SubElement(row,f'{{{ns}}}c',{'r':f'{chr(65+col)}{index}'})
                if isinstance(value,(int,float)):
                    ET.SubElement(cell,f'{{{ns}}}v').text=str(value)
                else:
                    cell.set('t','inlineStr')
                    ET.SubElement(ET.SubElement(cell,f'{{{ns}}}is'),f'{{{ns}}}t').text=str(value)
        dimension=sheet.find(f'{{{ns}}}dimension')
        if dimension is not None: dimension.set('ref',f'A1:F{len(rows)+1}')
        with ZipFile(path,'w',ZIP_DEFLATED) as output:
            for entry in source.infolist():
                output.writestr(entry,ET.tostring(sheet,encoding='utf-8',xml_declaration=True) if entry.filename=='xl/worksheets/sheet1.xml' else source.read(entry.filename))

target=Path(sys.argv[1]);target.mkdir(parents=True,exist_ok=True)
if len(sys.argv)>2 and sys.argv[2]=='--full':
    template=target/'downloaded-template.xlsx'
    names=['Lyra','Mira','Nova','Orin','Pavo','Quill','Riven','Sora','Tavi','Vela']
    rows=[[f'Mock {names[n]} {chr(65+team)}','FEMALE' if n>=7 else 'MALE','Advanced' if n<4 else 'Intermediate',5000,team*10+n+1,f'MOCK-EMP-{team}-{n}'] for team in range(4) for n in range(10)]
    from_template(template,target/'forty-players.xlsx',rows)
    from_template(template,target/'invalid-row.xlsx',[['Invalid Mock','UNKNOWN','Intermediate',-1,1,'BAD-ROW']])
    from_template(template,target/'duplicate-reference.xlsx',[rows[0],rows[0]])
    # A synthetic geometric badge, never an employee photograph.
    def chunk(kind,data):
        return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
    pixels=b''.join(b'\0'+b''.join(bytes((245,245,255) if (x-64)**2+(y-64)**2<32**2 else (37,99,235)) for x in range(128)) for y in range(128))
    (target/'synthetic-avatar.png').write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',128,128,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(pixels))+chunk(b'IEND',b''))
    print('Created template-based synthetic fixtures: 40 players, 12 female and 28 male; separate invalid/duplicate workbooks')
    sys.exit(0)
rows=[]
for team in range(4):
    for n in range(10):
        female=n >= (8 if team==3 else 7)
        rows.append([f'Test Player {team}-{n}','FEMALE' if female else 'MALE','Corporate',5000,team*10+n+1,f'EMP-{team}-{n}'])
workbook(target/'forty-players.xlsx',rows)
workbook(target/'captain-duplicate.xlsx',[['Test Captain 0','MALE','',5000,'','WRONG-CAPTAIN-REFERENCE']])
print('Created 40-player and captain-duplicate synthetic XLSX fixtures')
