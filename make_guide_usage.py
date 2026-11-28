#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Génère le mode d'emploi MLTV (PDF), style cohérent avec le guide d'installation."""
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.lib.colors import HexColor
from reportlab.pdfgen import canvas

ORANGE = HexColor("#FF7A18")
DARK = HexColor("#1B1B1F")
GREY = HexColor("#555555")
LIGHT = HexColor("#FFF2E8")
W, H = A4

c = canvas.Canvas("Mode-emploi-MLTV.pdf", pagesize=A4)

def header(title, subtitle):
    c.setFillColor(ORANGE)
    c.rect(0, H - 34*mm, W, 34*mm, fill=1, stroke=0)
    # petit logo (triangle play dans carré)
    c.setFillColor(HexColor("#FFFFFF"))
    c.roundRect(16*mm, H - 27*mm, 14*mm, 14*mm, 2*mm, fill=1, stroke=0)
    c.setFillColor(ORANGE)
    p = c.beginPath()
    p.moveTo(21*mm, H - 24*mm); p.lineTo(21*mm, H - 16*mm); p.lineTo(28*mm, H - 20*mm)
    p.close(); c.drawPath(p, fill=1, stroke=0)
    c.setFillColor(HexColor("#FFFFFF"))
    c.setFont("Helvetica-Bold", 22)
    c.drawString(36*mm, H - 20*mm, title)
    c.setFont("Helvetica", 11)
    c.drawString(36*mm, H - 27*mm, subtitle)

def section(y, titre):
    c.setFillColor(ORANGE)
    c.setFont("Helvetica-Bold", 13)
    c.drawString(16*mm, y, titre)
    c.setStrokeColor(ORANGE); c.setLineWidth(1)
    c.line(16*mm, y - 2*mm, W - 16*mm, y - 2*mm)
    return y - 8*mm

def bullet(y, gras, texte):
    c.setFillColor(ORANGE)
    c.circle(18*mm, y + 1.1*mm, 1.0*mm, fill=1, stroke=0)
    c.setFillColor(DARK)
    c.setFont("Helvetica-Bold", 10.5)
    c.drawString(22*mm, y, gras)
    wgt = c.stringWidth(gras, "Helvetica-Bold", 10.5)
    c.setFillColor(GREY)
    c.setFont("Helvetica", 10.5)
    # retour à la ligne simple si trop long
    maxw = W - 16*mm - (22*mm + wgt)
    mots = texte.split()
    ligne = ""
    yy = y
    first = True
    for m in mots:
        test = (ligne + " " + m).strip()
        if c.stringWidth(test, "Helvetica", 10.5) > (maxw if first else (W - 16*mm - 22*mm)):
            if first:
                c.drawString(22*mm + wgt, yy, " " + ligne)
                first = False
            else:
                c.drawString(22*mm, yy, ligne)
            yy -= 5.0*mm
            ligne = m
        else:
            ligne = test
    if ligne:
        if first:
            c.drawString(22*mm + wgt, yy, " " + ligne)
        else:
            c.drawString(22*mm, yy, ligne)
    return yy - 7.0*mm

# ---------- PAGE 1 ----------
header("MLTV", "Mode d'emploi — se servir de l'application sur Android TV")

y = H - 44*mm
c.setFillColor(GREY); c.setFont("Helvetica-Oblique", 10.5)
c.drawString(16*mm, y, "Tout se pilote a la telecommande. Les quelques commandes utiles tiennent sur cette page.")
y -= 10*mm

y = section(y, "Regarder la television")
y = bullet(y, "Se deplacer :", "fleches HAUT / BAS pour parcourir les chaines, OK pour lancer celle qui est encadree.")
y = bullet(y, "Plein ecran :", "appuie encore sur OK sur la chaine en cours. En plein ecran, HAUT / BAS zappe directement.")
y = bullet(y, "Revenir :", "la touche GAUCHE ramene toujours en arriere (plein ecran -> liste, liste -> categories).")
y = bullet(y, "Changer de bouquet :", "va dans la colonne des categories (a gauche) et choisis le bouquet voulu.")

y -= 2*mm
y = section(y, "Favoris, recherche et tri")
y = bullet(y, "Mettre en favori :", "appui LONG sur OK sur une chaine. Retrouve-les dans la categorie « Favoris ».")
y = bullet(y, "Masquer / reorganiser :", "appui LONG sur OK ouvre le menu pour masquer ou deplacer une chaine ou un bouquet.")
y = bullet(y, "Rechercher :", "ouvre la recherche et tape le nom d'une chaine pour la trouver vite.")

y -= 2*mm
y = section(y, "Replay (rattrapage)")
y = bullet(y, "Reperer :", "dans le guide, une petite icone signale les programmes rejouables.")
y = bullet(y, "Rejouer :", "selectionne un programme passe muni de cette icone et lance-le pour le revoir.")

y -= 2*mm
y = section(y, "Films et Series")
y = bullet(y, "Parcourir :", "ouvre Films ou Series ; le catalogue est classe par categories (ouverture sur « Tout »).")
y = bullet(y, "A regarder plus tard :", "ajoute un film ou une serie a ta liste pour le retrouver facilement.")
y = bullet(y, "Reprendre :", "un contenu deja commence reprend la ou tu t'es arrete. Sous-titres reglables.")

y -= 2*mm
y = section(y, "Reglages utiles (roue dentee)")
y = bullet(y, "Source :", "playlist M3U ou Xtream Codes (identifiants de l'abonnement).")
y = bullet(y, "Fluidite :", "Equilibre / Confort / Maxi selon ta connexion, si l'image coupe.")
y = bullet(y, "Theme :", "8 couleurs au choix pour l'accent de l'interface.")

# pied de page
c.setFillColor(LIGHT)
c.rect(0, 0, W, 16*mm, fill=1, stroke=0)
c.setFillColor(ORANGE); c.setFont("Helvetica-Bold", 9.5)
c.drawCentredString(W/2, 6.5*mm, "MLTV — lecteur IPTV personnel    •    installation : laimarc.github.io/MLTV/MLTV.apk")

c.showPage()
c.save()
print("OK")
