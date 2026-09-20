import sys
import re
import unicodedata
import urllib.parse

import requests
from bs4 import BeautifulSoup

HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.5481.100 Safari/537.36',
    'Referer': 'https://www.google.com/',
    'Accept-Language': 'en-US,en;q=0.9',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8'
}


# Function to modify the song title and artist string for the URL
def modify_string_for_url(string):
    special_chars = ["'", '£', "%", '/', '.', '(', ')', '=', '?', '[', ']', '#', '@', '|', ',', '¥', '​']

    # Normalize the string and remove accents
    normalized_string = unicodedata.normalize('NFKD', string).encode('ASCII', 'ignore').decode('utf-8')

    # Replace common symbols with the expected equivalents for URLs
    modified_string = normalized_string.replace('&', 'and').replace('$', 's').replace('!', 'i')

    # Remove special characters
    for symbol in special_chars:
        modified_string = modified_string.replace(symbol, "")

    # Replace spaces with a single hyphen
    modified_string = modified_string.replace(" ", "-")

    # Remove any consecutive hyphens (in case multiple spaces were replaced)
    modified_string = re.sub(r'-+', '-', modified_string)

    return modified_string.lower()


# Il tag ARTIST di un brano in collaborazione arriva spesso come "Artista A, Artista B"
# o "Artista A & Artista B" (a seconda di chi ha taggato il file). Genius invece, nei
# propri URL, unisce sempre più artisti con "and" (es. "claver-gold-and-murubutu-...",
# mai "claver-gold-murubutu-..."): senza questa normalizzazione l'URL indovinato dà
# quasi sempre 404 per qualunque brano con più di un artista.
def build_artist_slug(artist_string):
    parts = [p.strip() for p in re.split(r'[,;&/]', artist_string) if p.strip()]
    joined = ' and '.join(parts) if parts else artist_string
    return modify_string_for_url(joined)


# Riserva usata solo quando l'URL indovinato non esiste: interroga lo stesso endpoint di
# ricerca (non ufficiale, senza chiave) che usa il sito genius.com per la propria barra di
# ricerca. Copre i casi che una regola fissa di costruzione dell'URL non può prevedere in
# anticipo (collaborazioni a 3+ artisti, "feat.", titoli con punteggiatura particolare,
# ...). Ritorna l'URL della pagina del brano, o None se la ricerca non trova nulla.
def search_genius_url(artist, song_title):
    query = f"{artist} {song_title}"
    search_url = "https://genius.com/api/search/multi?q=" + urllib.parse.quote(query)
    search_headers = dict(HEADERS, **{'Accept': 'application/json'})

    try:
        response = requests.get(search_url, headers=search_headers, timeout=15)
    except requests.RequestException:
        return None

    if response.status_code != 200:
        return None

    try:
        data = response.json()
    except ValueError:
        return None

    for section in data.get('response', {}).get('sections', []):
        if section.get('type') == 'song':
            hits = section.get('hits', [])
            if hits:
                return hits[0].get('result', {}).get('url')
    return None


# Estrae il testo pulito dei lyrics dalla pagina già scaricata (soup). Ritorna None se il
# contenitore dei lyrics non viene trovato in pagina.
def parse_lyrics(soup):
    # Selettore semantico esposto da Genius per i blocchi di lyrics, al posto del
    # prefisso di classe CSS compilata (es. "Lyrics__Container-sc-..."): quella classe
    # cambia ad ogni redeploy del sito e in più, oggi, matcha anche blocchi che non sono
    # lyrics (il riquadro "Contributors/Translations" in cima alla pagina), facendo
    # comparire quel testo mescolato prima dei lyrics veri
    lyrics_containers = soup.select('div[data-lyrics-container="true"]')
    if not lyrics_containers:
        return None

    parts = []
    for container in lyrics_containers:
        # Genius marca esplicitamente come "escluso dalla selezione" il blocco iniziale
        # (conteggio contributor, elenco lingue di traduzione): va rimosso, altrimenti
        # finisce mescolato in testa al testo della canzone
        for excluded in container.select('[data-exclude-from-selection="true"]'):
            excluded.decompose()

        # I tag <br> non producono alcun testo con get_text(): vanno convertiti in "a
        # capo" prima di estrarre il testo, altrimenti i versi finiscono tutti attaccati
        for br in container.find_all('br'):
            br.replace_with('\n')

        # get_text() (a differenza di leggere solo i .string dei singoli nodi) scende
        # correttamente in tutti i tag annidati (i link di annotazione di Genius
        # avvolgono spesso più righe in un unico <a>), quindi non perde testo
        parts.append(container.get_text())

    lyrics = '\n'.join(parts)

    # Rimuove le etichette di sezione tra parentesi quadre ([Chorus], [Verse 1], ...)
    lyrics = re.sub(r'\[[^\]]*\]', '', lyrics)
    # Comprime le righe vuote multiple lasciate dalla rimozione delle etichette qui sopra
    lyrics = re.sub(r'\n{3,}', '\n\n', lyrics)

    return lyrics.strip()


# Function to extract lyrics from the page. Ritorna il testo pulito, oppure None se non
# trovato. IMPORTANTE: ogni messaggio di stato/debug va su stderr, mai su stdout - lo
# stdout viene letto direttamente da Java come "il testo dei lyrics" (vedi
# LyricsController.fetchLyricsFromPythonScript), quindi deve contenere solo quello.
def extract_lyrics(artist, song_title):
    artist_url = build_artist_slug(artist)
    song_title_url = modify_string_for_url(song_title)
    url = f"https://genius.com/{artist_url}-{song_title_url}-lyrics"
    print(f"Fetching lyrics from: {url}", file=sys.stderr)

    response = requests.get(url, headers=HEADERS, timeout=15)

    if response.status_code != 200:
        print(f"Direct URL guess failed (status {response.status_code}), trying Genius search instead", file=sys.stderr)
        found_url = search_genius_url(artist, song_title)
        if not found_url:
            print("Genius search did not find this song either.", file=sys.stderr)
            return None

        print(f"Found via search: {found_url}", file=sys.stderr)
        url = found_url
        response = requests.get(url, headers=HEADERS, timeout=15)
        if response.status_code != 200:
            print(f"Failed to retrieve lyrics from search result, status code: {response.status_code}", file=sys.stderr)
            return None

    soup = BeautifulSoup(response.text, 'html.parser')
    lyrics = parse_lyrics(soup)

    if lyrics is None:
        print("Could not find the lyrics on the page.", file=sys.stderr)
        return None

    return lyrics


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python get_lyrics.py 'artist' 'song title'", file=sys.stderr)
        sys.exit(2)

    artist_name = sys.argv[1]
    song_title = sys.argv[2]

    try:
        lyrics_result = extract_lyrics(artist_name, song_title)
    except requests.RequestException as e:
        print(f"Network error while fetching lyrics: {e}", file=sys.stderr)
        lyrics_result = None

    if lyrics_result:
        # Unico output su stdout in caso di successo: il testo pulito dei lyrics, e
        # nient'altro (Java lo legge così com'è)
        print(lyrics_result)
        sys.exit(0)
    else:
        # Nessun testo trovato: exit code diverso da zero, così Java può distinguere
        # "non trovato" da "trovato ma vuoto" invece di considerarlo sempre un successo
        sys.exit(1)
