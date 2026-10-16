# Terminal Hour — Play Console checklist and listing copy

## You must do (Claude can't)
1. Create a Google Play developer account at https://play.google.com/console (personal, US$25 one-time, ID verification).
2. Take 2–8 phone screenshots (Settings ▸ Theme: one set in Tactical, a couple in Tower; include a widget).
3. Recruit 12 testers (Gmail accounts). Closed test must have 12 opted in for 14 consecutive days before you can apply for production.

## Store listing
- **App name (≤30):** Terminal Hour: HUD Clock
- **Short description (≤80):** Alarm, world clock, timer & pomodoro with sci-fi HUD themes and home widgets.
- **Category:** Tools · **Tags:** Clock, Alarm clock, Productivity
- **Graphics:** `store/icon-512.png`, `store/feature-graphic-1024x500.png`, your screenshots
- **Privacy policy URL:** https://github.com/COURAGEOUS1234/TimeApp/blob/main/store/PRIVACY.md (add a contact email first)

**Full description:**

Terminal Hour turns your phone's clock into a sci-fi heads-up display — and it's a complete clock app underneath.

ALARMS
• Multiple alarms with repeat days, names and snooze length
• Synthesised siren or your phone's alarm tone, with optional fade-in
• Full-screen alert over the lock screen; survives restarts
• "T-minus" countdown to your next alarm

WORLD CLOCK
• Search any city, see the time difference and whether it's today or tomorrow there

STOPWATCH & TIMER
• Laps with split times • Timer presets • Live notifications while running

POMODORO FOCUS
• Custom focus, short and long breaks and rounds • Auto-start options • Daily focus stats

TWO HUD THEMES
• Tactical — amber field HUD with stencil digits, hash reticle and time tape
• Tower — clean ice-blue sky-station HUD with thin type and circular gauges

HOME-SCREEN WIDGETS
• HUD Clock, HUD World Clock and HUD Focus (start/pause pomodoro from the home screen)

ALWAYS-ON ALTERNATIVES
• Standby mode and a charging screen saver with burn-in protection

PRIVATE BY DESIGN
• No internet permission, no ads, no tracking. Everything stays on your phone.

## App content answers
- **Ads:** No · **In-app purchases:** No
- **Data safety:** No data collected; no data shared. (App has no internet permission.)
- **Target audience:** 13 and over (avoids Families policy requirements)
- **Content rating questionnaire:** Utility/productivity; answer "No" to violence, sexuality, language, drugs, gambling, user interaction → expect "Everyone"
- **Exact alarm permission (USE_EXACT_ALARM):** declare "Alarm clock app" — exact alarms are the core function.
- **Full-screen intent:** declare "Alarm / timer alerts".
- **Government / financial / health features:** None.

## Policy notes
- Do not mention the films the themes are inspired by, in the listing or screenshots (trademark/impersonation policy).
- Package name `com.gabriel.timeapp` becomes permanent on first upload.

## Release flow
1. GitHub ▸ Actions ▸ latest run ▸ download **TerminalHour-play-bundle** (`app-release.aab`, signed with the upload key once secrets are set).
2. Play Console ▸ Testing ▸ Closed testing ▸ Create release ▸ upload the `.aab`. Choose **Play App Signing** (Google holds the app key; the upload key can be reset if lost).
3. Add testers' emails, share the opt-in link, wait 14 days, then Dashboard ▸ Apply for production.
