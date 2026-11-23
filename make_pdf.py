from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.lib.colors import HexColor
from reportlab.pdfgen import canvas
from reportlab.platypus import Paragraph
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.enums import TA_LEFT

ORANGE = HexColor("#FF7A18")
INK = HexColor("#14181f")
MUTED = HexColor("#5d6470")
LINE = HexColor("#e6e8ec")
PANEL = HexColor("#f6f7f9")
TIPBG = HexColor("#fff6ef")
TIPBD = HexColor("#ffd9b8")

W, H = A4
c = canvas.Canvas("/home/claude/MLTV/Guide-MLTV-AndroidTV.pdf", pagesize=A4)
c.setTitle("Installer MLTV sur Android TV")

LM = 22 * mm
RM = W - 22 * mm
y = H - 24 * mm

# Header
c.setFillColor(ORANGE)
c.setFont("Helvetica-Bold", 30)
c.drawCentredString(W / 2, y, "MLTV")
y -= 9 * mm
c.setFillColor(MUTED)
c.setFont("Helvetica", 11.5)
c.drawCentredString(W / 2, y, "Installer l'application sur une box Android TV — en 5 etapes")
y -= 12 * mm

body_style = ParagraphStyle("body", fontName="Helvetica", fontSize=11.5, leading=16,
                            textColor=INK, alignment=TA_LEFT)
title_style = ParagraphStyle("title", fontName="Helvetica-Bold", fontSize=13.5, leading=16,
                             textColor=INK, alignment=TA_LEFT)

steps = [
    ("Installer &laquo; Downloader &raquo;",
     "Sur la TV, ouvre le <b>Play Store</b> et installe l'application <b>Downloader</b> (icone orange)."),
    ("Entrer l'adresse",
     "Ouvre <b>Downloader</b>, tape cette adresse puis valide sur <b>Go</b> :", "ADDR"),
    ("Autoriser l'installation",
     "Si la TV demande d'<b>autoriser les sources inconnues</b>, choisis <b>Autoriser</b> (c'est normal et sans risque)."),
    ("Installer puis ouvrir",
     "Quand le telechargement est fini, appuie sur <b>Installer</b>, puis sur <b>Ouvrir</b>."),
    ("Se connecter",
     "Dans MLTV, va sur la <b>roue dentee</b> puis <b>Xtream Codes</b>, saisis <b>serveur</b>, "
     "<b>utilisateur</b> et <b>mot de passe</b> de l'abonnement, puis <b>Se connecter</b>. "
     "Les chaines s'affichent !"),
]

col_x = LM + 14 * mm
col_w = RM - col_x

for i, step in enumerate(steps, 1):
    title, text = step[0], step[1]
    addr = len(step) > 2
    # top separator line
    c.setStrokeColor(LINE)
    c.setLineWidth(0.8)
    c.line(LM, y + 6 * mm, RM, y + 6 * mm)
    # number badge
    c.setFillColor(ORANGE)
    c.circle(LM + 5 * mm, y - 1 * mm, 5 * mm, fill=1, stroke=0)
    c.setFillColor(HexColor("#ffffff"))
    c.setFont("Helvetica-Bold", 14)
    c.drawCentredString(LM + 5 * mm, y - 3 * mm, str(i))
    # title
    p = Paragraph(title, title_style)
    tw, th = p.wrap(col_w, 20 * mm)
    p.drawOn(c, col_x, y - th + 3 * mm)
    yy = y - th - 1 * mm
    # body
    p = Paragraph(text, body_style)
    bw, bh = p.wrap(col_w, 40 * mm)
    p.drawOn(c, col_x, yy - bh + 2 * mm)
    yy = yy - bh
    # address box
    if addr:
        yy -= 3 * mm
        box_h = 9 * mm
        c.setFillColor(PANEL)
        c.setStrokeColor(LINE)
        c.roundRect(col_x, yy - box_h + 2 * mm, col_w, box_h, 3 * mm, fill=1, stroke=1)
        c.setFillColor(INK)
        c.setFont("Courier-Bold", 12)
        c.drawString(col_x + 4 * mm, yy - box_h + 5 * mm, "laimarc.github.io/MLTV/MLTV.apk")
        yy -= box_h
    y = yy - 9 * mm

# Tip box
y -= 2 * mm
tip_h = 16 * mm
c.setFillColor(TIPBG)
c.setStrokeColor(TIPBD)
c.roundRect(LM, y - tip_h, RM - LM, tip_h, 4 * mm, fill=1, stroke=1)
tip_style = ParagraphStyle("tip", fontName="Helvetica", fontSize=11, leading=15, textColor=INK)
tp = Paragraph("<b><font color='#FF7A18'>Mises a jour automatiques</font></b> &mdash; rien a faire : "
               "au demarrage, MLTV propose tout seul la nouvelle version. Il suffit d'accepter.", tip_style)
w2, h2 = tp.wrap(RM - LM - 12 * mm, tip_h)
tp.drawOn(c, LM + 6 * mm, y - tip_h / 2 - h2 / 2 + 2 * mm)
y -= tip_h

# Footer
c.setFillColor(MUTED)
c.setFont("Helvetica", 9.5)
c.drawCentredString(W / 2, 16 * mm, "MLTV — lecteur IPTV personnel   -   lien d'installation : laimarc.github.io/MLTV/MLTV.apk")

c.showPage()
c.save()
print("OK")
