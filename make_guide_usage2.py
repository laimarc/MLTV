#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Mode d'emploi détaillé MLTV (PDF multi-pages, étape par étape)."""
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.lib.colors import HexColor
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
_D="/usr/share/fonts/truetype/dejavu/"
pdfmetrics.registerFont(TTFont("DV", _D+"DejaVuSans.ttf"))
pdfmetrics.registerFont(TTFont("DVB", _D+"DejaVuSans-Bold.ttf"))
pdfmetrics.registerFont(TTFont("DVO", _D+"DejaVuSans-Oblique.ttf"))


ORANGE = HexColor("#FF7A18")
DARK = HexColor("#1B1B1F")
GREY = HexColor("#444444")
LIGHT = HexColor("#FFF2E8")
W, H = A4
LM, RM = 16*mm, 16*mm
TXTW = W - LM - RM

c = canvas.Canvas("Mode-emploi-MLTV.pdf", pagesize=A4)
y = 0.0

def footer():
    c.setFillColor(LIGHT); c.rect(0, 0, W, 14*mm, fill=1, stroke=0)
    c.setFillColor(ORANGE); c.setFont("DVB", 9)
    c.drawCentredString(W/2, 5.5*mm, "MLTV — lecteur IPTV personnel   •   installation : laimarc.github.io/MLTV/MLTV.apk")

def banner():
    c.setFillColor(ORANGE); c.rect(0, H - 30*mm, W, 30*mm, fill=1, stroke=0)
    c.setFillColor(HexColor("#FFFFFF")); c.roundRect(LM, H - 24*mm, 12*mm, 12*mm, 2*mm, fill=1, stroke=0)
    c.setFillColor(ORANGE)
    p = c.beginPath(); p.moveTo(LM+4*mm, H-21.5*mm); p.lineTo(LM+4*mm, H-14.5*mm); p.lineTo(LM+10*mm, H-18*mm); p.close()
    c.drawPath(p, fill=1, stroke=0)
    c.setFillColor(HexColor("#FFFFFF"))
    c.setFont("DVB", 20); c.drawString(LM+16*mm, H - 17*mm, "MLTV — Mode d'emploi")
    c.setFont("DV", 10.5); c.drawString(LM+16*mm, H - 23*mm, "Se servir de l'application sur Android TV (a la telecommande)")

def new_page(first=False):
    global y
    if not first:
        footer(); c.showPage()
    banner(); footer()
    y = H - 38*mm

def need(space):
    if y - space < 18*mm:
        new_page()

def wrap(text, font, size, width):
    words = text.split(); lines=[]; cur=""
    for w in words:
        t=(cur+" "+w).strip()
        if c.stringWidth(t,font,size) > width: lines.append(cur); cur=w
        else: cur=t
    if cur: lines.append(cur)
    return lines

def section(title):
    global y
    need(16*mm)
    c.setFillColor(ORANGE); c.setFont("DVB", 13)
    c.drawString(LM, y, title)
    c.setStrokeColor(ORANGE); c.setLineWidth(1); c.line(LM, y-2*mm, W-RM, y-2*mm)
    y -= 8*mm

def step(num, text):
    global y
    lines = wrap(text, "DV", 10.3, TXTW-9*mm)
    need(len(lines)*4.8*mm + 2*mm)
    # pastille numéro
    c.setFillColor(ORANGE); c.circle(LM+2.6*mm, y+1.0*mm, 2.6*mm, fill=1, stroke=0)
    c.setFillColor(HexColor("#FFFFFF")); c.setFont("DVB", 9)
    c.drawCentredString(LM+2.6*mm, y-0.6*mm, str(num))
    c.setFillColor(GREY); c.setFont("DV", 10.3)
    for i,ln in enumerate(lines):
        c.drawString(LM+9*mm, y, ln); y -= 4.8*mm
    y -= 1.6*mm

def note(text):
    global y
    lines = wrap(text, "DVO", 9.6, TXTW-6*mm)
    need(len(lines)*4.6*mm + 3*mm)
    c.setFillColor(LIGHT); c.roundRect(LM, y-(len(lines)*4.6*mm)+0.5*mm, TXTW, len(lines)*4.6*mm+3*mm, 2*mm, fill=1, stroke=0)
    c.setFillColor(HexColor("#A64B00")); c.setFont("DVO", 9.6)
    yy = y
    for ln in lines:
        c.drawString(LM+3*mm, yy, ln); yy -= 4.6*mm
    y = yy - 2*mm

