# Truth Mode Shots — Android project v1.0

A local-first Android app shell for the user's Truth Mode Team Shots Engine. It includes a manual team-shots probability/value calculator, local analysis history, calibration-profile import, and an automatic-data adapter interface.

## Important build status

This workspace does **not** contain the Android SDK or Gradle, so a compiled APK has not been produced or tested here. Open this project in Android Studio, allow Gradle sync, then use **Build → Build Bundle(s) / APK(s) → Build APK(s)**. The debug APK should be generated at:

`app/build/outputs/apk/debug/app-debug.apk`

Use Android Studio with JDK 17+, Android SDK Platform 35, and internet access for Gradle dependency resolution. Minimum supported Android version is API 24 (Android 7.0).

## Features in this MVP

- Dark mobile dashboard and five configured leagues: Premier League, La Liga, Bundesliga, Serie A, Ligue 1.
- Manual entry of team season shots, last-five shots, opponent season/last-five shots conceded, venue estimate, tactical/lineup estimate, dispersion and bookmaker lines.
- Multiple Over lines in one run using `line@odds`, comma-separated, e.g. `8.5@1.42, 9.5@1.50`.
- Negative Binomial primary distribution and Poisson benchmark.
- Fair odds, expected value and configured gates: 80% minimum calibrated probability, odds 1.35–1.55, EV at least +5%.
- Explicit NO BET when a league-specific calibration profile is unavailable or does not cover the raw probability range.
- Local history and calibration profiles stored in WebView local storage.
- Automatic fixture → match statistics → team-shots odds retrieval through a configurable HTTPS adapter.

## Statistical model

The current projection is the initial v1.0 weighted estimator:

`lambda = 0.25*season_shots + 0.20*last5_shots + 0.20*opponent_season_conceded + 0.15*opponent_last5_conceded + 0.10*venue_estimate + 0.10*tactical_estimate`

The Negative Binomial distribution uses mean `lambda` and user-entered dispersion `r`; variance is `lambda + lambda^2/r`. Poisson is available as a benchmark. These weights and dispersion are not trained or validated by this app. Do not interpret raw model probability as calibrated probability.

A line such as Over 9.5 requires at least 10 shots. For a half-shot line `L`, the app calculates `P(X >= floor(L)+1)`.

A line is numerically approved only when all are true:

- A calibration bin exists for that league and raw probability.
- Calibrated probability is at least 80%.
- Odds are between 1.35 and 1.55 inclusive.
- Expected value using calibrated probability is at least +5%.

Even a PASS is a screening result, not a guarantee or a recommendation to stake. Check the source, settlement rules, lineup and data timestamp.

## Calibration profile format

Import one JSON file per league from Settings. The app expects:

```json
{
  "schema_version": 1,
  "validated": true,
  "league": "Premier League",
  "out_of_sample_selections": 350,
  "validation_date": "2026-11-01",
  "calibration_method": "isotonic or reliability-bin mapping",
  "bins": [
    {"raw_min": 0.75, "raw_max": 0.80, "calibrated_probability": 0.76},
    {"raw_min": 0.80, "raw_max": 0.85, "calibrated_probability": 0.81},
    {"raw_min": 0.85, "raw_max": 0.90, "calibrated_probability": 0.86}
  ]
}
```

The numbers above are **illustrative schema examples only**, not measured calibration results. Do not import them as a real profile. Build bins from genuine out-of-sample predictions and settled outcomes. Profiles should be league-specific, based on at least 300 out-of-sample selections, and preferably use a separate holdout set. The app checks the fields and count but cannot independently audit the source data or prove that the profile is truthful.

## Automatic data adapter contract

The app does not include a bookmaker feed, football statistics subscription, SportyBet integration, or scraping. You must supply an API provider or your own secure adapter. Configure the HTTPS base URL, read-only token and endpoint paths in the Data tab.

### 1. Fixtures endpoint

The app requests:

`GET /fixtures?league=Premier%20League&from=YYYY-MM-DD`

Expected response:

