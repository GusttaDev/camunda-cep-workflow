"""
Gera uma imagem SVG de um arquivo BPMN, no estilo do Camunda Modeler / bpmn-js.

Usa as coordenadas gravadas no proprio .bpmn (secao <bpmndi:BPMNDiagram>), as mesmas
que o Operate e o Modeler usam para desenhar o processo. So depende da biblioteca padrao.

Uso (a partir da raiz do projeto):
    python3 docs/render_bpmn.py                                  # gera docs/process-cep.svg
    python3 docs/render_bpmn.py caminho/entrada.bpmn saida.svg   # outro arquivo

Rode de novo sempre que alterar o process-cep.bpmn.
"""
import sys
import math
import xml.etree.ElementTree as ET
from html import escape

NS = {
    "bpmn": "http://www.omg.org/spec/BPMN/20100524/MODEL",
    "bpmndi": "http://www.omg.org/spec/BPMN/20100524/DI",
    "dc": "http://www.omg.org/spec/DD/20100524/DC",
    "di": "http://www.omg.org/spec/DD/20100524/DI",
    "zeebe": "http://camunda.org/schema/zeebe/1.0",
}
STROKE = "#22242a"
FONT = "Arial, Helvetica, sans-serif"

from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
src = sys.argv[1] if len(sys.argv) > 1 else ROOT / "src/main/resources/bpmn/process-cep.bpmn"
out = sys.argv[2] if len(sys.argv) > 2 else ROOT / "docs/process-cep.svg"
root = ET.parse(src).getroot()
process = root.find("bpmn:process", NS)

elements = {}
for el in process:
    tag = el.tag.split("}")[1]
    info = {"tag": tag, "name": el.get("name", "")}
    td = el.find("bpmn:extensionElements/zeebe:taskDefinition", NS)
    if td is not None:
        info["type"] = td.get("type")
    if el.find("bpmn:timerEventDefinition", NS) is not None:
        info["timer"] = el.find("bpmn:timerEventDefinition/bpmn:timeDuration", NS).text
    cond = el.find("bpmn:conditionExpression", NS)
    if cond is not None:
        info["cond"] = cond.text
    if el.get("default"):
        info["default"] = el.get("default")
    elements[el.get("id")] = info

defaults = {i["default"] for i in elements.values() if "default" in i}

svg = []
add = svg.append
maxx = maxy = 0


def text_block(x, y, w, lines, size=12, weight="normal", style="normal", color=STROKE, anchor="middle"):
    cx = x + w / 2 if anchor == "middle" else x
    out_ = []
    for i, line in enumerate(lines):
        out_.append(
            f'<text x="{cx:.1f}" y="{y + i * (size + 3):.1f}" font-family="{FONT}" font-size="{size}" '
            f'font-weight="{weight}" font-style="{style}" fill="{color}" text-anchor="{anchor}">{escape(line)}</text>'
        )
    return "".join(out_)


def wrap(text, max_chars):
    words, lines, cur = text.split(), [], ""
    for w in words:
        if len(cur) + len(w) + (1 if cur else 0) > max_chars:
            lines.append(cur)
            cur = w
        else:
            cur = f"{cur} {w}" if cur else w
    if cur:
        lines.append(cur)
    return lines


def gear(x, y):
    # engrenagem simples (icone de service task)
    cx, cy, r1, r2 = x + 9, y + 9, 7, 4.5
    pts = []
    for i in range(16):
        ang = math.pi * 2 * i / 16
        r = r1 if i % 2 == 0 else r1 - 2
        pts.append(f"{cx + r * math.cos(ang):.1f},{cy + r * math.sin(ang):.1f}")
    return (f'<polygon points="{" ".join(pts)}" fill="white" stroke="{STROKE}" stroke-width="1"/>'
            f'<circle cx="{cx}" cy="{cy}" r="{r2 - 1.5}" fill="white" stroke="{STROKE}" stroke-width="1"/>')


def clock(cx, cy, r):
    s = f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="white" stroke="{STROKE}" stroke-width="1.5"/>'
    for i in range(12):
        a = math.pi * 2 * i / 12
        s += (f'<line x1="{cx + (r - 3) * math.cos(a):.1f}" y1="{cy + (r - 3) * math.sin(a):.1f}" '
              f'x2="{cx + r * math.cos(a):.1f}" y2="{cy + r * math.sin(a):.1f}" stroke="{STROKE}" stroke-width="1"/>')
    s += f'<line x1="{cx}" y1="{cy}" x2="{cx}" y2="{cy - r + 4}" stroke="{STROKE}" stroke-width="1.5"/>'
    s += f'<line x1="{cx}" y1="{cy}" x2="{cx + r - 5}" y2="{cy}" stroke="{STROKE}" stroke-width="1.5"/>'
    return s


plane = root.find("bpmndi:BPMNDiagram/bpmndi:BPMNPlane", NS)