new_page(first=True)

section("1. Se connecter (une seule fois)")
step(1, "Ouvre MLTV, puis va sur la roue dentee en haut a gauche.")
step(2, "Dans l'onglet « Source », choisis « Xtream Codes ».")
step(3, "Saisis l'adresse du serveur, l'utilisateur et le mot de passe de ton abonnement, puis « Se connecter ».")
step(4, "Les chaines se chargent. C'est enregistre : tu n'auras plus a le refaire.")

section("2. Regarder une chaine en direct")
step(1, "Dans la colonne des chaines (au centre), monte / descends avec les fleches HAUT et BAS.")
step(2, "Appuie sur OK sur une chaine : elle demarre dans l'apercu, a droite.")
step(3, "Appuie encore sur OK sur la meme chaine pour passer en PLEIN ECRAN.")
step(4, "En plein ecran : HAUT / BAS zappe d'une chaine a l'autre ; la touche GAUCHE revient a la liste.")
note("Astuce : l'application rouvre toujours sur la derniere chaine que tu regardais.")

section("3. Changer de bouquet (categorie)")
step(1, "Depuis la liste des chaines, appuie sur GAUCHE pour atteindre la colonne des categories.")
step(2, "Choisis un bouquet avec HAUT / BAS puis OK. « Tout » = toutes les chaines ; « ★ Favoris » = tes favorites.")

section("4. Mettre une chaine en favori")
step(1, "Place-toi sur une chaine, puis fais un appui LONG sur OK : « Ajoute aux favoris » s'affiche.")
step(2, "Un nouvel appui long la retire. Retrouve toutes tes favorites dans la categorie « ★ Favoris ».")

section("5. Ranger ou masquer un bouquet")
step(1, "Dans la colonne des categories, fais un appui LONG sur OK sur un bouquet.")
step(2, "Choisis « Deplacer (haut/bas) » pour le reordonner, ou « Masquer » pour le cacher (« Afficher » pour le remontrer).")
note("« Tout » et « ★ Favoris » restent toujours en haut et ne se deplacent pas.")

section("6. Rechercher une chaine")
step(1, "Remonte jusqu'au champ de recherche, en haut de la liste.")
step(2, "Tape le nom (ou une partie) : la liste se filtre automatiquement.")

section("7. Voir le programme (guide)")
step(1, "Depuis la liste des chaines, appuie sur DROITE pour entrer dans la grille des programmes de la chaine.")
step(2, "En te deplacant sur un programme, son resume s'affiche dans la colonne de droite (avec l'heure et la duree).")

section("8. Replay : revoir une emission passee")
step(1, "Entre dans la grille des programmes (etape 7) et descends vers une emission deja diffusee.")
step(2, "Les emissions rejouables portent une petite icone. Selectionne-en une et appuie sur OK : elle rejoue depuis le debut.")
note("Le nombre de jours disponibles en replay depend de ton fournisseur.")

section("9. Films et Series (a la demande)")
step(1, "Depuis l'accueil, ouvre « Films » ou « Series ». La colonne de gauche liste les categories (ouverture sur « Tout »).")
step(2, "Appuie sur OK sur un titre pour ouvrir sa fiche : affiche, resume et duree.")
step(3, "Sur la fiche : « Lire » lance la lecture (et reprend la ou tu t'es arrete) ; « + A regarder » l'ajoute a ta liste.")
step(4, "Retrouve ta liste dans « ★ A regarder » et tes dernieres lectures dans « ⟳ Historique ». La touche GAUCHE revient en arriere.")

section("10. Sous-titres (films et series)")
step(1, "Pendant la lecture, ouvre le bouton sous-titres (CC), a cote de la roue dentee de la barre de lecture.")
step(2, "Choisis la langue, ou desactive-les. Les sous-titres forces (passages en langue etrangere) s'affichent d'office.")

section("11. Reglages utiles (roue dentee)")
step(1, "Source : changer la playlist (M3U ou Xtream Codes).")
step(2, "Fluidite : « Equilibre », « Confort » ou « Maxi » — a monter si l'image se coupe.")
step(3, "Theme : 8 couleurs au choix pour l'accent de l'interface.")

section("12. Mises a jour")
step(1, "Au demarrage, si une nouvelle version existe, MLTV te propose de l'installer. Accepte : rien d'autre a faire.")

footer()
c.showPage()
c.save()
print("OK")
