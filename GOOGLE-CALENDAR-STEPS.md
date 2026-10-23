# Add personal Google Calendar to JL02 TV

1. On a computer, visit https://calendar.google.com/ and sign in to your **personal** Google account.
2. Open the gear icon → **Settings**.
3. In **Settings for my calendars**, select the calendar you want shown.
4. Find **Integrate calendar** → **Secret address in iCal format** and copy the URL ending `.ics`. (If Google does not offer it, this method is unavailable for that calendar.)
5. In JL02 TV, open **Calendars** → **Add Google Calendar iCal link (read-only)**.
6. Paste the URL and select **Save & sync**. A Bluetooth keyboard or the Google TV remote phone input feature can make pasting easier.
7. In JL02 TV select Refresh to update. Only events scheduled for today are displayed; subscriptions can be delayed.

## Privacy
The secret iCal URL is a bearer secret: do not publish it, send screenshots containing it, or put it in a GitHub variable. JL02 stores it in private app preferences; it is not encrypted by this prototype, so this is intended only for personally owned, trusted TVs. Revoke it from Google Calendar settings if compromised. This is not a two-way sync and does not modify your Google Calendar. Company Microsoft 365 calendar policies should be respected. Microsoft events currently take precedence rather than being merged.