```json
{
  "fixtures": [
    {
      "id": "fixture-123",
      "league": "Premier League",
      "home": "Home FC",
      "away": "Away FC",
      "kickoff": "2026-11-20T19:00:00Z"
    }
  ]
}
```

League names should match the five app labels exactly.

### 2. Match statistics endpoint

The default path is `/matches/{matchId}/team-shots`.

Expected response:

```json
{
  "source": "your statistics provider",
  "retrieved_at": "2026-11-19T12:00:00Z",
  "shot_definition": "total shots including blocked shots",
  "teams": [
    {
      "side": "home",
      "season_shots": 15.1,
      "last5_shots": 14.4,
      "opponent_season_shots_conceded": 12.8,
      "opponent_last5_shots_conceded": 13.2,
      "venue_estimate": 15.4,
      "tactical_estimate": 14.8,
      "dispersion": 12
    },
    {
      "side": "away",
      "season_shots": 10.4,
      "last5_shots": 9.8,
      "opponent_season_shots_conceded": 14.0,
      "opponent_last5_shots_conceded": 13.7,
      "venue_estimate": 10.1,
      "tactical_estimate": 9.5,
      "dispersion": 12
    }
  ]
}
```

### 3. Team-shots odds endpoint

The default path is `/matches/{matchId}/odds?market=team_shots`.

Expected response:

```json
{
  "source": "your bookmaker odds provider",
  "retrieved_at": "2026-11-19T12:01:00Z",
  "markets": [
    {"side": "home", "market": "team_shots_over", "line": 9.5, "odds": 1.48},
    {"side": "home", "market": "team_shots_over", "line": 10.5, "odds": 1.55},
    {"side": "away", "market": "team_shots_over", "line": 7.5, "odds": 1.51}
  ]
}
```

The app filters the returned markets by team side. The adapter must ensure odds are current and identify the bookmaker and market settlement definition. It must not transform match odds or goal odds into team-shot odds.

### Security

This MVP stores the API base/path settings locally and keeps the token in the current WebView session only. For production, do not embed a provider master key in an APK. Use a backend proxy with server-side secrets, limited scopes, authentication, rate limits and audit logs. Use HTTPS only. The provider must allow the app's WebView origin (`https://appassets.androidplatform.net`) through CORS.

## Suggested production work before real-money use

1. Select and contract a statistics provider and a permitted bookmaker-odds provider.
2. Build and secure the adapter backend; normalize shot definitions and timestamps.
3. Train league-specific model coefficients and dispersion from historical pre-match data.
4. Run walk-forward backtests and independent out-of-sample calibration.
5. Audit the calibration profile and settlement logic.
6. Test the APK on multiple Android devices and protect credentials.

## Project structure

- `app/src/main/java/.../MainActivity.kt` — Android WebView host.
- `app/src/main/assets/index.html` — app UI, local calculator, data adapter client and history.
- `app/src/main/AndroidManifest.xml` — app manifest and network permission.

## Cloud build from a phone (no Windows PC required)

This project includes `.github/workflows/build-apk.yml` for GitHub Actions. To build from an Android phone:

1. Create or sign in to a GitHub account in your phone browser.
2. Create a new repository named `truth-mode-shots` (public or private).
3. Extract this ZIP on your phone, then upload the extracted project files and folders to the repository root. Make sure the hidden `.github/workflows/build-apk.yml` file is included; if the mobile upload UI cannot upload hidden folders, create that workflow file in GitHub using **Add file → Create new file** and enter the path `.github/workflows/build-apk.yml`, then paste the workflow contents from this project.
4. Open the repository's **Actions** tab, select **Build Truth Mode Shots APK**, and tap **Run workflow**. A push to `main` also starts a build.
5. When the run completes successfully, open the run, scroll to **Artifacts**, and download `truth-mode-shots-debug-apk`.
6. Extract the downloaded artifact ZIP and tap `app-debug.apk` to install. Android may ask you to allow installation from that browser or file manager.

The cloud workflow compiles a debug APK; it does not provide live football or bookmaker data. Automatic data still requires a compatible HTTPS provider/adapter configured in the app. The app's model weights are preliminary and must not be treated as calibrated until genuine league-specific out-of-sample calibration profiles are supplied.
