# Smart Canteen Token System

A JavaFX + MySQL desktop app. Students log in, order food, and receive a token number.
The canteen admin serves tokens in strict **FIFO** order (first order placed = first served).

## Features
- Register / Login (students self-register; passwords stored as SHA-256 hashes)
- Food menu, cart with quantities, running total
- Place order -> saved in one DB transaction -> daily token number (resets to 1 each day)
- Student sees live queue position (auto-refresh every 5 seconds)
- Admin dashboard: FIFO token queue ("Serve Next Token"), add / update price / remove menu items

## Database (5 tables)
`users`, `food_items`, `orders`, `order_items`, `tokens` - see `sql/schema.sql`.

## Setup
1. Install JDK 17+, Maven, MySQL 8, and (optionally) VS Code with the Java Extension Pack.
   JavaFX is downloaded by Maven automatically.
2. Create the database and tables:
   ```
   mysql -u root -p < sql/schema.sql
   ```
3. Set DB credentials if yours differ from user `root` / empty password:
   ```
   # Windows (PowerShell)
   $env:CANTEEN_DB_PASS = "your_mysql_password"
   # Linux / macOS
   export CANTEEN_DB_PASS=your_mysql_password
   ```
   (Optional: `CANTEEN_DB_URL`, `CANTEEN_DB_USER`.)
4. Test the JDBC connection (Step 7):
   ```
   mvn compile exec:java -Dexec.mainClass=com.canteen.db.DBConnection
   ```
   You should see `Connected to database: smart_canteen`.
5. Run the app:
   ```
   mvn javafx:run
   ```

## Default admin login
- Email: `admin@canteen.com`
- Password: `admin123`  (created automatically on first start - change it!)

## Project structure
```
src/main/java/com/canteen/
  MainApp.java            JavaFX entry point + screen navigation
  db/DBConnection.java    JDBC connection (+ test main)
  model/                  User, FoodItem, CartItem, Token
  dao/                    UserDAO, FoodDAO, OrderDAO, TokenDAO (all SQL lives here)
  service/TokenQueue.java FIFO queue (ArrayDeque) synced with the DB
  ui/                     LoginView, StudentView, AdminView, Ui helpers
```

## How the FIFO queue works
`TokenQueue` keeps WAITING tokens in an `ArrayDeque`. New tokens are added at the back
(`enqueue`); the admin's **Serve Next Token** removes from the front (`serveNext`), marking
the token `SERVED` and the order `COMPLETED`. `reload()` rebuilds the queue from the database
(ordered by `token_id`), so several running copies of the app stay in sync via Refresh.

## Ideas for Step 16 (GitHub + PPT)
- Add screenshots of Login, Menu/Cart, Token confirmation, Admin queue.
- Mention: OOP (models/DAO/service layers), JDBC transactions, FIFO queue (Deque), JavaFX UI.
- Future work: order history, payment, SMS/email token alerts, BCrypt hashing.
