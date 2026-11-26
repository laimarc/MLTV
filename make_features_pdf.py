from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.lib.colors import HexColor
from reportlab.lib.styles import ParagraphStyle
from reportlab.platypus import (SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle,
                                ListFlowable, ListItem)
from reportlab.lib.enums import TA_CENTER

ORANGE = HexColor("#FF7A18")
INK = HexColor("#14181f")
MUTED = HexColor("#5d6470")

doc = SimpleDocTemplate("/home/claude/MLTV/MLTV-Fonctionnalites.pdf", pagesize=A4,
                        leftMargin=20*mm, rightMargin=20*mm, topMargin=16*mm, bottomMargin=16*mm,
                        title="MLTV — Fonctionnalites")

logo = ParagraphStyle("logo", fontName="Helvetica-Bold", fontSize=30, textColor=ORANGE,
                      alignment=TA_CENTER, leading=36, spaceAfter=8)
sub = ParagraphStyle("sub", fontName="Helvetica", fontSize=11.5, textColor=MUTED,
                     alignment=TA_CENTER, spaceAfter=14)
sec = ParagraphStyle("sec", fontName="Helvetica-Bold", fontSize=14, textColor=HexColor("#ffffff"),
                     leftIndent=6, spaceBefore=4, spaceAfter=0, leading=20)
item = ParagraphStyle("item", fontName="Helvetica", fontSize=10.8, textColor=INK, leading=15)

def section(title, items):
    t = Table([[Paragraph(title, sec)]], colWidths=[doc.width])
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), ORANGE),
        ("LEFTPADDING", (0, 0), (-1, -1), 10),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ("ROUNDEDCORNERS", [4, 4, 4, 4]),
    ]))
    flow = [Spacer(1, 9), t, Spacer(1, 6)]
    li = [ListItem(Paragraph(x, item), leftIndent=14, value="•") for x in items]
    flow.append(ListFlowable(li, bulletType="bullet", bulletColor=ORANGE,
                             bulletFontSize=9, leftIndent=12, spaceBefore=1))
    return flow

story = [Paragraph("MLTV", logo),
         Paragraph("Lecteur IPTV personnel — liste des fonctionnalites", sub)]

story += section("Television en direct", [
    "Liste de chaines facon TiViMate : numero, logo, programme en cours",
    "Categories / bouquets reorganisables et masquables (appui long sur OK)",
    "Favoris (appui long), avec une categorie dediee &laquo; Favoris &raquo;",
    "Recherche de chaines par nom",
    "Guide 3 colonnes directement sur l'accueil : chaines / grille horaire / apercu + synopsis",
    "Programme en cours surligne dans la couleur du theme",
    "Date et heure affichees en haut a droite",
    "Plein ecran : zapping haut/bas, retour a la liste par la touche gauche",
    "Reconnexion automatique en cas de micro-coupure",
    "Memoire tampon reglable : Equilibre / Confort / Maxi",
])

story += section("Replay / Rattrapage", [
    "Rejouer les programmes passes (jusqu'a plusieurs jours selon le fournisseur)",
    "Icone de rejouabilite sur les programmes concernes dans le guide",
    "Lecture &laquo; tolerante &raquo; compatible avec les serveurs au format non-standard",
])

story += section("Films et Series (a la demande)", [
    "Catalogue classe par categories, ouverture directe sur ta categorie favorite",
    "Fiches detaillees : affiche, synopsis, duree",
    "Liste &laquo; A regarder plus tard &raquo; (films et series)",
    "Historique des contenus lances recemment",
    "Reprise de lecture la ou tu t'es arrete",
    "Sous-titres : pistes forcees affichees d'office, choix de la langue, desactivation immediate",
    "Navigation fluide, retour par la touche gauche",
])

story += section("Personnalisation (roue dentee)", [
    "Source : playlist M3U ou Xtream Codes",
    "Reglage de la fluidite (tampon)",
    "Theme : 8 couleurs modernes (orange, indigo, emeraude, cyan, rose, violet, ambre, sarcelle)",
    "Version de l'application affichee",
])

story += section("Pratique", [
    "Mises a jour automatiques : l'appli propose la nouvelle version au demarrage",
    "Partage facile a tes proches via un simple lien d'installation",
    "Compatible Android TV, Fire TV, Nvidia Shield, box, telephones et tablettes",
])

story += [Spacer(1, 16),
          Paragraph("MLTV — installation : laimarc.github.io/MLTV/MLTV.apk",
                    ParagraphStyle("foot", fontName="Helvetica", fontSize=9.5,
                                   textColor=MUTED, alignment=TA_CENTER))]

doc.build(story)
print("OK")
