import requests
from bs4 import BeautifulSoup
import re
import unicodedata

# Function to modify the song title and artist string for the URL
def modify_string_for_url(string):
    special_chars = ["'", '£', "%", '/', '.', '(', ')', '=', '?', '[', ']', '#', '@', '|', ',', '¥', '\u200b']

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

# Function to extract lyrics from the page
def extract_lyrics(artist, song_title):
    # Modify artist and song title to match Genius URL structure
    artist_url = modify_string_for_url(artist)
    song_title_url = modify_string_for_url(song_title)

    # Construct the URL
    url = f"https://genius.com/{artist_url}-{song_title_url}-lyrics"
    print(f"Fetching lyrics from: {url}")

    # Send a GET request to the URL
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.5481.100 Safari/537.36',
        'Referer': 'https://www.google.com/',
        'Accept-Language': 'en-US,en;q=0.9',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8'
    }

    response = requests.get(url, headers=headers)

    if response.status_code != 200:
        print(f"Failed to retrieve lyrics, status code: {response.status_code}")
        return

    # Parse the HTML using BeautifulSoup
    soup = BeautifulSoup(response.text, 'html.parser')

    # Find the lyrics container
    lyrics_divs = soup.select('div[class*="Lyrics__Container-sc-"]')

    if not lyrics_divs:
        print("Could not find the lyrics on the page.")
        return

    # Extract and clean the lyrics
    lyrics = ""
    last_line = ""  # To track the last line added
    for div in lyrics_divs:
        # Go through all child elements of the div, handling line breaks explicitly
        for element in div.descendants:
            if element.name == 'br':
                lyrics += '\n'
            elif element.string:
                line = element.string.strip()
                # Only add the line if it's not a duplicate of the immediately previous line
                if line and line != last_line:
                    lyrics += line + '\n'
                    last_line = line  # Update the last added line

    # Clean up the lyrics text
    lyrics = re.sub(r'\[[^\]]*\]', '', lyrics)  # Remove content inside brackets

    print("\nLyrics:")
    print(lyrics.strip())

if __name__ == "__main__":
    import sys

    if len(sys.argv) < 3:
        print("Usage: python get_lyrics.py 'artist' 'song title'")
    else:
        artist_name = sys.argv[1]
        song_title = sys.argv[2]
        extract_lyrics(artist_name, song_title)
