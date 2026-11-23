# ⚽ Footballion — Daily Football Trivia & Rarity Challenge

🎮 **[Play Live Demo → footballion.onrender.com](https://footballion.onrender.com)**  
*(Hosted on Render's free tier — please allow ~30 seconds to wake up on the first visit if the server was asleep.)*

**Footballion** is a daily football trivia web application built with **Java 21** and **Spring Boot**. Unlike traditional trivia games where every right answer is equal, Footballion rewards **deep football knowledge ("Ball Knowledge")**: players have **45 seconds** per question to come up with the **rarest valid answer** they can think of across a database of **50,000+ players**, **790+ clubs**, and **1.7M+ historical match records**.

---

## ✨ Key Features

- **14 Dynamic Question Strategies:** Every day generates a deterministic 5-question challenge seeded by the current date so all players worldwide compete on the exact same board.
- **5-Tier Rarity Scoring Engine (`10 / 30 / 60 / 85 / 100 pts`):** Scores answers by combining candidate-pool percentile ranking, player prominence (current & peak market value, recency, international caps), and live daily crowd picks.
- **45-Second Countdown & Instant Autocomplete:** Real-time debounced search across 50,000+ players and clubs with instant wrong-guess feedback so players can keep guessing until the clock runs out.
- **Gaussian Bell Curve Daily Summary:** End-of-game summary plots the player's score (`YOU`) on a dynamic SVG normal distribution curve against today's global average and percentile, with one-click WhatsApp/clipboard emoji sharing.

---

## 🏗️ Architecture & Software Engineering Highlights

- **Strategy Pattern (Open/Closed Principle):**
  - Defined via [`QuestionStrategy`](src/main/java/com/footballion/footballion/strategy/QuestionStrategy.java) and implemented by 14 independent `@Component` classes (e.g., [`UclGoalscorerStrategy`](src/main/java/com/footballion/footballion/strategy/UclGoalscorerStrategy.java), [`ClubLoyalistStrategy`](src/main/java/com/footballion/footballion/strategy/ClubLoyalistStrategy.java), [`PlayedForBothClubsStrategy`](src/main/java/com/footballion/footballion/strategy/PlayedForBothClubsStrategy.java), [`DirectClubTransferStrategy`](src/main/java/com/footballion/footballion/strategy/DirectClubTransferStrategy.java)).
  - Spring automatically injects `List<QuestionStrategy>` into [`QuestionService`](src/main/java/com/footballion/footballion/service/QuestionService.java), allowing new question categories to be added without modifying core service logic.
- **Streamed ETL & Pre-Aggregation ([`DataLoader`](src/main/java/com/footballion/footballion/loader/DataLoader.java)):**
  - Streams raw Transfermarkt datasets (`appearances.csv`, `games.csv`, `game_events.csv`, `transfers.csv` — over 1.7M rows) and pre-aggregates them in memory by `(playerId, clubId, competitionId)` into ~35,000 indexed summary records (`aggregated_appearances.csv`), turning multi-hundred-megabyte scans into millisecond indexed lookups.
- **Thread-Safe Live Crowd Tracking ([`RarityAndStatsService`](src/main/java/com/footballion/footballion/service/RarityAndStatsService.java)):**
  - Uses `ConcurrentHashMap` and synchronized score aggregation to track live daily pick ratios and global score percentiles across concurrent HTTP worker threads.

---

## 🎯 Scoring Algorithm Guide (In Simple Words)

Every valid answer is awarded one of **5 fixed point tiers** based on how rare or hard-to-think-of that answer is:

| Points | Tier Name | Badge Label | Share Emoji | What It Means |
| :---: | :--- | :--- | :---: | :--- |
| **10 pts** | **Generic / Obvious** | `The Mainstream Pick 📢` | 🟨 | Household superstars, top-value players, or the biggest clubs in a league. |
| **30 pts** | **Moderately Common** | `Solid Squad Player 👍` | 🟦 | Well-known regular starters or upper-tier clubs that many fans know. |
| **60 pts** | **Fairly Rare** | `Starting 11 ⚡` | 🟩 | Squad/rotational players, mid-tier starters, or mid-table clubs. |
| **85 pts** | **Very Rare** | `Absolute Ball Knowledge! 🔥` | 🔥 | Lesser-known backups, fringe players, older veterans, or smaller clubs. |
| **100 pts** | **Extremely Rare** | `GOAT 🐐` | 💎 | Deep-cut obscure players, low-market-value reserves, long-retired players, or the smallest clubs in a league. |

*(Incorrect answers, skipped questions, or running out of time earn **0 points** 🟥.)*

### How the Score Is Calculated in 4 Steps

1. **Step 1 — Measure How "Famous" Your Answer Is:**
   - **For Players:** Combines current market value, peak career market value (so retired legends are still recognized as famous), recent activity (`lastSeason`), and international caps.
   - **For Clubs:** Sums the active squad market value of all players at that club.
2. **Step 2 — Compare Your Answer to All Other Valid Answers for That Question:**
   - A €10M player might be an *obvious* star on a smaller club, but a *rare* reserve on Real Madrid. The engine fetches **all valid candidates for that specific question** across all 14 question types:
     - *Player in Club / Position in League / Nationality in League* (Top 5 European leagues: Premier League, La Liga, Serie A, Bundesliga, Ligue 1)
     - *Club in League* (13 major leagues) & *Player by Nationality*
     - *UEFA Champions League Goalscorer & Assister*
     - *Major Tournament Goalscorer* (FIFA World Cup & UEFA Euro)
     - *Club Loyalist* (100+ league appearances for a Top 5 European club)
     - *Played for Both Clubs* & *Direct Club-to-Club Transfer*
   - Your pick is ranked by percentile within that pool (**70%** relative pool rarity + **30%** global prominence).
3. **Step 3 — Blend with Today's Live Crowd Picks:**
   - As more players play the daily challenge today, up to **40%** of the rarity calculation adjusts based on what percentage of today's players chose the same answer.
4. **Step 4 — Lock into the 5 Point Tiers:**
   - **Rarity `0 – 21`** $\rightarrow$ **10 points**
   - **Rarity `22 – 44`** $\rightarrow$ **30 points**
   - **Rarity `45 – 67`** $\rightarrow$ **60 points**
   - **Rarity `68 – 84`** $\rightarrow$ **85 points**
   - **Rarity `85 – 100`** $\rightarrow$ **100 points**

---

## 🛠️ Tech Stack

- **Backend:** Java 21, Spring Boot 4, Spring Web MVC, Spring Data JPA (Hibernate)
- **Database:** H2 File/In-Memory Database (PostgreSQL driver included for cloud deployment)
- **Data Processing:** OpenCSV 5.9
- **Frontend:** HTML5, CSS3 (Glassmorphism UI), Vanilla JavaScript, Dynamic SVG Gaussian Bell Curve
- **Containerization:** Multi-stage `Dockerfile` (Eclipse Temurin 21)

---

## 🚀 Getting Started Locally

### Prerequisites
- **Java 21+** installed

### Run with Maven Wrapper
```bash
./mvnw spring-boot:run
```
Then open **[http://localhost:8080](http://localhost:8080)** in your browser.

### Run with Docker
```bash
docker build -t footballion .
docker run -p 8080:8080 footballion
```