# arestas primeiro (ficam por baixo)
for edge in plane.findall("bpmndi:BPMNEdge", NS):
    eid = edge.get("bpmnElement")
    info = elements[eid]
    pts = [(float(w.get("x")), float(w.get("y"))) for w in edge.findall("di:waypoint", NS)]
    d = "M " + " L ".join(f"{x},{y}" for x, y in pts)
    add(f'<path d="{d}" fill="none" stroke="{STROKE}" stroke-width="1.5" marker-end="url(#arrow)"/>')
    if eid in defaults:  # marca de fluxo default
        (x1, y1), (x2, y2) = pts[0], pts[1]
        L = math.hypot(x2 - x1, y2 - y1)
        ux, uy = (x2 - x1) / L, (y2 - y1) / L
        mx, my = x1 + ux * 10, y1 + uy * 10
        px, py = -uy, ux
        add(f'<line x1="{mx - ux * 4 + px * 5:.1f}" y1="{my - uy * 4 + py * 5:.1f}" '
            f'x2="{mx + ux * 4 - px * 5:.1f}" y2="{my + uy * 4 - py * 5:.1f}" stroke="{STROKE}" stroke-width="1.5"/>')
    if "cond" in info:  # losango de fluxo condicional
        (x1, y1), (x2, y2) = pts[0], pts[1]
        L = math.hypot(x2 - x1, y2 - y1)
        ux, uy = (x2 - x1) / L, (y2 - y1) / L
        px, py = -uy, ux
        a = (x1, y1)
        b = (x1 + ux * 8 + px * 4, y1 + uy * 8 + py * 4)
        c = (x1 + ux * 16, y1 + uy * 16)
        e = (x1 + ux * 8 - px * 4, y1 + uy * 8 - py * 4)
        add(f'<polygon points="{a[0]},{a[1]} {b[0]:.1f},{b[1]:.1f} {c[0]:.1f},{c[1]:.1f} {e[0]:.1f},{e[1]:.1f}" '
            f'fill="white" stroke="{STROKE}" stroke-width="1.2"/>')
    lbl = edge.find("bpmndi:BPMNLabel/dc:Bounds", NS)
    if lbl is not None and info["name"]:
        lx, ly = float(lbl.get("x")), float(lbl.get("y"))
        add(text_block(lx, ly + 11, float(lbl.get("width")), [info["name"]], size=12, weight="bold"))

for shape in plane.findall("bpmndi:BPMNShape", NS):
    info = elements[shape.get("bpmnElement")]
    b = shape.find("dc:Bounds", NS)
    x, y, w, h = (float(b.get(k)) for k in ("x", "y", "width", "height"))
    maxx, maxy = max(maxx, x + w), max(maxy, y + h)
    tag = info["tag"]
    cx, cy, r = x + w / 2, y + h / 2, w / 2

    if tag == "startEvent":
        add(f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="#eef9ef" stroke="{STROKE}" stroke-width="2"/>')
    elif tag == "endEvent":
        add(f'<circle cx="{cx}" cy="{cy}" r="{r - 2}" fill="#fdecec" stroke="{STROKE}" stroke-width="4"/>')
    elif tag == "intermediateCatchEvent":
        add(f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="white" stroke="{STROKE}" stroke-width="1.5"/>')
        add(f'<circle cx="{cx}" cy="{cy}" r="{r - 3}" fill="white" stroke="{STROKE}" stroke-width="1.5"/>')
        if "timer" in info:
            add(clock(cx, cy, r - 7))
    elif tag == "exclusiveGateway":
        add(f'<polygon points="{cx},{y} {x + w},{cy} {cx},{y + h} {x},{cy}" fill="#fff8e6" stroke="{STROKE}" stroke-width="2"/>')
        k = 9
        add(f'<path d="M {cx - k} {cy - k} L {cx + k} {cy + k} M {cx + k} {cy - k} L {cx - k} {cy + k}" '
            f'stroke="{STROKE}" stroke-width="4" stroke-linecap="round"/>')
    elif tag in ("serviceTask", "task"):
        add(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="10" fill="#eef4ff" stroke="{STROKE}" stroke-width="2"/>')
        add(gear(x + 5, y + 5))
        lines = wrap(info["name"], 14)
        ty = cy - (len(lines) - 1) * 7.5 + 2
        add(text_block(x, ty, w, lines, size=12))
        if "type" in info:
            add(text_block(x, y + h + 14, w, [f'type: {info["type"]}'], size=10, style="italic", color="#3b5bdb"))

    lbl = shape.find("bpmndi:BPMNLabel/dc:Bounds", NS)
    if lbl is not None and info["name"] and tag not in ("serviceTask", "task"):
        lx, ly, lw = float(lbl.get("x")), float(lbl.get("y")), float(lbl.get("width"))
        lines = wrap(info["name"], 16)
        add(text_block(lx, ly + 11, lw, lines, size=11))
        if "timer" in info:
            add(text_block(lx, ly + 11 + len(lines) * 14, lw, [info["timer"]], size=10, style="italic", color="#5b6170"))
        maxy = max(maxy, ly + 30)
        maxx = max(maxx, lx + lw)

pad = 20
minx, miny = 120, 50
W, H = maxx - minx + 2 * pad, maxy - miny + 2 * pad
header = (
    f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{minx - pad} {miny - pad} {W} {H}" width="{W}" height="{H}">'
    f'<defs><marker id="arrow" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="8" markerHeight="8" orient="auto">'
    f'<path d="M 0 0 L 10 5 L 0 10 z" fill="{STROKE}"/></marker></defs>'
    f'<rect x="{minx - pad}" y="{miny - pad}" width="{W}" height="{H}" fill="white"/>'
)
with open(out, "w", encoding="utf-8") as f:
    f.write(header + "".join(svg) + "</svg>\n")
print(f"ok {out} {W}x{H}")
